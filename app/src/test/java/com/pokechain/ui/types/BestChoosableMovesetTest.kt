package com.pokechain.ui.types

import com.pokechain.data.models.PvERankingEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests para bestChoosableMoveset: el mejor moveset elegible (ataque cargado
 * SIN "+", el supermega predefinido no es elegible por el jugador) a partir
 * de resultados con movimientos subóptimos activados.
 */
class BestChoosableMovesetTest {

    private fun entry(id: Int, name: String, rank: Int, cm: String?, form: String = "Normal") = PvERankingEntry(
        rat = 0.0, dps = 0.0, tdo = 0.0,
        id = id, name = name, form = form,
        cm = cm, originalRank = rank
    )

    @Test
    fun `picks best line without plus marker`() {
        val sub = listOf(
            entry(448, "Mega Lucario", rank = 3, cm = "hiperrayo+", form = "Mega"),
            entry(448, "Mega Lucario", rank = 15, cm = "hidrobomba", form = "Mega"),
            entry(448, "Mega Lucario", rank = 22, cm = "auraesfera", form = "Mega"),
        )
        val choosable = bestChoosableMoveset(sub, 448)
        assertEquals(15, choosable?.originalRank)
        assertEquals("hidrobomba", choosable?.cm)
    }

    @Test
    fun `skips double plus markers`() {
        // "++"/"+++" también son supermega (endsWith("+") = true)
        val sub = listOf(
            entry(448, "Mega Lucario", rank = 1, cm = "hiperrayo++", form = "Mega"),
            entry(448, "Mega Lucario", rank = 9, cm = "hidrobomba", form = "Mega"),
        )
        assertEquals(9, bestChoosableMoveset(sub, 448)?.originalRank)
    }

    @Test
    fun `returns null when only plus attacks available`() {
        val sub = listOf(entry(448, "Mega Lucario", rank = 1, cm = "hiperrayo+", form = "Mega"))
        assertNull(bestChoosableMoveset(sub, 448))
    }

    @Test
    fun `ignores other pokemon entries`() {
        val sub = listOf(
            entry(448, "Mega Lucario", rank = 1, cm = "hiperrayo+", form = "Mega"),
            entry(150, "Mewtwo", rank = 2, cm = "psicoataque"),
        )
        assertNull(bestChoosableMoveset(sub, 448))
    }

    @Test
    fun `null cm lines are filtered out`() {
        val sub = listOf(
            entry(448, "Mega Lucario", rank = 1, cm = "hiperrayo+", form = "Mega"),
            entry(448, "Mega Lucario", rank = 5, cm = null, form = "Mega"),
        )
        assertNull(bestChoosableMoveset(sub, 448))
    }
}