package com.pokechain.data.dialgadex

import com.pokechain.data.models.PvERankingEntry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException

/**
 * Tests del flujo de carga del Top por Max: red primero y, si la red falla,
 * respaldo con el snapshot local empaquetado.
 */
class MaxBattleLoaderTest {

    private val liveEntry = PvERankingEntry(
        rat = 500.0, dps = 500.0, tdo = 0.0, id = 815, name = "Cinderace",
        form = "Gigantamax", originalRank = 1
    )

    private fun snapshotWithFire(): MaxBattleSnapshot {
        val file = listOf(
            File("src/main/assets/max_battles_snapshot.json"),
            File("app/src/main/assets/max_battles_snapshot.json")
        ).firstOrNull { it.exists() }
            ?: error("No se encuentra max_battles_snapshot.json")
        return MaxBattleSnapshot.fromJson(file.readText())!!
    }

    @Test
    fun `live data is returned when the network works`() = runBlocking {
        val loader = MaxBattleLoader({ _, _ -> listOf(liveEntry) }, null)

        val result = loader.load("fire", 10)

        assertTrue(result is MaxBattleLoader.Result.Live)
        assertEquals(listOf(liveEntry), (result as MaxBattleLoader.Result.Live).entries)
    }

    @Test
    fun `network failure falls back to the local snapshot`() = runBlocking {
        val loader = MaxBattleLoader({ _, _ -> throw IOException("sin red") }, snapshotWithFire())

        val result = loader.load("fire", 10)

        assertTrue(result is MaxBattleLoader.Result.Cached)
        val cached = result as MaxBattleLoader.Result.Cached
        assertEquals(10, cached.entries.size)
        assertEquals("Cinderace", cached.entries.first().name)
        assertTrue(cached.generatedAt.isNotBlank())
    }

    @Test
    fun `empty network response falls back to the local snapshot`() = runBlocking {
        val loader = MaxBattleLoader({ _, _ -> emptyList() }, snapshotWithFire())

        val result = loader.load("fire", 10)

        assertTrue(result is MaxBattleLoader.Result.Cached)
    }

    @Test
    fun `network failure without snapshot reports a network error`() = runBlocking {
        val loader = MaxBattleLoader({ _, _ -> throw IOException("sin red") }, null)

        val result = loader.load("fire", 10)

        assertTrue(result is MaxBattleLoader.Result.Failed)
        val failed = result as MaxBattleLoader.Result.Failed
        assertTrue(failed.isNetwork)
        assertEquals("sin red", failed.message)
    }

    @Test
    fun `empty response without snapshot reports no data`() = runBlocking {
        val loader = MaxBattleLoader({ _, _ -> emptyList() }, null)

        val result = loader.load("fire", 10)

        assertTrue(result is MaxBattleLoader.Result.Failed)
        assertTrue(!(result as MaxBattleLoader.Result.Failed).isNetwork)
    }

    @Test
    fun `type slug is normalized before fetching`() = runBlocking {
        var requested = ""
        val loader = MaxBattleLoader({ type, _ -> requested = type; emptyList() }, null)

        loader.load("  FIRE  ", 10)

        assertEquals("fire", requested)
    }
}
