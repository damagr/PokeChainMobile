package com.pokechain.data.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Toggle ⇄ de formas del chip (ScannerState.cycleForm). */
class ScanModelsTest {

    @Test
    fun `cicla entre las formas y vuelve a la primera`() {
        ScannerState.setForms(listOf("", "alolan", "hisuian"))
        assertEquals("alolan", ScannerState.cycleForm())
        assertEquals("hisuian", ScannerState.cycleForm())
        assertEquals("", ScannerState.cycleForm())
        assertEquals(0, ScannerState.formIndex.value) // 3 formas: tras 3 ciclos vuelve al inicio
    }

    @Test
    fun `sin formas o con una sola devuelve null`() {
        ScannerState.setForms(emptyList())
        assertNull(ScannerState.cycleForm())
        ScannerState.setForms(listOf(""))
        assertNull(ScannerState.cycleForm())
    }

    @Test
    fun `setForms reinicia el indice`() {
        ScannerState.setForms(listOf("", "alolan"))
        ScannerState.cycleForm() // -> "alolan" (índice 1)
        ScannerState.setForms(listOf("", "galarian"))
        assertEquals(0, ScannerState.formIndex.value)
        assertEquals("galarian", ScannerState.cycleForm()) // desde 0 avanza a 1
    }

    @Test
    fun `reset limpia las formas`() {
        ScannerState.setForms(listOf("", "alolan"))
        ScannerState.reset()
        assertEquals(emptyList<String>(), ScannerState.formKeys.value)
        assertNull(ScannerState.cycleForm())
    }
}
