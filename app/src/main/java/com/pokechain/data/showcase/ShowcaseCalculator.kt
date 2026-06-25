package com.pokechain.data.showcase

import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Stateless engine that scores a Pokémon for PokéStop Showcases.
 * Formula matches PokeXperience's calculator (community-verified).
 *
 * Max total: 800 (height) + ~150 (weight) + 50 (IVs) + 178 (XXL bonus) = 1178
 */
object ShowcaseCalculator {

    /**
     * Calculate showcase score.
     *
     * @param size Per-species size data (base height, base weight, max height)
     * @param inputHeight Pokémon's height in meters
     * @param inputWeight Pokémon's weight in kilograms
     * @param atkIv Optional attack IV (0–15, null to skip IV scoring)
     * @param defIv Optional defense IV (0–15)
     * @param hpIv Optional HP IV (0–15)
     */
    fun calculate(
        size: SpeciesSize,
        inputHeight: Double,
        inputWeight: Double,
        atkIv: Int? = null,
        defIv: Int? = null,
        hpIv: Int? = null
    ): ShowcaseResult {
        val ratio = size.maxHeight / size.baseHeight
        val maxWeight = size.baseWeight * (ratio + 0.5)
        val cappedHeight = min(inputHeight, size.maxHeight)
        val cappedWeight = min(inputWeight, maxWeight)

        val heightPts = 800.0 * cappedHeight / size.maxHeight
        val weightPts = 150.0 * cappedWeight / maxWeight

        val haveIvs = atkIv != null && defIv != null && hpIv != null
        val ivPts = if (haveIvs) 50.0 * (atkIv!! + defIv!! + hpIv!!) / 45.0 else 0.0

        val heightRatio = cappedHeight / size.baseHeight
        val isXXL = heightRatio >= 1.5
        val xxlPts = if (isXXL) 178 else 0

        val total = (heightPts + weightPts + ivPts + xxlPts).roundToInt()

        return ShowcaseResult(
            total = total,
            heightPts = heightPts,
            weightPts = weightPts,
            ivPts = ivPts,
            haveIvs = haveIvs,
            xxlPts = xxlPts,
            isXXL = isXXL,
            isXXS = heightRatio < 0.5,
            ratio = heightRatio,
            capped = inputHeight > size.maxHeight || inputWeight > maxWeight,
            maxH = size.maxHeight,
            maxW = maxWeight
        )
    }
}
