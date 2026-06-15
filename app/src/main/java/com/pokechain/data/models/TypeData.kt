package com.pokechain.data.models

import androidx.compose.ui.graphics.Color

enum class PokemonType(
    val color: Color,
    val nameEs: String,
    val nameEn: String
) {
    NORMAL(  Color(0xFFA8A878), "Normal",   "Normal"),
    FIRE(    Color(0xFFF08030), "Fuego",    "Fire"),
    WATER(   Color(0xFF6890F0), "Agua",     "Water"),
    GRASS(   Color(0xFF78C850), "Planta",   "Grass"),
    ELECTRIC(Color(0xFFF8D030), "Eléctrico", "Electric"),
    ICE(     Color(0xFF98D8D8), "Hielo",    "Ice"),
    FIGHTING(Color(0xFFC03028), "Lucha",    "Fighting"),
    POISON(  Color(0xFFA040A0), "Veneno",   "Poison"),
    GROUND(  Color(0xFFE0C068), "Tierra",   "Ground"),
    FLYING(  Color(0xFFA890F0), "Volador",  "Flying"),
    PSYCHIC( Color(0xFFF85888), "Psíquico", "Psychic"),
    BUG(     Color(0xFFA8B820), "Bicho",    "Bug"),
    ROCK(    Color(0xFFB8A038), "Roca",     "Rock"),
    GHOST(   Color(0xFF705898), "Fantasma", "Ghost"),
    DRAGON(  Color(0xFF7038F8), "Dragón",   "Dragon"),
    DARK(    Color(0xFF705848), "Siniestro", "Dark"),
    STEEL(   Color(0xFFB8B8D0), "Acero",    "Steel"),
    FAIRY(   Color(0xFFEE99AC), "Hada",     "Fairy");

    fun displayName(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> nameEn
        AppLanguage.ES -> nameEs
    }

    companion object {
        fun fromString(type: String): PokemonType? = entries.find {
            it.name.equals(type, ignoreCase = true) ||
            it.nameEs.equals(type, ignoreCase = true)
        }
    }
}

/**
 * Type effectiveness for Dialgadex-style display.
 *
 * The chart maps (defendingType, attackingType) -> multiplier.
 *  2.0 = super effective (weakness)
 *  0.5 = not very effective (resistance)
 *  0.0 = immune
 *  1.0 = normal damage (not stored explicitly)
 *
 * For dual-type Pokémon the multipliers are multiplied.
 */
object TypeChart {

    private val chart: Map<PokemonType, Map<PokemonType, Double>> = mapOf(
        PokemonType.NORMAL to mapOf(
            PokemonType.FIGHTING to 2.0,
            PokemonType.GHOST to 0.0,
        ),
        PokemonType.FIRE to mapOf(
            PokemonType.FIRE to 0.5,
            PokemonType.WATER to 2.0,
            PokemonType.GRASS to 0.5,
            PokemonType.ICE to 0.5,
            PokemonType.GROUND to 2.0,
            PokemonType.BUG to 0.5,
            PokemonType.ROCK to 2.0,
            PokemonType.STEEL to 0.5,
            PokemonType.FAIRY to 0.5,
        ),
        PokemonType.WATER to mapOf(
            PokemonType.FIRE to 0.5,
            PokemonType.WATER to 0.5,
            PokemonType.ELECTRIC to 2.0,
            PokemonType.GRASS to 2.0,
            PokemonType.ICE to 0.5,
            PokemonType.STEEL to 0.5,
        ),
        PokemonType.GRASS to mapOf(
            PokemonType.FIRE to 2.0,
            PokemonType.WATER to 0.5,
            PokemonType.ELECTRIC to 0.5,
            PokemonType.GRASS to 0.5,
            PokemonType.ICE to 2.0,
            PokemonType.POISON to 2.0,
            PokemonType.GROUND to 0.5,
            PokemonType.FLYING to 2.0,
            PokemonType.BUG to 2.0,
        ),
        PokemonType.ELECTRIC to mapOf(
            PokemonType.ELECTRIC to 0.5,
            PokemonType.GROUND to 2.0,
            PokemonType.FLYING to 0.5,
            PokemonType.STEEL to 0.5,
        ),
        PokemonType.ICE to mapOf(
            PokemonType.FIRE to 2.0,
            PokemonType.ICE to 0.5,
            PokemonType.FIGHTING to 2.0,
            PokemonType.ROCK to 2.0,
            PokemonType.STEEL to 2.0,
        ),
        PokemonType.FIGHTING to mapOf(
            PokemonType.FLYING to 2.0,
            PokemonType.PSYCHIC to 2.0,
            PokemonType.BUG to 0.5,
            PokemonType.ROCK to 0.5,
            PokemonType.DARK to 0.5,
            PokemonType.FAIRY to 2.0,
        ),
        PokemonType.POISON to mapOf(
            PokemonType.GRASS to 0.5,
            PokemonType.FIGHTING to 0.5,
            PokemonType.POISON to 0.5,
            PokemonType.GROUND to 2.0,
            PokemonType.PSYCHIC to 2.0,
            PokemonType.BUG to 0.5,
            PokemonType.FAIRY to 0.5,
        ),
        PokemonType.GROUND to mapOf(
            PokemonType.WATER to 2.0,
            PokemonType.ELECTRIC to 0.0,
            PokemonType.GRASS to 2.0,
            PokemonType.ICE to 2.0,
            PokemonType.POISON to 0.5,
            PokemonType.ROCK to 0.5,
        ),
        PokemonType.FLYING to mapOf(
            PokemonType.ELECTRIC to 2.0,
            PokemonType.GRASS to 0.5,
            PokemonType.ICE to 2.0,
            PokemonType.FIGHTING to 0.5,
            PokemonType.GROUND to 0.0,
            PokemonType.BUG to 0.5,
            PokemonType.ROCK to 2.0,
        ),
        PokemonType.PSYCHIC to mapOf(
            PokemonType.FIGHTING to 0.5,
            PokemonType.PSYCHIC to 0.5,
            PokemonType.BUG to 2.0,
            PokemonType.GHOST to 2.0,
            PokemonType.DARK to 2.0,
        ),
        PokemonType.BUG to mapOf(
            PokemonType.FIRE to 2.0,
            PokemonType.GRASS to 0.5,
            PokemonType.FIGHTING to 0.5,
            PokemonType.GROUND to 0.5,
            PokemonType.FLYING to 2.0,
            PokemonType.ROCK to 2.0,
        ),
        PokemonType.ROCK to mapOf(
            PokemonType.NORMAL to 0.5,
            PokemonType.FIRE to 0.5,
            PokemonType.WATER to 2.0,
            PokemonType.GRASS to 2.0,
            PokemonType.FIGHTING to 2.0,
            PokemonType.POISON to 0.5,
            PokemonType.GROUND to 2.0,
            PokemonType.FLYING to 0.5,
            PokemonType.STEEL to 2.0,
        ),
        PokemonType.GHOST to mapOf(
            PokemonType.NORMAL to 0.0,
            PokemonType.FIGHTING to 0.0,
            PokemonType.POISON to 0.5,
            PokemonType.BUG to 0.5,
            PokemonType.GHOST to 2.0,
            PokemonType.DARK to 2.0,
        ),
        PokemonType.DRAGON to mapOf(
            PokemonType.FIRE to 0.5,
            PokemonType.WATER to 0.5,
            PokemonType.ELECTRIC to 0.5,
            PokemonType.GRASS to 0.5,
            PokemonType.ICE to 2.0,
            PokemonType.DRAGON to 2.0,
            PokemonType.FAIRY to 2.0,
        ),
        PokemonType.DARK to mapOf(
            PokemonType.FIGHTING to 2.0,
            PokemonType.PSYCHIC to 0.0,
            PokemonType.BUG to 2.0,
            PokemonType.GHOST to 0.5,
            PokemonType.DARK to 0.5,
            PokemonType.FAIRY to 2.0,
        ),
        PokemonType.STEEL to mapOf(
            PokemonType.NORMAL to 0.5,
            PokemonType.FIRE to 2.0,
            PokemonType.GRASS to 0.5,
            PokemonType.ICE to 0.5,
            PokemonType.FIGHTING to 2.0,
            PokemonType.POISON to 0.0,
            PokemonType.GROUND to 2.0,
            PokemonType.FLYING to 0.5,
            PokemonType.PSYCHIC to 0.5,
            PokemonType.BUG to 0.5,
            PokemonType.ROCK to 0.5,
            PokemonType.DRAGON to 0.5,
            PokemonType.STEEL to 0.5,
            PokemonType.FAIRY to 0.5,
        ),
        PokemonType.FAIRY to mapOf(
            PokemonType.FIGHTING to 0.5,
            PokemonType.POISON to 2.0,
            PokemonType.BUG to 0.5,
            PokemonType.DRAGON to 0.0,
            PokemonType.DARK to 0.5,
            PokemonType.STEEL to 2.0,
        ),
    )

    /**
     * Single-type multiplier: attacking type vs defending type.
     */
    fun getMultiplier(attacking: PokemonType, defending: PokemonType): Double {
        return chart[defending]?.get(attacking) ?: 1.0
    }

    /**
     * Returns the combined effectiveness for a Pokémon with the given types.
     * @return Pair of (resistances, weaknesses) — attacking types grouped by effect.
     */
    fun getEffectiveness(
        defendingTypes: List<PokemonType>
    ): Pair<List<PokemonType>, List<PokemonType>> {
        val multipliers = mutableMapOf<PokemonType, Double>()

        for (atkType in PokemonType.entries) {
            var mult = 1.0
            for (defType in defendingTypes) {
                val m = chart[defType]?.get(atkType) ?: 1.0
                mult *= m
            }
            multipliers[atkType] = mult
        }

        val resistances = multipliers
            .filter { it.value < 1.0 }
            .keys
            .toList()

        val weaknesses = multipliers
            .filter { it.value > 1.0 }
            .keys
            .toList()

        return resistances to weaknesses
    }
}
