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
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Motor de análisis de frames de Pokémon GO (calibrado con capturas 1080×2400):
 *
 *  - Pantalla principal: OCR del nombre, PC ("PC523") y peso/altura ("95,04kg" / "0,39m")
 *  - Valoración: OCR de los PS ("75 / 75 PS") + medición de las 3 barras
 *
 * Barras (geometría calibrada, relativo a 1080×2400):
 *  - Pista fija: x = 128..502 (ancho 374px), 3 segmentos con huecos
 *  - Relleno: RGB(240,167,75) naranja (rosa en hundos) — cálido
 *  - Pista vacía: RGB(226,226,224) — gris
 *  - Panel: blanco puro. Etiquetas ("Ataque"...) también cálidas pero dispersas
 *  - La medalla de valoración (círculo cálido) se excluye por extenderse
 *    a la izquierda de la pista
 *
 * El relleno de cada barra es proporcional al IV: ratio × 15.
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

    suspend fun analyze(bitmap: Bitmap, maskRect: Rect? = null): ScanResult {
        val image = InputImage.fromBitmap(bitmap, 0)
        val text = recognizer.process(image).awaitTask()
        val lines = text.textBlocks.flatMap { block -> block.lines.map { it.text.trim() } }
            .filter { it.isNotBlank() }

        // ── PC, PS, peso y altura ──────────────────────────────────
        var cp: Int? = null
        var hp: Int? = null
        var weight: Double? = null
        var height: Double? = null
        for (line in lines) {
            if (cp == null) CP_REGEX.find(line)?.let { cp = it.groupValues[1].toIntOrNull() }
            if (hp == null) {
                // "75 / 75 PS" (PS al final) o "PS 75"
                HP_REGEX.find(line)?.let { hp = (it.groupValues[2] ?: it.groupValues[1]).toIntOrNull() }
            }
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
        val bars = analyzeBars(bitmap, maskRect)

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

    // ── Barras de la Valoración (geometría calibrada) ──────────────

    /**
     * Detecta las 3 barras y devuelve sus ratios de relleno (0.0–1.0)
     * de arriba a abajo, o null si no las encuentra con confianza.
     *
     * @param maskRect región del chip flotante (coords del bitmap) a excluir
     */
    private fun analyzeBars(bmp: Bitmap, maskRect: Rect?): List<Double>? {
        val w = bmp.width
        val h = bmp.height
        val trackStart = (TRACK_START_REL * w).toInt()
        val trackEnd = (TRACK_END_REL * w).toInt()
        val zoneTop = (ZONE_TOP_REL * h).toInt()
        val zoneBottom = (ZONE_BOTTOM_REL * h).toInt()
        val trackW = trackEnd - trackStart
        if (trackW <= 0) return null

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

        // 1) Filas de barra: cobertura (relleno + pista) > 55% de la pista
        val barRows = mutableListOf<Int>()
        for (y in zoneTop..zoneBottom) {
            if (masked(trackStart, y) || masked(trackEnd, y)) continue
            var cov = 0
            for (x in trackStart..trackEnd) {
                if (masked(x, y)) continue
                val p = bmp.getPixel(x, y)
                if (isWarm(p) || isTrack(p)) cov++
            }
            if (cov.toDouble() / trackW > 0.55) barRows.add(y)
        }

        // 2) Agrupar filas contiguas en bandas
        val bands = mutableListOf<Pair<Int, Int>>()
        var start = -1
        var prev = -1
        for (y in barRows) {
            if (start < 0) start = y
            else if (y - prev > 4) { bands.add(start to prev); start = y }
            prev = y
        }
        if (start >= 0) bands.add(start to prev)
        val minH = (5.0 * h / 2400).toInt()
        val barBands = bands.filter { (a, b) -> b - a >= minH }
        if (barBands.size != 3) return null

        // 3) Exclusión de la medalla: píxeles cálidos a la IZQUIERDA de la pista
        val medalLeftX = (0.055 * w).toInt()
        val medalRightX = (0.102 * w).toInt()
        for ((a, b) in barBands) {
            val ym = (a + b) / 2
            for (x in medalLeftX..medalRightX) {
                if (isWarm(bmp.getPixel(x, ym))) return null // es la medalla
            }
        }

        // 4) Por banda: relleno = último píxel cálido dentro de la pista
        return barBands.map { (a, b) ->
            val ym = (a + b) / 2
            var fillEnd = trackStart - 1
            for (x in trackStart..trackEnd) {
                if (masked(x, ym)) continue
                if (isWarm(bmp.getPixel(x, ym))) fillEnd = x
            }
            ((fillEnd - trackStart + 1).toDouble() / (trackW + 1)).coerceIn(0.0, 1.0)
        }
    }

    // ── Utilidades ─────────────────────────────────────────────────

    private suspend fun Task<Text>.awaitTask(): Text = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { if (cont.isActive) cont.resume(it) }
        addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
    }

    companion object {
        private val CP_REGEX = Regex("(?:PC|CP)\\s*([0-9]{2,5})")
        // "163 / 163 PS" (PS al final) o "PS 163"
        private val HP_REGEX = Regex("(?:(?:PS|HP)\\s*([0-9]{1,4}))|(?:([0-9]{1,4})\\s*/\\s*[0-9]{1,4}\\s*(?:PS|HP))")
        private val WEIGHT_REGEX = Regex("([0-9]{1,3}[.,][0-9]{2})\\s*kg")
        private val HEIGHT_REGEX = Regex("([0-9]{1,2}[.,][0-9]{2})\\s*m(?!\\w)")

        // Geometría calibrada (relativo a la pantalla)
        const val TRACK_START_REL = 0.1204  // x=128/1080
        const val TRACK_END_REL = 0.4648    // x=502/1080
        const val ZONE_TOP_REL = 0.76       // y
        const val ZONE_BOTTOM_REL = 0.92    // y
    }
}