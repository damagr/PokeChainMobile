package com.pokechain.data.pvpoke

import com.pokechain.data.models.Move
import com.pokechain.data.models.Pokemon
import kotlinx.serialization.Serializable

@Serializable
data class GameMasterResponse(
    val pokemon: List<Pokemon>,
    val shadowPokemon: List<String>? = null,
)

@Serializable
data class PvPRawEntry(
    val speciesId: String,
    val speciesName: String,
    val score: Double,
    val moveset: List<String>,
    val scores: List<Double>? = null,
    val stats: RawStats? = null,
    val editorScore: Int? = null,
    val editorNotes: String? = null,
)

@Serializable
data class RawStats(
    val product: Int,
    val atk: Double,
    val `def`: Double,
    val hp: Int,
)
