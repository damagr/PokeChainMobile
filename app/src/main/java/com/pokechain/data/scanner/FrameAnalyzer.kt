package com.pokechain.data.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
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
 * Motor de análisis de frames de Pokémon GO:
 *  - OCR (ML Kit, en dispositivo) del nombre, PC y PS
 *  - Medición de las 3 barras de la Valoración -> IVs (0-15)
 *
 * El relleno de cada barra es proporcional al IV: relleno/longitud × 15.
 * Los umbrales son heurísticos y pueden requerir calibración si PoGo
 * cambia su UI; la app incluye corrección manual como red de seguridad.
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

    suspend fun analyze(bitmap: Bitmap): ScanResult {
        val text = recognizer.process(InputImage.fromBitmap(bitmap, 0)).awaitTask()
        val lines = text.textBlocks.flatMap { block -> block.lines.map { it.text.trim() } }
            .filter { it.isNotBlank() }

        // ── PC y PS ────────────────────────────────────────────────
        var cp: Int? = null
        var hp: Int? = null
        // ── Peso y altura (página del Pokémon, para Exhibición) ────
        var weight: Double? = null
        var height: Double? = null
        for (line in lines) {
            if (cp == null) CP_REGEX.find(line)?.let { cp = it.groupValues[1].toIntOrNull() }
            if (hp == null) HP_REGEX.find(line)?.let { hp = it.groupValues[1].toIntOrNull() }
            if (weight == null) WEIGHT_REGEX.find(line)?.let {
                weight = it.groupValues[1].replace(',', '.').toDoubleOrNull()
            }
            if (height == null) HEIGHT_REGEX.find(line)?.let {
                height = it.groupValues[1].replace(',', '.').toDoubleOrNull()
            }
        }

        // ── Especie por nombre ─────────────────────────────────────
        val species = matchSpecies(lines)

        // ── Barras de la Valoración ────────────────────────────────
        val bars = analyzeBars(bitmap)

        return ScanResult(
            speciesName = species?.first,
            dex = species?.second,
            cp = cp,
            hp = hp,
            heightM = height,
            weightKg = weight,
            ivAtk = bars?.getOrNull(0)?.let { (it * 15).roundToInt() },
            ivDef = bars?.getOrNull(1)?.let { (it * 15).roundToInt() },
            ivSta = bars?.getOrNull(2)?.let { (it * 15).roundToInt() },
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

    // ── Barras de la Valoración ────────────────────────────────────

    /**
     * Detecta las 3 barras horizontales del panel de Valoración y devuelve
     * sus ratios de relleno (0.0–1.0) de arriba a abajo, o null si no
     * encuentra exactamente 3 bandas alineadas.
     */
    private fun analyzeBars(bmp: Bitmap): List<Double>? {
        val w = bmp.width
        val h = bmp.height
        val top = (h * 0.45).toInt() // las barras están en la mitad inferior

        // 1) Filas con un tramo horizontal claro suficientemente ancho
        data class Run(val y: Int, val start: Int, val end: Int)
        val runs = mutableListOf<Run>()
        val threshold = 60 // luminancia mínima para considerar píxel de barra (panel oscuro de fondo)
        for (y in top until h) {
            var bestStart = -1
            var bestEnd = -1
            var curStart = -1
            for (x in 0 until w) {
                val p = bmp.getPixel(x, y)
                val lum = 0.299f * Color.red(p) + 0.587f * Color.green(p) + 0.114f * Color.blue(p)
                if (lum > threshold) {
                    if (curStart < 0) curStart = x
                } else if (curStart >= 0) {
                    if (x - curStart > bestEnd - bestStart) { bestStart = curStart; bestEnd = x }
                    curStart = -1
                }
            }
            if (curStart >= 0 && w - curStart > bestEnd - bestStart) { bestStart = curStart; bestEnd = w }
            if (bestEnd - bestStart > w * 0.25) runs.add(Run(y, bestStart, bestEnd))
        }
        if (runs.size < 9) return null // cada barra ocupa varias filas (>=3)

        // 2) Agrupar filas contiguas en bandas
        data class Band(val rows: MutableList<Run>)
        val bands = mutableListOf<Band>()
        for (run in runs.sortedBy { it.y }) {
            val last = bands.lastOrNull()
            if (last != null && run.y - last.rows.last().y <= 2) last.rows.add(run)
            else bands.add(Band(mutableListOf(run)))
        }
        // Quedarnos con las 3 bandas más anchas (las barras son las más largas)
        val candidates = bands.sortedByDescending { b -> b.rows.maxOf { it.end - it.start } }.take(3)
            .sortedBy { it.rows.first().y }
        if (candidates.size < 3) return null

        // 3) Alineación: las 3 barras comparten extensión horizontal aproximada
        val starts = candidates.map { b -> b.rows.minOf { it.start } }
        val ends = candidates.map { b -> b.rows.maxOf { it.end } }
        val tolerance = w * 0.05
        if ((ends.max() - ends.min()) > tolerance) return null
        if ((starts.max() - starts.min()) > tolerance) return null

        // 4) Por banda: pista = extensión común; relleno = sub-tramo brillante
        val fillThreshold = 150 // luminancia del relleno (más brillante que la pista)
        val ratios = candidates.map { band ->
            val trackStart = band.rows.minOf { it.start }
            val trackEnd = band.rows.maxOf { it.end }
            if (trackEnd <= trackStart) return@map -1.0
            // perfil de luminancia medio por columna en las filas de la banda
            val profile = DoubleArray(trackEnd - trackStart)
            var count = 0
            for (row in band.rows) {
                if (row.start > trackStart || row.end < trackEnd) continue
                count++
                for (x in trackStart until trackEnd) {
                    val p = bmp.getPixel(x, row.y)
                    profile[x - trackStart] += 0.299f * Color.red(p) + 0.587f * Color.green(p) + 0.114f * Color.blue(p)
                }
            }
            if (count == 0) return@map -1.0
            for (i in profile.indices) profile[i] /= count
            // desde la derecha, retroceder hasta encontrar el final del relleno brillante
            var fillEnd = -1
            for (i in profile.indices.reversed()) {
                if (profile[i] >= fillThreshold) { fillEnd = i + 1; break }
            }
            if (fillEnd <= 0) return@map 0.0
            // el relleno empieza siempre por la izquierda de la pista
            (fillEnd.toDouble() / (trackEnd - trackStart)).coerceIn(0.0, 1.0)
        }
        if (ratios.any { it < 0 }) return null
        return ratios
    }

    // ── Utilidades ─────────────────────────────────────────────────

    private suspend fun Task<Text>.awaitTask(): Text = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { if (cont.isActive) cont.resume(it) }
        addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
    }

    companion object {
        private val CP_REGEX = Regex("(?:PC|CP)\\s*([0-9]{2,5})")
        private val HP_REGEX = Regex("(?:PS|HP)\\s*([0-9]{2,4})")
        // PoGo muestra "12,54 kg" (ES) o "12.54 kg" (EN) — coma y punto
        private val WEIGHT_REGEX = Regex("([0-9]{1,3}[.,][0-9]{2})\\s*kg")
        private val HEIGHT_REGEX = Regex("([0-9]{1,2}[.,][0-9]{2})\\s*m(?!\\w)")
    }
}