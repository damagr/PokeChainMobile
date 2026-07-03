package com.pokechain.data.pvpoke

import com.pokechain.data.models.BaseStats
import com.pokechain.data.models.Family
import com.pokechain.data.models.Pokemon
import com.pokechain.data.models.PvPFilterParams
import com.pokechain.data.models.PvPLeague
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PvPDataProcessorTest {

    private fun poke(
        speciesId: String,
        dex: Int,
        parent: String? = null,
        eliteMoves: List<String>? = null,
        defaultIVs: Map<String, List<Double>>? = null,
    ) = Pokemon(
        speciesId = speciesId,
        speciesName = speciesId,
        dex = dex,
        types = emptyList(),
        baseStats = BaseStats(0, 0, 0),
        family = if (parent != null) Family(parent = parent) else null,
        eliteMoves = eliteMoves,
        defaultIVs = defaultIVs,
    )

    private fun raw(speciesId: String, score: Double = 0.0) = PvPRawEntry(
        speciesId = speciesId,
        speciesName = speciesId,
        score = score,
        moveset = emptyList(),
    )

    // ── processRankings: dedup by base species ─────────────────────

    @Test
    fun `shadow and normal of same species are deduped to one`() {
        val gm = GameMasterResponse(
            pokemon = listOf(
                poke("snorlax", 143),
                poke("snorlax_shadow", 143),
            )
        )
        val processor = PvPDataProcessor(gm)
        val rawList = listOf(raw("snorlax"), raw("snorlax_shadow"))

        // includeShadow = false → snorlax_shadow filtered out, snorlax stays
        val results = processor.processRankings(
            rawList,
            PvPFilterParams(league = PvPLeague.GREAT, includeShadow = false, count = 10)
        )
        assertEquals(1, results.size)
        assertEquals("snorlax", results[0].speciesId)
    }

    @Test
    fun `includeShadow true keeps only shadow entries`() {
        val gm = GameMasterResponse(
            pokemon = listOf(poke("snorlax", 143), poke("snorlax_shadow", 143))
        )
        val processor = PvPDataProcessor(gm)
        val results = processor.processRankings(
            listOf(raw("snorlax"), raw("snorlax_shadow")),
            PvPFilterParams(league = PvPLeague.GREAT, includeShadow = true, count = 10)
        )
        assertEquals(1, results.size)
        assertTrue(results[0].isShadow)
    }

    @Test
    fun `count limits how many raw entries are processed`() {
        val gm = GameMasterResponse(pokemon = listOf(poke("a", 1), poke("b", 2), poke("c", 3)))
        val processor = PvPDataProcessor(gm)
        val rawList = listOf(raw("a"), raw("b"), raw("c"))
        val results = processor.processRankings(
            rawList,
            PvPFilterParams(league = PvPLeague.GREAT, count = 2)
        )
        assertEquals(2, results.size)
    }

    @Test
    fun `originalRank is index plus one`() {
        val gm = GameMasterResponse(pokemon = listOf(poke("a", 1), poke("b", 2)))
        val processor = PvPDataProcessor(gm)
        val results = processor.processRankings(
            listOf(raw("a"), raw("b")),
            PvPFilterParams(league = PvPLeague.GREAT, count = 10)
        )
        assertEquals(1, results[0].originalRank)
        assertEquals(2, results[1].originalRank)
    }

    // ── traceBaseForm: parent chain walk ───────────────────────────

    @Test
    fun `traceBaseForm walks parent chain to root`() {
        // raichu → pikachu → pichu
        val gm = GameMasterResponse(
            pokemon = listOf(
                poke("raichu", 26, parent = "pikachu"),
                poke("pikachu", 25, parent = "pichu"),
                poke("pichu", 172),
            )
        )
        val processor = PvPDataProcessor(gm)
        val result = PvPResult("raichu", "Raichu", 0.0, emptyList(), false, false, emptySet(), 26)
        assertEquals("pichu", processor.traceBaseForm(result))
    }

    @Test
    fun `traceBaseForm strips shadow and xl suffixes`() {
        val gm = GameMasterResponse(
            pokemon = listOf(
                poke("venusaur", 3, parent = "ivysaur"),
                poke("ivysaur", 2, parent = "bulbasaur"),
                poke("bulbasaur", 1),
            )
        )
        val processor = PvPDataProcessor(gm)
        val result = PvPResult("venusaur_shadow", "Venusaur", 0.0, emptyList(), true, false, emptySet(), 3)
        assertEquals("bulbasaur", processor.traceBaseForm(result))
    }

    @Test
    fun `traceBaseForm is cycle safe`() {
        // a → b → a (cycle): must terminate, not hang
        val gm = GameMasterResponse(
            pokemon = listOf(
                poke("a", 1, parent = "b"),
                poke("b", 2, parent = "a"),
            )
        )
        val processor = PvPDataProcessor(gm)
        val result = PvPResult("a", "A", 0.0, emptyList(), false, false, emptySet(), 1)
        val base = processor.traceBaseForm(result)
        assertTrue(base == "a" || base == "b")
    }

    // ── traceBaseFormForDex: shortest speciesId seed ───────────────

    @Test
    fun `traceBaseFormForDex picks shortest speciesId as seed`() {
        // palkia (shorter) vs palkia_origin (longer) — both dex 484
        val gm = GameMasterResponse(
            pokemon = listOf(
                poke("palkia", 484),
                poke("palkia_origin", 484),
            )
        )
        val processor = PvPDataProcessor(gm)
        assertEquals("palkia", processor.traceBaseFormForDex(484))
    }

    @Test
    fun `traceBaseFormForDex returns null for unknown dex`() {
        val gm = GameMasterResponse(pokemon = listOf(poke("a", 1)))
        val processor = PvPDataProcessor(gm)
        assertNull(processor.traceBaseFormForDex(999))
    }

    @Test
    fun `traceBaseDexForDex returns dex of traced base`() {
        // charizard → charmeleon → charmander
        val gm = GameMasterResponse(
            pokemon = listOf(
                poke("charizard", 6, parent = "charmeleon"),
                poke("charmeleon", 5, parent = "charmander"),
                poke("charmander", 4),
            )
        )
        val processor = PvPDataProcessor(gm)
        assertEquals(4, processor.traceBaseDexForDex(6))
    }

    // ── needsXLCandy via processRankings ───────────────────────────

    @Test
    fun `needsXL true when defaultIVs cp above 40`() {
        val gm = GameMasterResponse(
            pokemon = listOf(
                poke("medicham", 308, defaultIVs = mapOf("cp1500" to listOf(45.0, 1.0, 1.0))),
            )
        )
        val processor = PvPDataProcessor(gm)
        val results = processor.processRankings(
            listOf(raw("medicham")),
            PvPFilterParams(league = PvPLeague.GREAT, xlCandy = true, count = 10)
        )
        assertTrue(results[0].needsXL)
    }

    @Test
    fun `xlCandy false filters out XL entries`() {
        val gm = GameMasterResponse(
            pokemon = listOf(
                poke("medicham", 308, defaultIVs = mapOf("cp1500" to listOf(45.0, 1.0, 1.0))),
            )
        )
        val processor = PvPDataProcessor(gm)
        val results = processor.processRankings(
            listOf(raw("medicham")),
            PvPFilterParams(league = PvPLeague.GREAT, xlCandy = false, count = 10)
        )
        assertTrue(results.isEmpty())
    }

    @Test
    fun `needsXL false when defaultIVs cp at or below 40`() {
        val gm = GameMasterResponse(
            pokemon = listOf(
                poke("azumarill", 184, defaultIVs = mapOf("cp1500" to listOf(40.0, 1.0, 1.0))),
            )
        )
        val processor = PvPDataProcessor(gm)
        val results = processor.processRankings(
            listOf(raw("azumarill")),
            PvPFilterParams(league = PvPLeague.GREAT, xlCandy = true, count = 10)
        )
        assertFalse(results[0].needsXL)
    }

    // ── Elite moves filter ─────────────────────────────────────────

    @Test
    fun `includeElite false filters out entries with elite moves`() {
        val gm = GameMasterResponse(
            pokemon = listOf(
                poke("a", 1, eliteMoves = listOf("FOOTPRINT_MOVE")),
            )
        )
        val processor = PvPDataProcessor(gm)
        val rawWithElite = listOf(
            PvPRawEntry("a", "A", 0.0, listOf("FOOTPRINT_MOVE")),
        )
        val results = processor.processRankings(
            rawWithElite,
            PvPFilterParams(league = PvPLeague.GREAT, includeElite = false, count = 10)
        )
        assertTrue(results.isEmpty())
    }

    @Test
    fun `includeElite true keeps entries with elite moves`() {
        val gm = GameMasterResponse(
            pokemon = listOf(poke("a", 1, eliteMoves = listOf("FOOTPRINT_MOVE")))
        )
        val processor = PvPDataProcessor(gm)
        val rawWithElite = listOf(
            PvPRawEntry("a", "A", 0.0, listOf("FOOTPRINT_MOVE")),
        )
        val results = processor.processRankings(
            rawWithElite,
            PvPFilterParams(league = PvPLeague.GREAT, includeElite = true, count = 10)
        )
        assertEquals(1, results.size)
        assertTrue(results[0].eliteMoves.contains("FOOTPRINT_MOVE"))
    }

    // ── cleanName strips parentheticals ────────────────────────────

    @Test
    fun `cleanName strips trailing parenthetical`() {
        val gm = GameMasterResponse(pokemon = listOf(poke("a", 1)))
        val processor = PvPDataProcessor(gm)
        // "Mewtwo (Shadow)" → "Mewtwo"
        val results = processor.processRankings(
            listOf(PvPRawEntry("a", "Mewtwo (Shadow)", 0.0, emptyList())),
            PvPFilterParams(league = PvPLeague.GREAT, count = 10)
        )
        assertEquals("Mewtwo", results[0].speciesName)
    }
}
