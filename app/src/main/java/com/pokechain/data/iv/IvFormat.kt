package com.pokechain.data.iv

import kotlin.math.roundToInt

/** Formato numérico de IVs para renombrar en Pokémon GO (superíndice %). */
object IvFormat {
    private const val SUPERS = "⁰¹²³⁴⁵⁶⁷⁸⁹"

    /** 93 → "⁹³" (sin i18n: son dígitos). */
    fun superscript(perfection: Int): String =
        perfection.toString().map { c -> if (c.isDigit()) SUPERS[c - '0'] else c }.joinToString("")

    /** Desde la suma de IVs (0-45): 40 → "⁸⁹" (40/45 ≈ 89%). */
    fun superscriptFromSum(ivSum: Int): String = superscript((ivSum / 45.0 * 100.0).roundToInt())
}