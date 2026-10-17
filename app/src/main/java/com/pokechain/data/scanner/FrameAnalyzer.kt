package com.pokechain.data.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.pokechain.data.dialgadex.NameTranslator
import com.pokechain.data.models.AppLanguage
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Motor de análisis de frames de Pokémon GO (calibrado con capturas 1080×2400).
 *
 * Por etapa del flujo:
 *  - [analyzeLooking]: OCR de la página del Pokémon — nombre, PC, PS
 *    ("75 / 75 PS") y peso/altura ("95,04kg" / "0,39m"). NO analiza barras.
 *  - [readBars]: solo píxeles — mide las 3 barras de la Valoración
 *    (pista x=128..502, relleno RGB(240,167,75) naranja/rosa).
 *  - [barsPresence]: chequeo barato (sin OCR) de si la Valoración sigue en pantalla.
 *
 * El relleno de cada barra es proporcional al IV: ratio × 15.
 * Las etiquetas ("Ataque"...) comparten el color cálido de las barras: se
 * distinguen por COBERTURA de pista (la barra cubre >55% de x=130..502).
 * La medalla de valoración se excluye por extenderse a la izquierda de la pista.
 */
class FrameAnalyzer(context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val translator = NameTranslator(context)

    /** Nombre (lowercase) -> dex, mezclando ES + EN (el idioma de PoGo depende del dispositivo). */
    private val nameToDex: Map<String, Int> = buildMap {
        for (lang in listOf(AppLanguage.EN, AppLanguage.ES)) {
            translator.getAllNames(lang).forEach { (dex, name) ->
                val key = name.lowercase().trim()
                if (key.isNotBlank() && !containsKey(key)) put(key, dex)
            }
        }
    }

    // ── Etapa LOOKING_POKEMON: OCR de la página del Pokémon ────────

    suspend fun analyzeLooking(bitmap: Bitmap, maskRect: Rect? = null): ScanResult {
        val image = InputImage.fromBitmap(bitmap, 0)
        val text = recognizer.process(image).awaitTask()
        val lines = text.textBlocks.flatMap { block -> block.lines.map { it.text.trim() } }
            .filter { it.isNotBlank() }

        var cp: Int? = null
        var hp: Int? = null
        var weight: Double? = null
        var height: Double? = null
        for (line in lines) {
            if (cp == null) CP_REGEX.find(line)?.let { cp = it.groupValues[1].toIntOrNull() }
            if (hp == null) HP_REGEX.find(line)?.let { hp = (it.groupValues[2] ?: it.groupValues[1]).toIntOrNull() }
            if (weight == null) WEIGHT_REGEX.find(line)?.let {
                weight = it.groupValues[1].replace(',', '.').toDoubleOrNull()
            }
            if (height == null) HEIGHT_REGEX.find(line)?.let {
                height = it.groupValues[1].replace(',', '.').toDoubleOrNull()
            }
        }

        val species = matchSpecies(lines)

        return ScanResult(
            speciesName = species?.first,
            dex = species?.second,
            cp = cp,
            hp = hp,
            heightM = height,
            weightKg = weight,
        )
    }

    // ── Especie: exacto o Levenshtein ≤ 2 ──────────────────────────

    private fun matchSpecies(lines: List<String>): Pair<String, Int>? {
        for (line in lines.sortedByDescending { it.length }) {
            val norm = line.lowercase().trim()
            nameToDex[norm]?.let { return line to it }
            var best: Pair<Int, Int>? = null // distancia to dex
            for ((name, dex) in nameToDex) {
                if (abs(name.length - norm.length) > 2) continue
                val d = levenshtein(norm, name)
                if (d <= 2 && (best == null || d < best.first)) best = d to dex
            }
            best?.let { return line to it.second }
        }
        return null
    }

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        for (i in a.indices) {
            val cur = IntArray(b.length + 1)
            cur[0] = i + 1
            for (j in b.indices) {
                cur[j + 1] = minOf(prev[j + 1] + 1, cur[j] + 1, prev[j] + if (a[i] == b[j]) 0 else 1)
            }
            prev = cur
        }
        return prev[b.length]
    }

    // ── Etapa POKEMON_READ: medir las 3 barras (sin OCR) ───────────

    /** Devuelve (atk, def, sta) como IVs 0-15, o null si no hay 3 barras fiables. */
    fun readBars(bitmap: Bitmap, maskRect: Rect? = null): Triple<Int, Int, Int>? {
        val bands = barBands(bitmap, maskRect) ?: return null
        val ratios = mutableListOf<Double>()
        for (band in bands) {
            val ym = (band.yStart + band.yEnd) / 2
            // El relleno cálido va de izquierda a derecha; la pista gris NO es cálida
            var fillEnd = band.xStart - 1
            for (x in band.xStart..band.xEnd) {
                val p = bitmap.getPixel(x, ym)
                if (isWarmColor(Color.red(p), Color.green(p), Color.blue(p))) fillEnd = x
            }
            ratios.add(((fillEnd - band.xStart + 1).toDouble() / (band.xEnd - band.xStart + 1)).coerceIn(0.0, 1.0))
        }
        return Triple(
            (ratios[0] * 15).roundToInt(),
            (ratios[1] * 15).roundToInt(),
            (ratios[2] * 15).roundToInt()
        )
    }

    // ── Etapa IV_READ: presencia barata de la Valoración (sin OCR) ─

    /** true si la Valoración (las 3 barras) sigue en pantalla. */
    fun barsPresence(bitmap: Bitmap, maskRect: Rect? = null): Boolean {
        val covered = coveredBarRows(bitmap, maskRect)
        val minRows = (12.0 * bitmap.height / 2400).toInt()
        return covered.size >= minRows
    }

    // ── Detección de bandas (común) ────────────────────────────────

    /**
     * Filas de la zona de barras con cobertura (relleno + pista) > 55% de la pista.
     */
    private fun coveredBarRows(bmp: Bitmap, maskRect: Rect?): List<Int> {
        val w = bmp.width
        val h = bmp.height
        val trackStart = (TRACK_START_REL * w).toInt()
        val trackEnd = (TRACK_END_REL * w).toInt()
        val zoneTop = (ZONE_TOP_REL * h).toInt()
        val zoneBottom = (ZONE_BOTTOM_REL * h).toInt()
        val trackW = trackEnd - trackStart
        if (trackW <= 0) return emptyList()

        fun isWarm(p: Int): Boolean {
            val r = Color.red(p); val g = Color.green(p); val b = Color.blue(p)
            return r > 150 && r - b > 40 && r - g > 20
        }
        fun isTrack(p: Int): Boolean {
            val r = Color.red(p); val g = Color.green(p); val b = Color.blue(p)
            return abs(r - 226) < 22 && abs(g - 226) < 22 && abs(b - 224) < 22
        }
        fun masked(x: Int, y: Int): Boolean =
            maskRect != null && x >= maskRect.left && x <= maskRect.right &&
                y >= maskRect.top && y <= maskRect.bottom

        val rows = mutableListOf<Int>()
        for (y in zoneTop..zoneBottom) {
            if (masked(trackStart, y) || masked(trackEnd, y)) continue
            var cov = 0
            for (x in trackStart..trackEnd) {
                if (masked(x, y)) continue
                val p = bmp.getPixel(x, y)
                if (isWarm(p) || isTrack(p)) cov++
            }
            if (cov.toDouble() / trackW > 0.55) rows.add(y)
        }
        return rows
    }

    private data class BarBand(val yStart: Int, val yEnd: Int, val xStart: Int, val xEnd: Int)

    /**
     * Los 3 grupos de filas cubiertas = las barras. La medalla (círculo cálido
     * que se extiende a la izquierda de la pista) y las etiquetas (texto
     * disperso, <55% de cobertura) quedan excluidas.
     */
    private fun barBands(bmp: Bitmap, maskRect: Rect?): List<BarBand>? {
        val rows = coveredBarRows(bmp, maskRect)
        val h = bmp.height
        val minH = (5.0 * h / 2400).toInt()
        val bands = mutableListOf<Pair<Int, Int>>()
        var start = -1
        var prev = -1
        for (y in rows) {
            if (start < 0) start = y
            else if (y - prev > 4) { bands.add(start to prev); start = y }
            prev = y
        }
        if (start >= 0) bands.add(start to prev)
        val barBands = bands.filter { (a, b) -> b - a >= minH }
        if (barBands.size != 3) return null

        val w = bmp.width
        val trackStart = (TRACK_START_REL * w).toInt()
        val trackEnd = (TRACK_END_REL * w).toInt()
        val medalLeftX = (0.055 * w).toInt()
        val medalRightX = (0.102 * w).toInt()

        fun isWarmAt(x: Int, y: Int): Boolean {
            val p = bmp.getPixel(x, y)
            val r = Color.red(p); val g = Color.green(p); val b = Color.blue(p)
            return r > 150 && r - b > 40 && r - g > 20
        }

        // La medalla se extiende a la izquierda de la pista.
        // Si todas las bandas tienen píxeles cálidos en el rango de la medalla,
        // es la medalla y no las barras: descartar. Un par de bandas con píxeles
        // cálidos ahí puede ser un reflejo falso: no bloquear la lectura.
        var medalBands = 0
        for ((a, b) in barBands) {
            val ym = (a + b) / 2
            for (x in medalLeftX..medalRightX) {
                if (isWarmAt(x, ym)) { medalBands++; break }
            }
        }
        if (medalBands == barBands.size) return null

        // Extensión de pista (gris + cálido) en la fila central de cada banda
        return barBands.map { (a, b) ->
            val ym = (a + b) / 2
            var t0 = Int.MAX_VALUE
            var t1 = Int.MIN_VALUE
            for (x in trackStart..trackEnd) {
                if (isWarmAt(x, ym) || isTrackAt(bmp, x, ym)) { t0 = minOf(t0, x); t1 = maxOf(t1, x) }
            }
            if (t0 > t1) return null
            BarBand(a, b, t0, t1)
        }
    }

    private fun isTrackAt(bmp: Bitmap, x: Int, y: Int): Boolean {
        val p = bmp.getPixel(x, y)
        val r = Color.red(p); val g = Color.green(p); val b = Color.blue(p)
        return abs(r - 226) < 22 && abs(g - 226) < 22 && abs(b - 224) < 22
    }

    private fun isWarmColor(r: Int, g: Int, b: Int): Boolean = r > 150 && r - b > 40 && r - g > 20

    // ── Utilidades ─────────────────────────────────────────────────

    private suspend fun Task<Text>.awaitTask(): Text = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { if (cont.isActive) cont.resume(it) }
        addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
    }

    companion object {
        private val CP_REGEX = Regex("(?:PC|CP)\\s*([0-9]{2,5})")
        private val HP_REGEX = Regex("(?:(?:PS|HP)\\s*([0-9]{1,4}))|(?:([0-9]{1,4})\\s*/\\s*[0-9]{1,4}\\s*(?:PS|HP))")
        private val WEIGHT_REGEX = Regex("(?i)([0-9]{1,3}[.,][0-9]{1,2})\\s*kg\\.?")
        private val HEIGHT_REGEX = Regex("(?i)([0-9]{1,2}[.,][0-9]{1,2})\\s*m\\.?(?!\\w)")

        // Geometría calibrada (relativo a la pantalla)
        const val TRACK_START_REL = 0.1204  // x=128/1080
        const val TRACK_END_REL = 0.4648    // x=502/1080
        const val ZONE_TOP_REL = 0.76       // y
        const val ZONE_BOTTOM_REL = 0.92    // y
    }
}