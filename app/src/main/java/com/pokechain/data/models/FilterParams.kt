package com.pokechain.data.models

enum class ShadowFilter {
    INCLUDE,
    EXCLUDE,
    ONLY
}

enum class EliteFilter {
    IMPORTANT,
    NOT_IMPORTANT
}

data class PvPFilterParams(
    val league: PvPLeague = PvPLeague.GREAT,
    val xlCandy: Boolean = true,
    val shadowFilter: ShadowFilter = ShadowFilter.INCLUDE,
    val eliteFilter: EliteFilter = EliteFilter.NOT_IMPORTANT,
    val count: Int = 20
)

enum class PvPLeague(val cp: Int) {
    GREAT(1500),
    ULTRA(2500),
    MASTER(10000)
}

data class PvEFilterParams(
    val unreleased: Boolean = false,
    val shadowFilter: ShadowFilter = ShadowFilter.INCLUDE,
    val legendary: Boolean = true,
    val mega: Boolean = true,
    val count: Int = 20
)
