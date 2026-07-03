package com.pokechain.ui.types

import com.pokechain.data.models.AppLanguage
import com.pokechain.data.models.BaseStats
import com.pokechain.data.models.Family
import com.pokechain.data.models.Pokemon
import com.pokechain.data.models.PvPLeague
import com.pokechain.data.pvpoke.GameMasterResponse
import com.pokechain.data.pvpoke.PvPRawEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression test for the Palkia form-label bug:
 * the old fetchLeagueRank returned a single rank per dex without
 * indicating which form it belonged to. buildFormRanks must return
 * one entry per form found in the top 100.
 */
class BuildFormRanksTest {

    private val palkiaDex = 484
    private val gamemaster = GameMasterResponse(
        pokemon = listOf(
            pokemon("palkia", palkiaDex),
            pokemon("palkia_origin", palkiaDex),
        )
    )

    private fun pokemon(speciesId: String, dex: Int) = Pokemon(
        speciesId = speciesId,
        speciesName = speciesId.replace("_", " ").replaceFirstChar { it.uppercase() },
        dex = dex,
        types = listOf("Water", "Dragon"),
        baseStats = BaseStats(0, 0, 0),
    )

    private fun raw(speciesId: String) = PvPRawEntry(
        speciesId = speciesId,
        speciesName = speciesId,
        score = 0.0,
        moveset = emptyList(),
    )

    // ── Palkia: the original bug ────────────────────────────────────

    @Test
    fun `palkia master multi form returns origin, normal and origin shadow`() {
        // Ranking order: index 0 = filler, 1 = palkia_origin, 15 = palkia_origin_shadow, 26 = palkia
        val raw = (0..200).map { i ->
            when (i) {
                1 -> raw("palkia_origin")
                15 -> raw("palkia_origin_shadow")
                26 -> raw("palkia")
                else -> raw("zubat_${i}") // dex mismatch filler
            }
        }

        val ranks = buildFormRanks(raw, palkiaDex, gamemaster, AppLanguage.ES)

        assertEquals(3, ranks.size)

        val byLabel = ranks.associateBy { it.formLabel }
        assertEquals(2, byLabel["Origen"]?.rank)
        assertEquals(27, byLabel["Normal"]?.rank)
        assertEquals(16, byLabel["Origen Oscuro"]?.rank)

        // Shadow flag correctness
        assertTrue(byLabel["Origen Oscuro"]?.isShadow == true)
        assertTrue(byLabel["Origen"]?.isShadow == false)
        assertTrue(byLabel["Normal"]?.isShadow == false)
    }

    @Test
    fun `palkia master english labels`() {
        val raw = listOf(raw("palkia_origin"), raw("palkia"), raw("palkia_origin_shadow"))
        val ranks = buildFormRanks(raw, palkiaDex, gamemaster, AppLanguage.EN)
        val byLabel = ranks.associateBy { it.formLabel }

        assertEquals(1, byLabel["Origin"]?.rank)
        assertEquals(2, byLabel["Normal"]?.rank)
        assertEquals(3, byLabel["Origin Shadow"]?.rank)
    }

    // ── Giratina: two distinct forms ────────────────────────────────

    @Test
    fun `giratina two forms get distinct labels`() {
        val gm = GameMasterResponse(
            pokemon = listOf(
                pokemon("giratina_altered", 487),
                pokemon("giratina_origin", 487),
            )
        )
        val raw = listOf(raw("giratina_origin"), raw("giratina_altered"))
        val ranks = buildFormRanks(raw, 487, gm, AppLanguage.ES)

        assertEquals(2, ranks.size)
        val byLabel = ranks.associateBy { it.formLabel }
        assertEquals(1, byLabel["Origen"]?.rank)
        assertEquals(2, byLabel["Modificada"]?.rank)
    }

    // ── Top-100 filter ──────────────────────────────────────────────

    @Test
    fun `forms above rank 100 are filtered out`() {
        // 200 filler entries, palkia at index 150 → rank 151 > 100
        val raw = (0..200).map { i ->
            if (i == 150) raw("palkia") else raw("filler_$i")
        }
        val ranks = buildFormRanks(raw, palkiaDex, gamemaster, AppLanguage.ES)
        assertTrue(ranks.isEmpty())
    }

    // ── No match ───────────────────────────────────────────────────

    @Test
    fun `dex not present in raw returns empty`() {
        val raw = listOf(raw("zubat"), raw("rattata"))
        val ranks = buildFormRanks(raw, palkiaDex, gamemaster, AppLanguage.ES)
        assertTrue(ranks.isEmpty())
    }

    // ── Shadow of normal form ───────────────────────────────────────

    @Test
    fun `shadow without alternate form gets plain shadow label`() {
        // palkia_shadow only, no _origin — should label as just "Oscuro"
        val gm = GameMasterResponse(pokemon = listOf(pokemon("palkia", palkiaDex)))
        val raw = listOf(raw("palkia_shadow"))
        val ranks = buildFormRanks(raw, palkiaDex, gm, AppLanguage.ES)

        assertEquals(1, ranks.size)
        assertEquals("Oscuro", ranks[0].formLabel)
        assertTrue(ranks[0].isShadow)
    }

    @Test
    fun `shadow without alternate form english gets plain Shadow label`() {
        val gm = GameMasterResponse(pokemon = listOf(pokemon("palkia", palkiaDex)))
        val raw = listOf(raw("palkia_shadow"))
        val ranks = buildFormRanks(raw, palkiaDex, gm, AppLanguage.EN)

        assertEquals(1, ranks.size)
        assertEquals("Shadow", ranks[0].formLabel)
    }

    // ── League parameter is not used by buildFormRanks (pure on raw) ─

    @Test
    fun `buildFormRanks ignores league, works on any raw list`() {
        val raw = listOf(raw("palkia_origin"), raw("palkia"))
        val ranksGreat = buildFormRanks(raw, palkiaDex, gamemaster, AppLanguage.ES)
        val ranksMaster = buildFormRanks(raw, palkiaDex, gamemaster, AppLanguage.ES)
        assertEquals(ranksGreat, ranksMaster)
    }
}
