package com.pokechain.data.scanner

/**
 * Parser puro (testeable en JVM, sin Android/ML Kit) de las líneas OCR de la
 * ficha del Pokémon: PC, PS, peso y altura.
 *
 * Estrategia anti-ruido:
 *  - Las etiquetas de tamaño ("MÁS PESADO", "MÁS GRANDE", "XXL"…) se limpian
 *    de la línea CRUDA antes de cualquier regex o normalización. Nunca
 *    contienen dígitos+unidad, pero si el OCR las fusiona con la medida
 *    ("1,59m MÁS GRANDE" o "1,59mMÁSGRANDE") romperían el match o perderían
 *    el dato. Por eso se limpian también como subcadena sin espacios.
 *  - Primero se matchea la línea cruda limpia (preserva unidades: "PS" no
 *    queda corrompido por la normalización, y el `(?!\w)` de la altura acepta
 *    texto separado por espacio tras la medida).
 *  - Fallback: la línea normalizada, que corrige confusiones frecuentes de
 *    OCR en los dígitos (O→0, l→1, S→5, Z→2, B→8).
 *
 * Ojo: la normalización convierte "PS"→"P5" y "HP"→"H5"; el regex de PS acepta
 * ambas grafías para que el fallback también funcione.
 */
object ScanTextParser {

    data class CardNumbers(
        val cp: Int?,
        val hp: Int?,
        val heightM: Double?,
        val weightKg: Double?
    )

    fun parse(rawLines: List<String>): CardNumbers {
        var cp: Int? = null
        var hp: Int? = null
        var height: Double? = null
        var weight: Double? = null

        // 1) Líneas limpias y normalizadas (repara dígitos confundidos por el
        //    OCR: "95,O4kg"→"95.04kg"). Elegir aquí evita matchear fragmentos
        //    erróneos de la línea cruda (p. ej. "4kg" dentro de "95,O4kg").
        for (raw in rawLines) {
            val norm = normalizeOcrLine(stripSizeLabels(raw))
            if (cp == null) CP_REGEX.find(norm)?.let { cp = it.groupValues[1].toIntOrNull() }
            if (hp == null) HP_REGEX.find(norm)?.let {
                hp = (it.groupValues[2] ?: it.groupValues[1]).toIntOrNull()
            }
            if (weight == null) WEIGHT_REGEX.find(norm)?.let {
                weight = it.groupValues[1].replace(',', '.').toDoubleOrNull()
            }
            if (height == null) HEIGHT_REGEX.find(norm)?.let {
                height = it.groupValues[1].replace(',', '.').toDoubleOrNull()
            }
        }

        // 2) Rescate en línea cruda limpia (sin normalizar): si el OCR pegó
        //    otra palabra tras la unidad ("1,59m TIERRA"), el espacio separa
        //    y el `(?!\w)` de la altura sí que matchea.
        for (raw in rawLines) {
            if (cp != null && hp != null && weight != null && height != null) break
            val clean = stripSizeLabels(raw)
            if (cp == null) CP_REGEX.find(clean)?.let { cp = it.groupValues[1].toIntOrNull() }
            if (hp == null) HP_REGEX.find(clean)?.let {
                hp = (it.groupValues[2] ?: it.groupValues[1]).toIntOrNull()
            }
            if (weight == null) WEIGHT_REGEX.find(clean)?.let {
                weight = it.groupValues[1].replace(',', '.').toDoubleOrNull()
            }
            if (height == null) HEIGHT_REGEX.find(clean)?.let {
                height = it.groupValues[1].replace(',', '.').toDoubleOrNull()
            }
        }

        return CardNumbers(cp, hp, height, weight)
    }

    /**
     * Quita las etiquetas de tamaño de la línea (case/acento-insensible,
     * como subcadena: cubre líneas sueltas y fusiones sin espacio del OCR).
     * Las palabras se buscan sobre una copia plegada (minúsculas sin acentos)
     * de la misma longitud, de modo que los índices coinciden con la original.
     */
    fun stripSizeLabels(line: String): String {
        val folded = buildString(line.length) { for (c in line) append(fold(c)) }
        var out = line
        var f = folded
        for (label in SIZE_LABELS) {
            var idx = f.indexOf(label)
            while (idx >= 0) {
                out = out.removeRange(idx, idx + label.length)
                f = f.removeRange(idx, idx + label.length)
                idx = f.indexOf(label, idx)
            }
        }
        return out
    }

    /**
     * Normaliza una línea de OCR para mejorar la detección de números:
     * quita espacios, unifica separador decimal (coma → punto) y corrige
     * confusiones frecuentes (O/o→0, l/I/i→1, S/s→5, Z/z→2, B→8).
     */
    fun normalizeOcrLine(line: String): String {
        val sb = StringBuilder()
        for (c in line) {
            when (c) {
                ' ' -> { /* quitar espacios */ }
                ',' -> sb.append('.')
                'O', 'o' -> sb.append('0')
                'l', 'I', 'i' -> sb.append('1')
                'S', 's' -> sb.append('5')
                'Z', 'z' -> sb.append('2')
                'B' -> sb.append('8')
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    /** Minúscula plegando acentos, carácter a carácter (longitud preservada). */
    private fun fold(c: Char): Char {
        val lower = when (c) {
            'Á', 'À', 'Â', 'Ä', 'Ã' -> 'a'
            'É', 'È', 'Ê', 'Ë' -> 'e'
            'Í', 'Ì', 'Î', 'Ï' -> 'i'
            'Ó', 'Ò', 'Ô', 'Ö', 'Õ' -> 'o'
            'Ú', 'Ù', 'Û', 'Ü' -> 'u'
            'Ñ' -> 'n'
            'Ç' -> 'c'
            else -> c.lowercaseChar()
        }
        return when (lower) {
            'á', 'à', 'â', 'ä', 'ã' -> 'a'
            'é', 'è', 'ê', 'ë' -> 'e'
            'í', 'ì', 'î', 'ï' -> 'i'
            'ó', 'ò', 'ô', 'ö', 'õ' -> 'o'
            'ú', 'ù', 'û', 'ü' -> 'u'
            'ñ' -> 'n'
            'ç' -> 'c'
            else -> lower
        }
    }

    /**
     * Etiquetas de tamaño y rótulos de la ficha, ya plegadas (minúsculas, sin
     * acentos) y por longitud descendente. Se limpian como subcadena: cubre
     * líneas sueltas ("MÁS PESADO"), fusiones con espacio ("44,77kg MÁS
     * PESADO") y sin espacio ("1,59mMÁSGRANDE"). Incluye "mas" suelto porque
     * al fusionar sin espacio queda colgando tras quitar el sustantivo; solo
     * se usa para las líneas numéricas (matchSpecies recibe la línea cruda).
     */
    private val SIZE_LABELS = listOf(
        "mas pesado", "mas grande", "mas ligero", "mas pequeno",
        "heaviest", "largest", "lightest", "smallest",
        "heavier", "larger", "lighter", "smaller",
        "pesado", "grande", "ligero", "pequeno",
        "altura", "peso", "height", "weight",
        "mas", "xxl", "xxs"
    )

    private val CP_REGEX = Regex("(?:PC|CP)\\s*([0-9]{2,5})")
    // "PS"/"HP" en crudo y "P5"/"H5" tras normalizar (S→5)
    private val HP_REGEX =
        Regex("(?i)(?:(?:P[5S]|H[5S])\\s*([0-9]{1,4}))|(?:([0-9]{1,4})\\s*/\\s*[0-9]{1,4}\\s*(?:P[5S]|H[5S]))")
    private val WEIGHT_REGEX = Regex("(?i)([0-9]{1,3}(?:[.,][0-9]{1,2})?)\\s*kg\\.?")
    private val HEIGHT_REGEX = Regex("(?i)([0-9]{1,2}(?:[.,][0-9]{1,2})?)\\s*m\\.?(?!\\w)")
}
