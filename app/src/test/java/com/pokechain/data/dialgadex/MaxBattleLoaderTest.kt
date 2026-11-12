package com.pokechain.data.dialgadex

import com.pokechain.data.models.PvERankingEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException

/**
 * Tests del flujo de carga del Top por Max: datos locales primero y
 * actualización en segundo plano, con deadline propio.
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

        val result = loader.refresh("fire", 10)

        assertTrue(result is MaxBattleLoader.Result.Live)
        assertEquals(listOf(liveEntry), (result as MaxBattleLoader.Result.Live).entries)
    }

    @Test
    fun `local entries are available without touching the network`() = runBlocking {
        var called = false
        val loader = MaxBattleLoader({ _, _ -> called = true; emptyList() }, snapshotWithFire())

        val local = loader.localEntries("fire", 10)

        assertEquals(10, local.size)
        assertEquals("Cinderace", local.first().name)
        assertTrue("No debe tocar la red", !called)
    }

    @Test
    fun `local entries are empty without snapshot`() {
        val loader = MaxBattleLoader({ _, _ -> emptyList() }, null)

        assertTrue(loader.localEntries("fire", 10).isEmpty())
        assertTrue(!loader.hasSnapshot)
    }

    @Test
    fun `network failure is reported without data`() = runBlocking {
        val loader = MaxBattleLoader({ _, _ -> throw IOException("sin red") }, null)

        val result = loader.refresh("fire", 10)

        assertTrue(result is MaxBattleLoader.Result.Failed)
        val failed = result as MaxBattleLoader.Result.Failed
        assertTrue(failed.isNetwork)
        assertEquals("sin red", failed.message)
    }

    @Test
    fun `empty response is reported as no data`() = runBlocking {
        val loader = MaxBattleLoader({ _, _ -> emptyList() }, null)

        val result = loader.refresh("fire", 10)

        assertTrue(result is MaxBattleLoader.Result.Failed)
        assertTrue(!(result as MaxBattleLoader.Result.Failed).isNetwork)
    }

    @Test
    fun `a hanging network is cut by the loader deadline`() = runBlocking {
        // Fuente que se queda colgada: el deadline del loader la corta
        val loader = MaxBattleLoader({ _, _ ->
            delay(60_000)
            emptyList()
        }, null, timeoutMs = 500L)

        val started = System.currentTimeMillis()
        val result = loader.refresh("fire", 10)
        val elapsed = System.currentTimeMillis() - started

        assertTrue(result is MaxBattleLoader.Result.Failed)
        assertTrue("Debe cortar rápido (${elapsed}ms)", elapsed < 5_000)
    }

    @Test
    fun `type slug is normalized before fetching`() = runBlocking {
        var requested = ""
        val loader = MaxBattleLoader({ type, _ -> requested = type; emptyList() }, null)

        loader.refresh("  FIRE  ", 10)

        assertEquals("fire", requested)
    }
}
