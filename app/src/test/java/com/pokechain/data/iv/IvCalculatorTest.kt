package com.pokechain.data.iv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IvCalculatorTest {

    // ── calculateCp ────────────────────────────────────────────────

    @Test
    fun `calculateCp matches known Pikachu value`() {
        // Pikachu base stats: atk=112, def=96, sta=111; lvl 40 cpm=0.7903
        // IVs 15/15/15 → CP 938 (floor of 127 * sqrt(111) * sqrt(126) * 0.7903² / 10)
        val cp = IvCalculator.calculateCp(112, 96, 111, 15, 15, 15, 0.7903)
        assertEquals(938, cp)
    }

    @Test
    fun `calculateCp zero IVs lower than max IVs`() {
        val cpm = 0.7903
        val maxIv = IvCalculator.calculateCp(200, 200, 200, 15, 15, 15, cpm)
        val zeroIv = IvCalculator.calculateCp(200, 200, 200, 0, 0, 0, cpm)
        assertTrue("max IV CP must exceed zero IV CP", maxIv > zeroIv)
    }

    // ── calculateHp ────────────────────────────────────────────────

    @Test
    fun `calculateHp floors correctly`() {
        // base sta 111 + iv 15 = 126; cpm 0.7903 → 126 * 0.7903 = 99.58 → floor 99
        val hp = IvCalculator.calculateHp(111, 15, 0.7903)
        assertEquals(99, hp)
    }

    @Test
    fun `calculateHp minimum is 10 in-game, but formula gives raw floor`() {
        // Very low stats → formula floor; in-game floors to 10 but the calculator
        // returns the raw formula value. Document the behaviour.
        val hp = IvCalculator.calculateHp(1, 0, 0.094)
        assertEquals(0, hp) // 1 * 0.094 = 0.094 → floor 0
    }

    // ── calculatePerfection ────────────────────────────────────────

    @Test
    fun `perfection 100 for 15-15-15`() {
        assertEquals(100.0, IvCalculator.calculatePerfection(15, 15, 15), 0.001)
    }

    @Test
    fun `perfection 0 for 0-0-0`() {
        assertEquals(0.0, IvCalculator.calculatePerfection(0, 0, 0), 0.001)
    }

    @Test
    fun `perfection 51 for 10-10-3`() {
        // (10+10+3)/45 * 100 = 51.111...
        assertEquals(51.111, IvCalculator.calculatePerfection(10, 10, 3), 0.01)
    }

    // ── findLevel ──────────────────────────────────────────────────

    @Test
    fun `findLevel returns all matching levels for given CP`() {
        // Pikachu 15/15/15 produces CP 938 at level 40
        val results = IvCalculator.findLevel(112, 96, 111, 15, 15, 15, targetCp = 938)
        assertTrue("expected at least one matching level", results.isNotEmpty())
        // All results must match the target CP
        results.forEach { assertEquals(938, it.cp) }
        // Results sorted ascending by level
        val levels = results.map { it.level }
        assertEquals(levels.sorted(), levels)
    }

    @Test
    fun `findLevel empty when no level matches`() {
        // absurd CP that no level can produce with these stats
        val results = IvCalculator.findLevel(10, 10, 10, 0, 0, 0, targetCp = 99999)
        assertTrue(results.isEmpty())
    }

    @Test
    fun `findLevel carries perfection and hp`() {
        val results = IvCalculator.findLevel(112, 96, 111, 15, 15, 15, targetCp = 938)
        assertTrue("lvl 40 should be among results", results.any { it.level == 40.0 })
        val lvl40 = results.first { it.level == 40.0 }
        assertEquals(100.0, lvl40.perfection, 0.001)
        assertEquals(99, lvl40.hp)
    }

    // ── calculateAtLevel ───────────────────────────────────────────

    @Test
    fun `calculateAtLevel returns cp and hp pair`() {
        val (cp, hp) = IvCalculator.calculateAtLevel(112, 96, 111, 15, 15, 15, 40.0)!!
        assertEquals(938, cp)
        assertEquals(99, hp)
    }

    @Test
    fun `calculateAtLevel null for unknown level`() {
        // 40.25 is not in the table (only 0.5 increments)
        val result = IvCalculator.calculateAtLevel(112, 96, 111, 15, 15, 15, 40.25)
        assertEquals(null, result)
    }

    // ── getPowerUpCost ─────────────────────────────────────────────

    @Test
    fun `powerUp cost zero when from equals to`() {
        val cost = IvCalculator.getPowerUpCost(20.0, 20.0)
        assertEquals(0, cost.dust)
        assertEquals(0, cost.candy)
        assertEquals(0, cost.xlCandy)
    }

    @Test
    fun `powerUp cost zero when to is below from`() {
        val cost = IvCalculator.getPowerUpCost(30.0, 20.0)
        assertEquals(0, cost.dust)
    }

    @Test
    fun `powerUp one step from lvl 19 to 19_5 costs 2500 dust 2 candy`() {
        // costPerPowerUp(19.0): 19 < 21 → 2500 dust, 2 candy
        val cost = IvCalculator.getPowerUpCost(19.0, 19.5)
        assertEquals(2500, cost.dust)
        assertEquals(2, cost.candy)
        assertEquals(0, cost.xlCandy)
    }

    @Test
    fun `powerUp from 39_5 to 40_5 crosses XL boundary`() {
        // 39.5 → 40.0 uses regular candy (10000 dust, 15 candy)
        // 40.0 → 40.5 uses XL (10000 dust, 0 candy, 10 XL)
        val cost = IvCalculator.getPowerUpCost(39.5, 40.5)
        assertEquals(20000, cost.dust)
        assertEquals(15, cost.candy)
        assertEquals(10, cost.xlCandy)
    }

    @Test
    fun `powerUp full climb 1 to 40 has positive dust candy and no XL`() {
        val cost = IvCalculator.getPowerUpCost(1.0, 40.0)
        assertTrue(cost.dust > 0)
        assertTrue(cost.candy > 0)
        assertEquals(0, cost.xlCandy) // below 40 → no XL
    }
}
