package com.pokechain.data.dialgadex

import android.util.Log
import com.pokechain.data.models.PvERankingEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Scraper de rankings de Combates Max (Dynamax/Gigantamax) desde Dittobase.
 *
 * Fuente: https://www.dittobase.com/pokemon-go/best-attackers/max-battles/{type}
 * (una página individualizada por tipo, p.ej. /fire, /water...)
 *
 * La tabla NO usa <table>/<tr>/<td>: es una FlexTable de Chakra UI con
 * filas <li role="row">. La estructura HA CAMBIADO con el tiempo:
 *   - Original (7 celdas): # | Pokémon | Max Move | Attack | Max damage | MMW | % of best
 *   - Actual  (8 celdas):  [espaciador] | # | Pokémon | Max Move | Attack | Max damage | MMW | % of best
 * Por eso el parser se ANCLA en la celda del Pokémon (la que trae el sprite
 * con alt) y lee los desplazamientos relativos (−1 = puesto, +1 = Max Move,
 * +3 = Max damage), que son idénticos en ambas estructuras.
 *
 * - El alt del sprite SIEMPRE trae el nombre completo con forma
 *   ("Gigantamax Cinderace" / "Dynamax Moltres").
 * - El src del sprite es la URL directa del sprite; de ella se extrae el dex
 *   (/pokemon/(\d+)): cubre la URL antigua (/go/pokemon/815-....png) y la
 *   actual (/go/optimized/v1/pokemon/815-...-128.webp).
 * - La celda Max Move muestra el nombre del signature move para G-Max
 *   ("G-Max Fireball") y SOLO el tipo para Dynamax ("Fire") — se deriva
 *   el nombre del Max Move con MAX_MOVE_BY_TYPE.
 *
 * La descarga se hace con la API asíncrona de OkHttp: así el timeout de la
 * coroutine se cumple siempre (incluso si la resolución DNS del dispositivo
 * se queda colgada, que OkHttp NO puede interrumpir) y la pantalla nunca
 * queda cargando indefinidamente.
 */
class MaxBattleScrapingEngine(
    /** URL base de la fuente (parametrizable para los tests). */
    private val baseUrl: String = BASE_URL,
    /** Deadline de la descarga en ms (parametrizable para los tests). */
    private val timeoutMs: Long = REQUEST_TIMEOUT_MS
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        // Red de seguridad de OkHttp (el deadline real es timeoutMs)
        .callTimeout(25, TimeUnit.SECONDS)
        .build()

    // Cache por tipo: "type" -> List<PvERankingEntry>
    private val typeCache = ConcurrentHashMap<String, List<PvERankingEntry>>()

    companion object {
        private const val TAG = "MaxBattle"
        private const val BASE_URL = "https://www.dittobase.com/pokemon-go/best-attackers/max-battles"
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36"

        /** Deadline de la descarga: pasado este tiempo se cancela y se avisa. */
        const val REQUEST_TIMEOUT_MS = 20_000L

        /** Deadline de la prueba de conexión (diagnóstico). */
        const val PROBE_TIMEOUT_MS = 5_000L

        /** Tipos válidos (coinciden con los slugs de Dittobase, en lowercase) */
        val VALID_TYPES = listOf(
            "normal", "fire", "water", "electric", "grass", "ice",
            "fighting", "poison", "ground", "flying", "psychic", "bug",
            "rock", "ghost", "dragon", "dark", "steel", "fairy"
        )

        val TYPE_DISPLAY_NAMES = mapOf(
            "normal" to "Normal",
            "fire" to "Fire",
            "water" to "Water",
            "electric" to "Electric",
            "grass" to "Grass",
            "ice" to "Ice",
            "fighting" to "Fighting",
            "poison" to "Poison",
            "ground" to "Ground",
            "flying" to "Flying",
            "psychic" to "Psychic",
            "bug" to "Bug",
            "rock" to "Rock",
            "ghost" to "Ghost",
            "dragon" to "Dragon",
            "dark" to "Dark",
            "steel" to "Steel",
            "fairy" to "Fairy"
        )

        /**
         * Mapeo tipo -> nombre del Max Move genérico (Dynamax).
         * El Max Move de un Dynamax depende del tipo de su movimiento rápido.
         */
        val MAX_MOVE_BY_TYPE = mapOf(
            "normal" to "Max Strike",
            "fire" to "Max Flare",
            "water" to "Max Geyser",
            "electric" to "Max Lightning",
            "grass" to "Max Overgrowth",
            "ice" to "Max Hailstorm",
            "fighting" to "Max Knuckle",
            "poison" to "Max Ooze",
            "ground" to "Max Quake",
            "flying" to "Max Airstream",
            "psychic" to "Max Mindstorm",
            "bug" to "Max Flutterby",
            "rock" to "Max Rockfall",
            "ghost" to "Max Phantasm",
            "dragon" to "Max Wyrmwind",
            "dark" to "Max Darkness",
            "steel" to "Max Steelspike",
            "fairy" to "Max Starfall"
        )
    }

    /**
     * Devuelve el Top [count] (por defecto 10) de atacantes Max para un tipo.
     * El orden ya viene dado por la web (ranked by Max Attack damage).
     *
     * @throws IOException si la descarga falla o se agota el tiempo.
     */
    suspend fun computeMaxByType(type: String, count: Int = 10): List<PvERankingEntry> {
        val key = type.trim().lowercase()
        typeCache[key]?.let { cached ->
            return cached.take(count)
        }

        Log.d(TAG, "Descargando ranking Max para '$key'")
        val html = withTimeoutOrNull(timeoutMs) { fetchHtml(key) }
            ?: throw IOException("Timeout de ${timeoutMs / 1000}s al descargar los datos")

        val results = parseHtml(html)
        Log.d(TAG, "Parseados ${results.size} atacantes para '$key'")
        // No cachear vacío: un 404/captcha transitorio no debe petrificar el ranking
        if (results.isEmpty()) return emptyList()
        val withRanks = results.take(count).mapIndexed { index, entry ->
            entry.copy(originalRank = index + 1)
        }
        typeCache[key] = withRanks
        return withRanks
    }

    /**
     * Descarga el HTML de un tipo de forma asíncrona y no bloqueante: la
     * coroutine puede cancelarse (timeout o "Cancelar" del usuario) y la
     * petición de OkHttp se aborta con call.cancel().
     */
    private suspend fun fetchHtml(type: String): String = suspendCancellableCoroutine { continuation ->
        val request = Request.Builder()
            .url("$baseUrl/$type")
            .header("User-Agent", USER_AGENT)
            .build()

        val call = client.newCall(request)
        continuation.invokeOnCancellation {
            Log.w(TAG, "Petición cancelada para '$type'")
            call.cancel()
        }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.w(TAG, "Fallo de red en '$type': ${e.javaClass.simpleName}: ${e.message}")
                if (continuation.isActive) continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val code = it.code
                    if (!it.isSuccessful) {
                        Log.w(TAG, "HTTP $code en '$type'")
                        if (continuation.isActive) {
                            continuation.resumeWithException(IOException("HTTP $code"))
                        }
                        return
                    }
                    val body = it.body?.string()
                    if (body.isNullOrEmpty()) {
                        Log.w(TAG, "Respuesta vacía en '$type'")
                        if (continuation.isActive) {
                            continuation.resumeWithException(IOException("Respuesta vacía"))
                        }
                        return
                    }
                    Log.d(TAG, "OK '$type': ${body.length} caracteres")
                    if (continuation.isActive) continuation.resume(body)
                }
            }
        })
    }

    /**
     * Prueba rápida de conectividad con la fuente (para diagnóstico en la
     * pantalla). Devuelve "HTTP <código>" si responde o el mensaje del error.
     */
    suspend fun probe(): String = withContext(Dispatchers.IO) {
        try {
            withTimeoutOrNull(PROBE_TIMEOUT_MS) {
                val request = Request.Builder()
                    .url("$baseUrl/fire")
                    .head()
                    .header("User-Agent", USER_AGENT)
                    .build()
                client.newCall(request).execute().use { response -> "HTTP ${response.code}" }
            } ?: "Timeout de ${PROBE_TIMEOUT_MS / 1000}s"
        } catch (e: Exception) {
            "${e.javaClass.simpleName}: ${e.message ?: "sin detalle"}"
        }
    }

    internal fun parseHtml(html: String): List<PvERankingEntry> {
        val doc: Document = Jsoup.parse(html)
        val entries = mutableListOf<PvERankingEntry>()

        // Filas de datos: <li role="row"> (el header de columnas es un <div role="row">, se ignora)
        val rows = doc.select("li[role=row]")
        for (row in rows) {
            val cells = row.select("[role=cell]")
            if (cells.size < 7) continue
            try {
                parseRow(cells)?.let { entries.add(it) }
            } catch (_: Exception) {
                // Skip malformed rows
            }
        }
        return entries
    }

    /**
     * Extrae una entrada de una fila de la tabla, anclada en la celda del
     * Pokémon (sprite con alt). Compatible con 7 y 8 celdas (espaciador).
     */
    private fun parseRow(cells: Elements): PvERankingEntry? {
        // Ancla: primera celda con img de sprite (alt no vacío y src de pokemon)
        var anchor = -1
        var spriteImg: org.jsoup.nodes.Element? = null
        for (i in cells.indices) {
            val img = cells[i].select("img").firstOrNull() ?: continue
            if (img.attr("alt").trim().isNotBlank() && img.attr("src").contains("/pokemon/")) {
                anchor = i
                spriteImg = img
                break
            }
        }
        if (anchor < 1 || anchor + 3 >= cells.size) return null

        // Puesto: celda inmediatamente anterior al ancla
        val rank = cells[anchor - 1].text().trim().toIntOrNull() ?: return null

        val spriteUrl = spriteImg!!.attr("abs:src").ifBlank { spriteImg.attr("src") }
        val alt = spriteImg.attr("alt").trim()

        // Dex real desde el src: /pokemon/(\d+) — cubre /go/pokemon/815-...png
        // y /go/optimized/v1/pokemon/815-...-128.webp
        var dex = 0
        Regex("""/pokemon/(\d+)""").find(spriteUrl)?.let { m ->
            dex = m.groupValues[1].toIntOrNull() ?: 0
        }

        // Forma: badge SVG (aria-label / use href) con fallback al prefijo del alt
        val badgeLabel = cells[anchor].select("[aria-label]").firstOrNull()?.attr("aria-label")
        val form = when {
            cells[anchor].select("use[href*=gigantamax]").isNotEmpty() || badgeLabel == "Gigantamax" -> "Gigantamax"
            cells[anchor].select("use[href*=dynamax]").isNotEmpty() || badgeLabel == "Dynamax" -> "Dynamax"
            alt.startsWith("Gigantamax", ignoreCase = true) -> "Gigantamax"
            alt.startsWith("Dynamax", ignoreCase = true) -> "Dynamax"
            else -> "Normal"
        }

        // Nombre: alt sin el prefijo de forma
        val name = when (form) {
            "Gigantamax" -> alt.removePrefix("Gigantamax ").trim()
            "Dynamax" -> alt.removePrefix("Dynamax ").trim()
            else -> alt
        }
        if (name.isBlank()) return null

        // Celda ancla+1: Max Move — icono de tipo (alt) + texto
        val moveCell = cells[anchor + 1]
        val moveType = moveCell.select("img").firstOrNull()?.attr("alt")?.trim() ?: ""
        val moveText = moveCell.select("p").firstOrNull()?.text()?.trim() ?: ""

        val maxMove = if (form == "Gigantamax") {
            moveText // signature move: "G-Max Fireball"
        } else {
            // Dynamax: la celda muestra solo el tipo ("Fire") -> derivar el Max Move
            MAX_MOVE_BY_TYPE[moveType.lowercase()] ?: moveText
        }

        // Celda ancla+3: Max damage
        val damage = cells[anchor + 3].text().trim().replace(",", "").toDoubleOrNull() ?: return null

        return PvERankingEntry(
            rat = damage,
            dps = damage,
            tdo = 0.0,
            id = dex,
            name = name,
            form = form,
            shadow = false,
            level = 40,
            unreleased = false,
            fm = null,
            fmIsElite = false,
            fmType = moveType,
            cm = maxMove,
            cmIsElite = false,
            cmType = moveType,
            tier = null,
            pct = null,
            originalRank = rank,
            spriteUrl = spriteUrl
        )
    }

    fun getCachedByType(type: String): List<PvERankingEntry> = typeCache[type.lowercase()] ?: emptyList()

    fun clearCache() {
        typeCache.clear()
    }
}
