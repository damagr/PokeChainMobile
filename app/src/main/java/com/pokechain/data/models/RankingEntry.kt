package com.pokechain.data.models

import kotlinx.serialization.Serializable

@Serializable
data class PvERankingEntry(
    val rat: Double,
    val dps: Double,
    val tdo: Double,
    val id: Int,
    val name: String,
    val form: String,
    val shadow: Boolean = false,
    val level: Int = 40,
    val unreleased: Boolean = false,
    val fm: String? = null,
    val fmIsElite: Boolean = false,
    val fmType: String? = null,
    val cm: String? = null,
    val cmIsElite: Boolean = false,
    val cmType: String? = null,
    val tier: String? = null,
    val pct: Double? = null,
    val originalRank: Int = 0,
)
