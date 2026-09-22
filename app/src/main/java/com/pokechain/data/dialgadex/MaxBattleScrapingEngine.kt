package com.pokechain.data.dialgadex

import com.pokechain.data.models.PvERankingEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.util.concurrent.TimeUnit

/**
 * Scraper de rankings de Combates Max (Dynamax/Gigantamax) desde PokemongoHub.
 *
 * Fuente: https://db.pokemongohub.net/pokemon-list/best-dynamax-per-type/{type}
 * (una tabla individualizada por tipo, p.ej. /normal, /fire, /water...)
 *
 * Columnas de la tabla:
 *   # | Name (+badge/imagen) | Fast Attack | Charged Attack (Max Move) | Max Move Damage | Max Phases
 */
class MaxBattleScrapingEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Cache por tipo: "type" -> List<PvERankingEntry>
    private val typeCache = mutableMapOf<String, List<PvERankingEntry>>()
    private val mutex = Mutex()

    companion object {
        private const val BASE_URL = "https://db.pokemongohub.net/pokemon-list/best-dynamax-per-type"

        /** Tipos válidos (coinciden con las URLs de PokemongoHub, en lowercase) */
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
    }

    /**
     * Devuelve el Top [count] (por defecto 10) de atacantes Max para un tipo.
     * El orden ya viene dado por la web (ranked by Max Move damage).
     */
    suspend fun computeMaxByType(type: String, count: Int = 10): List<PvERankingEntry> = withContext(Dispatchers.IO) {
        mutex.withLock {
            typeCache[type]?.let { cached ->
                return@withLock cached.take(count)
            }

            val results = fetchAndParseType(type)
            val withRanks = results.take(count).mapIndexed { index, entry ->
                entry.copy(originalRank = index + 1)
            }
            typeCache[type] = withRanks
            withRanks
        }
    }

    private fun fetchAndParseType(type: String): List<PvERankingEntry> {
        val request = Request.Builder()
            .url("$BASE_URL/$type")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
            .build()

        client.newCall(request).execute().use { response ->
            val html = response.body?.string() ?: return emptyList()
            return parseHtml(html)
        }
    }

    internal fun parseHtml(html: String): List<PvERankingEntry> {
        val doc: Document = Jsoup.parse(html)
        val entries = mutableListOf<PvERankingEntry>()

        val rows = doc.select("table tbody tr")
        for (row in rows) {
            val cells = row.select("td")
            if (cells.size < 6) continue
            try {
                parseRow(cells)?.let { entries.add(it) }
            } catch (_: Exception) {
                // Skip malformed rows
            }
        }
        return entries
    }

    /**
     * Extrae una entrada de una fila de la tabla.
     */
    private fun parseRow(cells: Elements): PvERankingEntry? {
        val rankText = cells[0].text().trim().removeSuffix(".")
        val rank = rankText.toIntOrNull() ?: return null

        val (name, form, dex) = parseNameAndForm(cells[1])

        val fastMove = parseMoveName(cells[2])
        val maxMove = parseMoveName(cells[3])

        val damageText = cells[4].text().trim().replace(",", "")
        val damage = damageText.toDoubleOrNull() ?: return null

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
            fm = fastMove,
            fmIsElite = false,
            fmType = null,
            cm = maxMove,
            cmIsElite = false,
            cmType = null,
            tier = null,
            pct = null,
            originalRank = rank
        )
    }

    /**
     * Extrae (nombre, forma, dex) de la celda de nombre.
     *
     * La primera imagen de la celda tiene src tipo:
     *   https://db.pokemongohub.net/images/official/thumb/815_gmax.webp
     *   https://db.pokemongohub.net/images/official/thumb/555_dynamax.webp
     * de donde se extrae el dex real y un hint de forma.
     *
     * El texto visible puede incluir "Gigantamax"/"Dynamax"/"Crowned Sword" etc.
     */
    private fun parseNameAndForm(cell: Element): Triple<String, String, Int> {
        val spriteSrc = cell.select("img").firstOrNull()?.attr("src") ?: ""

        var dex = 0
        var srcFormHint: String? = null
        Regex("""thumb/(\d+)(?:_([a-z0-9_]+))?\.webp""").find(spriteSrc)?.let { m ->
            dex = m.groupValues[1].toIntOrNull() ?: 0
            srcFormHint = m.groupValues[2].takeIf { it.isNotBlank() }
        }

        val text = cell.text().trim()

        val (name, form) = when {
            text.startsWith("Gigantamax ", ignoreCase = true) ->
                text.substring("Gigantamax ".length).trim() to "Gigantamax"
            text.startsWith("Dynamax ", ignoreCase = true) ->
                text.substring("Dynamax ".length).trim() to "Dynamax"
            text.contains("Crowned Sword", ignoreCase = true) ->
                text.replace("Crowned Sword", "", ignoreCase = true).trim() to "Crowned Sword"
            text.contains("Crowned Shield", ignoreCase = true) ->
                text.replace("Crowned Shield", "", ignoreCase = true).trim() to "Crowned Shield"
            text.contains("Eternamax", ignoreCase = true) ->
                text.replace("Eternamax", "", ignoreCase = true).trim() to "Eternamax"
            srcFormHint == "gmax" -> text to "Gigantamax"
            srcFormHint == "dynamax" -> text to "Dynamax"
            else -> text to "Normal"
        }

        return Triple(name, form, dex)
    }

    private fun parseMoveName(cell: Element): String? {
        val link = cell.select("a").firstOrNull()
        val name = link?.text() ?: cell.text()
        return name.trim().takeIf { it.isNotBlank() }
    }

    fun getCachedByType(type: String): List<PvERankingEntry> = typeCache[type] ?: emptyList()

    fun clearCache() {
        typeCache.clear()
    }
}