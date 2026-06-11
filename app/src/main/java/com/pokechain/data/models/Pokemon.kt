package com.pokechain.data.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Pokemon(
    val speciesId: String,
    val speciesName: String,
    val dex: Int,
    val types: List<String>,
    val baseStats: BaseStats,
    val fastMoves: List<String>,
    val chargedMoves: List<String>,
    val tags: List<String>? = null,
    val family: Family? = null,
    val eliteMoves: List<String>? = null,
    val level25CP: Double? = null,
    val buddyDistance: Int = 3,
    val thirdMoveCost: JsonElement? = null,
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
    val id: String? = null,
    val parent: String? = null,
    val evolutions: List<String>? = null
)

@Serializable
data class Move(
    val moveId: String,
    val name: String,
    val type: String,
    val power: Int = 0,
    val energy: Int = 0,
    val energyGain: Int = 0,
    val cooldown: Int = 0,
    val turns: Double? = null,
)
