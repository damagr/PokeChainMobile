package com.pokechain.data.models

import kotlinx.serialization.Serializable

@Serializable
data class PvPRankingEntry(
    val speciesId: String,
    val speciesName: String,
    val score: Double,
    val moveset: List<String>,
    val moves: MoveUsage? = null,
    val matchups: List<Matchup>? = null,
    val counters: List<Counter>? = null,
    val scores: List<Double>? = null,
    val stats: RankingStats? = null,
    val editorScore: Int? = null,
)

@Serializable
data class MoveUsage(
    val fastMoves: List<MoveEntry>,
    val chargedMoves: List<MoveEntry>
)

@Serializable
data class MoveEntry(
    val moveId: String,
    val uses: Int
)

@Serializable
data class Matchup(
    val opponent: String,
    val rating: Int,
    val opRating: Int? = null
)

@Serializable
data class Counter(
    val opponent: String,
    val rating: Int
)

@Serializable
data class RankingStats(
    val product: Int,
    val atk: Double,
    val `def`: Double,
    val hp: Int
)

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
