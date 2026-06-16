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
    SUNSHINE(1500, "sunshine"),
    CATCH(1500, "catch"),
    COLOR(1500, "color"),
    ELEMENT(1500, "element"),
    EVOLUTION(1500, "evolution"),
    FANTASY(1500, "fantasy"),
    ULTRA_FANTASY(2500, "fantasy"),
    FIGHTING(1500, "fighting"),
    FLYING(1500, "flying"),
    FOSSIL(1500, "fossil"),
    HALLOWEEN(1500, "halloween"),
    ULTRA_HALLOWEEN(2500, "halloween"),
    HISUI(1500, "hisui"),
    JUNGLE(1500, "jungle"),
    KANTO(1500, "kanto"),
    LOVE(1500, "love"),
    MOUNTAIN(1500, "mountain"),
    PREMIER(1500, "premier"),
    ULTRA_PREMIER(2500, "premier"),
    PSYCHIC(1500, "psychic"),
    REMIX(1500, "remix"),
    ULTRA_REMIX(2500, "remix"),
    RETRO(1500, "retro"),
    SPRING(1500, "spring"),
    SUMMER(1500, "summer"),
    ULTRA_SUMMER(2500, "summer"),
    WEATHER(1500, "weather"),
    ULTRA_WEATHER(2500, "weather"),
    WILLPOWER(1500, "willpower"),
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
