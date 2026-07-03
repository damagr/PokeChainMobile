package com.pokechain.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Pokemon(
    val speciesId: String,
    val speciesName: String,
    val dex: Int,
    val types: List<String>,
    val baseStats: BaseStats,
    val tags: List<String>? = null,
    val family: Family? = null,
    val eliteMoves: List<String>? = null,
    val released: Boolean = true,
    val defaultIVs: Map<String, List<Double>>? = null,
)

@Serializable
data class BaseStats(
    val atk: Int,
    val def: Int,
    val hp: Int
)

@Serializable
data class Family(
    val parent: String? = null,
)
