package com.pokechain.data.iv

import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Result of an IV level search.
 * @param level The Pokémon level (e.g. 20.0, 20.5)
 * @param cp Calculated CP at this level (should match user input)
 * @param hp Estimated HP at this level
 * @param perfection IV perfection percentage (sum of IVs / 45 × 100)
 */
data class IvResult(
    val level: Double,
    val cp: Int,
    val hp: Int,
    val perfection: Double
)

/**
 * Core IV calculator engine.
 * Given a Pokémon's base stats, its displayed CP, and known IVs,
 * finds all matching levels and their associated data.
 */
object IvCalculator {

    /**
     * CP Multiplier table — maps level → CPM.
     * Values sourced from community research (TheSilphRoad / GamePress).
     * Covers levels 1.0 through 51.0 in 0.5 increments.
     */
    val cpmTable: Map<Double, Double> = mapOf(
        1.0 to 0.094,
        1.5 to 0.135137432,
        2.0 to 0.16639787,
        2.5 to 0.192650919,
        3.0 to 0.21573247,
        3.5 to 0.236572661,
        4.0 to 0.25572005,
        4.5 to 0.273530381,
        5.0 to 0.29024988,
        5.5 to 0.306057377,
        6.0 to 0.3210876,
        6.5 to 0.335445036,
        7.0 to 0.34921268,
        7.5 to 0.362457751,
        8.0 to 0.3752356,
        8.5 to 0.387592411,
        9.0 to 0.39956728,
        9.5 to 0.411193551,
        10.0 to 0.4225,
        10.5 to 0.432926413,
        11.0 to 0.44310755,
        11.5 to 0.45305996,
        12.0 to 0.4627984,
        12.5 to 0.472336083,
        13.0 to 0.48168495,
        13.5 to 0.4908558,
        14.0 to 0.49985844,
        14.5 to 0.508701765,
        15.0 to 0.51739395,
        15.5 to 0.525942511,
        16.0 to 0.5343543,
        16.5 to 0.542635737,
        17.0 to 0.5507927,
        17.5 to 0.5588306,
        18.0 to 0.5667545,
        18.5 to 0.57456915,
        19.0 to 0.5822789,
        19.5 to 0.589887906,
        20.0 to 0.5974,
        20.5 to 0.6048188,
        21.0 to 0.6121573,
        21.5 to 0.619399365,
        22.0 to 0.6265671,
        22.5 to 0.633644533,
        23.0 to 0.64065295,
        23.5 to 0.647576423,
        24.0 to 0.65443563,
        24.5 to 0.6612148,
        25.0 to 0.667934,
        25.5 to 0.674577537,
        26.0 to 0.6811649,
        26.5 to 0.687680637,
        27.0 to 0.69414365,
        27.5 to 0.700538673,
        28.0 to 0.7068842,
        28.5 to 0.713164996,
        29.0 to 0.7193991,
        29.5 to 0.72557155,
        30.0 to 0.7317,
        30.5 to 0.734741009,
        31.0 to 0.73776948,
        31.5 to 0.740785594,
        32.0 to 0.74378943,
        32.5 to 0.746781211,
        33.0 to 0.74976104,
        33.5 to 0.752729087,
        34.0 to 0.7556855,
        34.5 to 0.75863037,
        35.0 to 0.76156384,
        35.5 to 0.764486065,
        36.0 to 0.76739717,
        36.5 to 0.770297266,
        37.0 to 0.7731865,
        37.5 to 0.776064962,
        38.0 to 0.77893275,
        38.5 to 0.781790055,
        39.0 to 0.784637,
        39.5 to 0.787473607,
        40.0 to 0.7903,
        40.5 to 0.792803968,
        41.0 to 0.79530001,
        41.5 to 0.79780392,
        42.0 to 0.800300002,
        42.5 to 0.802803892,
        43.0 to 0.805300005,
        43.5 to 0.807803902,
        44.0 to 0.810299992,
        44.5 to 0.81280387,
        45.0 to 0.81530001,
        45.5 to 0.81780391,
        46.0 to 0.820300021,
        46.5 to 0.822803893,
        47.0 to 0.825299978,
        47.5 to 0.827803898,
        48.0 to 0.830299973,
        48.5 to 0.832803941,
        49.0 to 0.835300057,
        49.5 to 0.83780394,
        50.0 to 0.84030002,
        50.5 to 0.842803923,
        51.0 to 0.845300015,
    )

    /** Levels sorted ascending for iteration. */
    val allLevels: List<Double> = cpmTable.keys.sorted()

    /**
     * Calculate CP at a given level for a Pokémon with known base stats and IVs.
     * Formula: CP = floor((baseAtk + atkIV) × √(baseDef + defIV) × √(baseSta + staIV) × CPM² / 10)
     */
    fun calculateCp(
        baseAtk: Int,
        baseDef: Int,
        baseSta: Int,
        atkIv: Int,
        defIv: Int,
        staIv: Int,
        cpm: Double
    ): Int {
        val atk = (baseAtk + atkIv).toDouble()
        val def = (baseDef + defIv).toDouble()
        val sta = (baseSta + staIv).toDouble()
        return floor(atk * sqrt(def) * sqrt(sta) * cpm * cpm / 10.0).toInt()
    }

    /**
     * Calculate HP at a given level.
     * Formula: HP = floor((baseSta + staIV) × CPM)
     */
    fun calculateHp(
        baseSta: Int,
        staIv: Int,
        cpm: Double
    ): Int {
        val sta = (baseSta + staIv).toDouble()
        return floor(sta * cpm).toInt()
    }

    /**
     * IV perfection percentage: sum of three IVs (0–45) mapped to 0–100%.
     */
    fun calculatePerfection(atkIv: Int, defIv: Int, staIv: Int): Double {
        return (atkIv + defIv + staIv) / 45.0 * 100.0
    }

    /**
     * Finds all levels where a Pokémon with the given base stats and IVs
     * produces the given CP.
     *
     * @param baseAtk Base Attack stat
     * @param baseDef Base Defense stat
     * @param baseSta Base Stamina stat
     * @param atkIv Attack IV (0–15)
     * @param defIv Defense IV (0–15)
     * @param staIv Stamina IV (0–15)
     * @param targetCp The CP displayed in-game
     * @return List of [IvResult] matching the target CP, sorted by level ascending.
     *         Returns empty list if no level matches.
     */
    fun findLevel(
        baseAtk: Int,
        baseDef: Int,
        baseSta: Int,
        atkIv: Int,
        defIv: Int,
        staIv: Int,
        targetCp: Int
    ): List<IvResult> {
        val perfection = calculatePerfection(atkIv, defIv, staIv)
        val results = mutableListOf<IvResult>()

        for (level in allLevels) {
            val cpm = cpmTable[level] ?: continue
            val cp = calculateCp(baseAtk, baseDef, baseSta, atkIv, defIv, staIv, cpm)
            if (cp == targetCp) {
                val hp = calculateHp(baseSta, staIv, cpm)
                results.add(IvResult(level = level, cp = cp, hp = hp, perfection = perfection))
            }
        }

        return results
    }

    // ── CP at specific level (for projection slider) ─────────────────

    /**
     * Calculates CP and HP at a specific level for projection purposes.
     */
    fun calculateAtLevel(
        baseAtk: Int, baseDef: Int, baseSta: Int,
        atkIv: Int, defIv: Int, staIv: Int,
        level: Double
    ): Pair<Int, Int>? {
        val cpm = cpmTable[level] ?: return null
        val cp = calculateCp(baseAtk, baseDef, baseSta, atkIv, defIv, staIv, cpm)
        val hp = calculateHp(baseSta, staIv, cpm)
        return cp to hp
    }

    // ── Power-up cost calculation ────────────────────────────────────

    /** Result of cost calculation between two levels. */
    data class PowerUpCost(
        val dust: Int,
        val candy: Int,
        val xlCandy: Int
    )

    /**
     * Power-up cost per half-level (0.5 increment) based on the *starting* level.
     * Returns (dust, candy, xlCandy).
     * Data sourced from Pokexperto — regular candy stops at level 40,
     * XL candy takes over for levels 40+.
     */
    private fun costPerPowerUp(fromLevel: Double): Triple<Int, Int, Int> {
        val lvl = fromLevel
        return when {
            // ── Regular candy (1.0 – 39.5) ──────────────────────
            lvl < 3.0  -> Triple(200,  1,  0)
            lvl < 5.0  -> Triple(400,  1,  0)
            lvl < 7.0  -> Triple(600,  1,  0)
            lvl < 9.0  -> Triple(800,  1,  0)
            lvl < 11.0 -> Triple(1000, 1,  0)
            lvl < 13.0 -> Triple(1300, 2,  0)
            lvl < 15.0 -> Triple(1600, 2,  0)
            lvl < 17.0 -> Triple(1900, 2,  0)
            lvl < 19.0 -> Triple(2200, 2,  0)
            lvl < 21.0 -> Triple(2500, 2,  0)
            lvl < 23.0 -> Triple(3000, 3,  0)
            lvl < 25.0 -> Triple(3500, 3,  0)
            lvl < 27.0 -> Triple(4000, 3,  0)
            lvl < 29.0 -> Triple(4500, 3,  0)
            lvl < 31.0 -> Triple(5000, 4,  0)
            lvl < 33.0 -> Triple(6000, 6,  0)
            lvl < 35.0 -> Triple(7000, 8,  0)
            lvl < 37.0 -> Triple(8000, 10, 0)
            lvl < 39.0 -> Triple(9000, 12, 0)
            lvl < 40.0 -> Triple(10000, 15, 0)
            // ── XL candy only (40.0 – 50.0) ────────────────────
            lvl < 41.0 -> Triple(10000, 0, 10)
            lvl < 42.0 -> Triple(11000, 0, 10)
            lvl < 43.0 -> Triple(11000, 0, 12)
            lvl < 44.0 -> Triple(12000, 0, 12)
            lvl < 45.0 -> Triple(12000, 0, 15)
            lvl < 46.0 -> Triple(13000, 0, 15)
            lvl < 47.0 -> Triple(13000, 0, 17)
            lvl < 48.0 -> Triple(14000, 0, 17)
            lvl < 49.0 -> Triple(14000, 0, 20)
            lvl < 50.0 -> Triple(15000, 0, 20)
            else       -> Triple(0, 0, 0)
        }
    }

    /**
     * Calculates total dust, candy, and XL candy required to power up
     * from [fromLevel] to [toLevel] (inclusive of each 0.5 step).
     */
    fun getPowerUpCost(fromLevel: Double, toLevel: Double): PowerUpCost {
        if (toLevel <= fromLevel) return PowerUpCost(0, 0, 0)

        var dust = 0
        var candy = 0
        var xlCandy = 0
        var current = fromLevel

        while (current < toLevel) {
            val (d, c, xl) = costPerPowerUp(current)
            dust += d
            candy += c
            xlCandy += xl
            current += 0.5
        }

        return PowerUpCost(dust, candy, xlCandy)
    }
}
