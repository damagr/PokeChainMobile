package com.pokechain.data.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TypeChartTest {

    // ── getMultiplier: single-type ─────────────────────────────────

    @Test
    fun `fire attacking grass is super effective 2x`() {
        assertEquals(2.0, TypeChart.getMultiplier(PokemonType.FIRE, PokemonType.GRASS), 0.001)
    }

    @Test
    fun `fire attacking water is not very effective 0_5x`() {
        assertEquals(0.5, TypeChart.getMultiplier(PokemonType.FIRE, PokemonType.WATER), 0.001)
    }

    @Test
    fun `normal attacking ghost is immune 0x`() {
        assertEquals(0.0, TypeChart.getMultiplier(PokemonType.NORMAL, PokemonType.GHOST), 0.001)
    }

    @Test
    fun `electric attacking ground is immune 0x`() {
        // Electric attacks have no effect on Ground
        assertEquals(0.0, TypeChart.getMultiplier(PokemonType.ELECTRIC, PokemonType.GROUND), 0.001)
    }

    @Test
    fun `water attacking fire is super effective 2x`() {
        assertEquals(2.0, TypeChart.getMultiplier(PokemonType.WATER, PokemonType.FIRE), 0.001)
    }

    @Test
    fun `normal attacking normal is 1x`() {
        // 1.0 is the default when not in chart
        assertEquals(1.0, TypeChart.getMultiplier(PokemonType.NORMAL, PokemonType.NORMAL), 0.001)
    }

    // ── getEffectiveness: dual-type ────────────────────────────────

    @Test
    fun `water flying takes 4x from electric`() {
        // Water/Electric 2x × Flying/Electric 2x = 4x
        val (res, weak) = TypeChart.getEffectiveness(
            listOf(PokemonType.WATER, PokemonType.FLYING)
        )
        // Electric is a weakness with combined 4x
        assertTrue("Electric should be a weakness", weak.contains(PokemonType.ELECTRIC))
        assertTrue("Electric not in resistances", !res.contains(PokemonType.ELECTRIC))
    }

    @Test
    fun `dragon flying takes 4x from ice`() {
        // Dragon/Ice 2x × Flying/Ice 2x = 4x
        val (_, weak) = TypeChart.getEffectiveness(
            listOf(PokemonType.DRAGON, PokemonType.FLYING)
        )
        assertTrue("Ice should be a weakness", weak.contains(PokemonType.ICE))
    }

    @Test
    fun `steel fairy resists dragon`() {
        // Steel resists Dragon 0.5, Fairy resists Dragon 0.0 → product 0.0
        val (res, weak) = TypeChart.getEffectiveness(
            listOf(PokemonType.STEEL, PokemonType.FAIRY)
        )
        assertTrue("Dragon should be resisted", res.contains(PokemonType.DRAGON))
        assertTrue("Dragon not a weakness", !weak.contains(PokemonType.DRAGON))
    }

    @Test
    fun `fire water resists fire 0_25x`() {
        // Fire/Fire 0.5 × Water/Fire 0.5 = 0.25
        val (res, _) = TypeChart.getEffectiveness(
            listOf(PokemonType.FIRE, PokemonType.WATER)
        )
        assertTrue("Fire should be resisted", res.contains(PokemonType.FIRE))
    }

    @Test
    fun `single type returns its resistances and weaknesses`() {
        val (res, weak) = TypeChart.getEffectiveness(listOf(PokemonType.GRASS))
        // Grass weaknesses: Fire, Ice, Poison, Flying, Bug
        assertTrue("Fire is a grass weakness", weak.contains(PokemonType.FIRE))
        // Grass resistances: Water, Electric, Grass, Ground
        assertTrue("Water is a grass resistance", res.contains(PokemonType.WATER))
    }

    // ── PokemonType.fromString ─────────────────────────────────────

    @Test
    fun `fromString matches case insensitive English`() {
        assertEquals(PokemonType.FIRE, PokemonType.fromString("Fire"))
        assertEquals(PokemonType.FIRE, PokemonType.fromString("fire"))
        assertEquals(PokemonType.FIRE, PokemonType.fromString("FIRE"))
    }

    @Test
    fun `fromString matches Spanish names`() {
        assertEquals(PokemonType.FIRE, PokemonType.fromString("Fuego"))
        assertEquals(PokemonType.FIRE, PokemonType.fromString("fuego"))
        assertEquals(PokemonType.WATER, PokemonType.fromString("Agua"))
        assertEquals(PokemonType.GRASS, PokemonType.fromString("Planta"))
    }

    @Test
    fun `fromString returns null for unknown`() {
        assertNull(PokemonType.fromString("cosmic"))
        assertNull(PokemonType.fromString(""))
        assertNull(PokemonType.fromString("bird"))
    }

    // ── displayName ────────────────────────────────────────────────

    @Test
    fun `displayName returns English in EN`() {
        assertEquals("Fire", PokemonType.FIRE.displayName(AppLanguage.EN))
    }

    @Test
    fun `displayName returns Spanish in ES`() {
        assertEquals("Fuego", PokemonType.FIRE.displayName(AppLanguage.ES))
    }
}
