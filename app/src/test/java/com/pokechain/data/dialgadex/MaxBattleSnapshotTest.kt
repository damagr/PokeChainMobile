package com.pokechain.data.dialgadex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Tests del snapshot local de Combates Max (app/src/main/assets).
 *
 * El asset se genera con tools/generate_max_snapshot.py a partir de Dittobase;
 * estos tests comprueban que el archivo empaquetado es válido y utilizable.
 */
class MaxBattleSnapshotTest {

    private val assetFile: File by lazy {
        val candidates = listOf(
            File("src/main/assets/max_battles_snapshot.json"),
            File("app/src/main/assets/max_battles_snapshot.json")
        )
        candidates.firstOrNull { it.exists() }
            ?: error("No se encuentra max_battles_snapshot.json (cwd=${System.getProperty("user.dir")})")
    }

    private fun loadSnapshot(): MaxBattleSnapshot {
        val snapshot = MaxBattleSnapshot.fromJson(assetFile.readText())
        assertNotNull("El snapshot empaquetado debe poder parsearse", snapshot)
        return snapshot!!
    }

    @Test
    fun `asset contains the 18 valid types`() {
        val snapshot = loadSnapshot()
        assertEquals(MaxBattleScrapingEngine.VALID_TYPES.toSet(), snapshot.availableTypes)
    }

    @Test
    fun `every entry has coherent data`() {
        val snapshot = loadSnapshot()
        for (type in MaxBattleScrapingEngine.VALID_TYPES) {
            val entries = snapshot.entriesFor(type)
            assertTrue("El tipo $type no debe estar vacío", entries.isNotEmpty())
            entries.forEach { entry ->
                assertTrue("$type: rank >= 1", entry.originalRank >= 1)
                assertTrue("$type: dex > 0", entry.id > 0)
                assertTrue("$type: rat > 0", entry.rat > 0)
                assertTrue("$type: nombre no vacío", entry.name.isNotBlank())
                assertTrue(
                    "$type: la sprite debe apuntar a un Pokémon",
                    entry.spriteUrl?.contains("/pokemon/") == true
                )
                assertTrue(
                    "$type: el Max Move no debe estar vacío",
                    !entry.cm.isNullOrBlank()
                )
            }
        }
    }

    @Test
    fun `ranks are consecutive from one`() {
        val snapshot = loadSnapshot()
        for (type in MaxBattleScrapingEngine.VALID_TYPES) {
            val ranks = snapshot.entriesFor(type).map { it.originalRank }
            assertEquals("$type: ranks 1..N", (1..entriesCount(snapshot, type)).toList(), ranks)
        }
    }

    @Test
    fun `entriesFor returns the expected fire leader`() {
        val snapshot = loadSnapshot()
        val top = snapshot.entriesFor("fire").first()
        assertEquals(1, top.originalRank)
        assertEquals(815, top.id) // Cinderace
        assertEquals("Cinderace", top.name)
        assertEquals("Gigantamax", top.form)
        assertEquals("G-Max Fireball", top.cm)
        assertEquals(580.0, top.rat, 0.001)
    }

    @Test
    fun `entriesFor is case insensitive and safe for unknown types`() {
        val snapshot = loadSnapshot()
        assertTrue(snapshot.entriesFor("FIRE").isNotEmpty())
        assertTrue(snapshot.entriesFor("no-existe").isEmpty())
    }

    @Test
    fun `generatedAt is present and formattable`() {
        val snapshot = loadSnapshot()
        assertTrue(snapshot.generatedAt.isNotBlank())
        assertTrue(snapshot.generatedAtLabel.contains("-"))
    }

    @Test
    fun `corrupt json returns null instead of crashing`() {
        assertNull(MaxBattleSnapshot.fromJson("{ esto no es json"))
        assertNull(MaxBattleSnapshot.fromJson(""))
    }

    private fun entriesCount(snapshot: MaxBattleSnapshot, type: String): Int =
        snapshot.entriesFor(type).size
}
