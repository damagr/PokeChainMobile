package com.pokechain.data.pvpoke

import com.pokechain.data.models.Move
import com.pokechain.data.models.Pokemon
import kotlinx.serialization.Serializable

@Serializable
data class GameMasterResponse(
    val pokemon: List<Pokemon>,
    val moves: List<Move>? = null,
    val settings: GameMasterSettings? = null,
    val cups: List<Cup>? = null,
    val formats: List<Format>? = null,
    val pokemonTags: List<String>? = null,
    val shadowPokemon: List<String>? = null,
)

@Serializable
data class GameMasterSettings(
    val partySize: Int = 3,
    val maxBuffStages: Int = 4,
    val buffDivisor: Int = 4,
)

@Serializable
data class Cup(
    val name: String,
    val title: String,
    val include: List<Filter>? = null,
    val exclude: List<Filter>? = null,
    val league: Int? = null,
    val levelCap: Int? = null,
    val partySize: Int? = null,
)

@Serializable
data class Filter(
    val filterType: String,
    val name: String? = null,
    val values: List<String>? = null,
)

@Serializable
data class Format(
    val title: String,
    val cup: String,
    val cp: Int,
    val meta: String? = null,
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
