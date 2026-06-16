package com.pokechain.data.models

data class PvPFilterParams(
    val league: PvPLeague = PvPLeague.GREAT,
    val xlCandy: Boolean = true,
    val includeShadow: Boolean = false,
    val includeElite: Boolean = true,
    val count: Int = 20,
    val fromRank: Int = 1
)

enum class PvPLeague(val cp: Int, val cup: String? = null) {
    GREAT(1500),
    ULTRA(2500),
    MASTER(10000),
    SUNSHINE(1500, "sunshine")
}

data class PvEFilterParams(
    val unreleased: Boolean = false,
    val includeShadow: Boolean = false,
    val legendary: Boolean = true,
    val mega: Boolean = true,
    val casualShadow: Boolean = false,
    val count: Int = 20,
    val fromRank: Int = 1
)

enum class AppLanguage(val code: String) {
    ES("es"),
    EN("en")
}
