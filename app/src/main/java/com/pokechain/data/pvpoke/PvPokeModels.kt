package com.pokechain.data.pvpoke

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
)
