package com.pokechain.data.showcase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShowcaseCalculatorTest {

    private val size = SpeciesSize(baseHeight = 1.0, baseWeight = 10.0, maxHeight = 2.0)

    // ── Basic scoring ──────────────────────────────────────────────

    @Test
    fun `max height input gives 800 height pts`() {
        val r = ShowcaseCalculator.calculate(size, inputHeight = 2.0, inputWeight = 10.0)
        assertEquals(800.0, r.heightPts, 0.01)
    }

    @Test
    fun `half height input gives 400 height pts`() {
        val r = ShowcaseCalculator.calculate(size, inputHeight = 1.0, inputWeight = 10.0)
        assertEquals(400.0, r.heightPts, 0.01)
    }

    @Test
    fun `height above max is capped and flagged`() {
        val r = ShowcaseCalculator.calculate(size, inputHeight = 3.0, inputWeight = 10.0)
        assertEquals(800.0, r.heightPts, 0.01)
        assertTrue(r.capped)
    }

    // maxWeight = baseWeight * (ratio + 0.5) = 10 * (2.0 + 0.5) = 25
    // weightPts = 150 * cappedWeight / maxWeight
    @Test
    fun `weight at max gives 150 weight pts`() {
        val r = ShowcaseCalculator.calculate(size, inputHeight = 1.0, inputWeight = 25.0)
        assertEquals(150.0, r.weightPts, 0.01)
    }

    @Test
    fun `weight above max is capped`() {
        val r = ShowcaseCalculator.calculate(size, inputHeight = 1.0, inputWeight = 999.0)
        assertEquals(150.0, r.weightPts, 0.01)
        assertTrue(r.capped)
    }

    // ── IV scoring ─────────────────────────────────────────────────

    @Test
    fun `max IVs give 50 pts`() {
        val r = ShowcaseCalculator.calculate(size, 1.0, 10.0, atkIv = 15, defIv = 15, hpIv = 15)
        assertEquals(50.0, r.ivPts, 0.01)
        assertTrue(r.haveIvs)
    }

    @Test
    fun `no IVs gives zero ivPts and haveIvs false`() {
        val r = ShowcaseCalculator.calculate(size, 1.0, 10.0)
        assertEquals(0.0, r.ivPts, 0.01)
        assertFalse(r.haveIvs)
    }

    @Test
    fun `partial IVs give proportional pts`() {
        // (10+10+10)/45 * 50 = 33.33
        val r = ShowcaseCalculator.calculate(size, 1.0, 10.0, atkIv = 10, defIv = 10, hpIv = 10)
        assertEquals(33.33, r.ivPts, 0.01)
    }

    // ── XXL / XXS ──────────────────────────────────────────────────

    @Test
    fun `height ratio at 1_5 triggers XXL bonus`() {
        // base 1.0, ratio 1.5 → height 1.5
        val r = ShowcaseCalculator.calculate(size, inputHeight = 1.5, inputWeight = 10.0)
        assertTrue(r.isXXL)
        assertEquals(178, r.xxlPts)
    }

    @Test
    fun `height ratio below 1_5 does not trigger XXL`() {
        val r = ShowcaseCalculator.calculate(size, inputHeight = 1.49, inputWeight = 10.0)
        assertFalse(r.isXXL)
        assertEquals(0, r.xxlPts)
    }

    @Test
    fun `height ratio below 0_5 triggers XXS`() {
        val r = ShowcaseCalculator.calculate(size, inputHeight = 0.49, inputWeight = 10.0)
        assertTrue(r.isXXS)
    }

    // ── Total ──────────────────────────────────────────────────────

    @Test
    fun `total sums all components and rounds`() {
        // height 2.0 (800) + weight 25 (150) + IVs 15-15-15 (50) + XXL (178) = 1178
        val r = ShowcaseCalculator.calculate(size, 2.0, 25.0, 15, 15, 15)
        assertEquals(1178, r.total)
    }

    @Test
    fun `total without IVs omits IV pts`() {
        // height 2.0 (800) + weight 25 (150) + XXL (178) = 1128
        val r = ShowcaseCalculator.calculate(size, 2.0, 25.0)
        assertEquals(1128, r.total)
    }
}
