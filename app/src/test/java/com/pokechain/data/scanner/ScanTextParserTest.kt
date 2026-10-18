package com.pokechain.data.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests del parser OCR de la ficha del Pokémon con líneas reales de capturas
 * (Sandslash 1,59m/44,77kg XXL; Beldum 95,04kg; Sandygast 0,39m…).
 */
class ScanTextParserTest {

    // ── Lectura completa de una ficha real ─────────────────────────

    @Test
    fun `ficha Sandslash XXL con etiquetas se lee completa`() {
        val lines = listOf(
            "PC1696", "Sandslash", "130 / 130 PS",
            "Exhibiciones", "Hundo",
            "44,77kg", "MÁS PESADO", "TIERRA",
            "XXL", "1,59m", "MÁS GRANDE",
            "1.662.624", "POLVOS ESTELARES", "394", "CARAMELOS SANDSHREW", "49",
            "MÁS PODER", "3.600",
            "GIMNASIOS E INCURSIONES", "COMBATES DE ENTRENADOR",
            "Garra Metal", "6", "Retribución", "NUEVO ATAQUE", "40.000"
        )
        val r = ScanTextParser.parse(lines)
        assertEquals(1696, r.cp)
        assertEquals(130, r.hp)
        assertEquals(44.77, r.weightKg!!, 1e-9)
        assertEquals(1.59, r.heightM!!, 1e-9)
    }

    @Test
    fun `ficha con PS y PC separados por espacio`() {
        val r = ScanTextParser.parse(listOf("PC 523", "Beldum", "75 / 75 PS", "95,04kg", "0,59m"))
        assertEquals(523, r.cp)
        assertEquals(75, r.hp)
        assertEquals(95.04, r.weightKg!!, 1e-9)
        assertEquals(0.59, r.heightM!!, 1e-9)
    }

    // ── Etiquetas de tamaño ────────────────────────────────────────

    @Test
    fun `etiquetas sueltas no producen lecturas falsas`() {
        val r = ScanTextParser.parse(
            listOf("MÁS PESADO", "MÁS GRANDE", "MÁS LIGERO", "XXL", "XXS", "PESO", "ALTURA")
        )
        assertNull(r.cp)
        assertNull(r.hp)
        assertNull(r.weightKg)
        assertNull(r.heightM)
    }

    @Test
    fun `etiqueta fusionada con espacio tras la medida`() {
        val r = ScanTextParser.parse(listOf("44,77kg MÁS PESADO", "1,59m MÁS GRANDE"))
        assertEquals(44.77, r.weightKg!!, 1e-9)
        assertEquals(1.59, r.heightM!!, 1e-9)
    }

    @Test
    fun `etiqueta fusionada sin espacio tras la medida`() {
        val r = ScanTextParser.parse(listOf("44,77kgMÁSPESADO", "1,59mMÁSGRANDE"))
        assertEquals(44.77, r.weightKg!!, 1e-9)
        assertEquals(1.59, r.heightM!!, 1e-9)
    }

    @Test
    fun `insignia XXL pegada a la altura`() {
        val r = ScanTextParser.parse(listOf("1,59mXXL"))
        assertEquals(1.59, r.heightM!!, 1e-9)
    }

    @Test
    fun `etiquetas en ingles`() {
        val r = ScanTextParser.parse(listOf("44.77kg Heavier", "1.59m Larger"))
        assertEquals(44.77, r.weightKg!!, 1e-9)
        assertEquals(1.59, r.heightM!!, 1e-9)
    }

    // ── Fallback normalizado (reparación de dígitos OCR) ───────────

    @Test
    fun `digitos confundidos por el OCR se reparan en el fallback`() {
        // 'O'→0 (captura de Beldum/Sandygast)
        val r = ScanTextParser.parse(listOf("95,O4kg", "O,39m"))
        assertEquals(95.04, r.weightKg!!, 1e-9)
        assertEquals(0.39, r.heightM!!, 1e-9)
    }

    @Test
    fun `PS corrupto a P5 por la normalizacion sigue leyendose`() {
        val r = ScanTextParser.parse(listOf("75 / 75 P5"))
        assertEquals(75, r.hp)
    }

    // ── Sin falsos positivos numéricos ─────────────────────────────

    @Test
    fun `polvos caramelos y precios no son peso ni altura`() {
        val r = ScanTextParser.parse(
            listOf("1.662.624", "3.600", "40.000", "49", "394", "Retribución")
        )
        assertNull(r.weightKg)
        assertNull(r.heightM)
        assertNull(r.cp)
        assertNull(r.hp)
    }

    @Test
    fun `peso de tres digitos y altura con punto`() {
        val r = ScanTextParser.parse(listOf("123,45kg", "2.10m"))
        assertEquals(123.45, r.weightKg!!, 1e-9)
        assertEquals(2.10, r.heightM!!, 1e-9)
    }

    // ── stripSizeLabels directamente ───────────────────────────────

    @Test
    fun `strip quita etiquetas con y sin acentos`() {
        assertEquals("44,77kg ", ScanTextParser.stripSizeLabels("44,77kg MÁS PESADO"))
        assertEquals("1,59m", ScanTextParser.stripSizeLabels("1,59mMÁSGRANDE"))
        assertEquals("TIERRA", ScanTextParser.stripSizeLabels("TIERRA"))
    }

    // ── Candidatas a nombre de especie ─────────────────────────────

    @Test
    fun `rótulos números y unidades no son candidatos a nombre`() {
        listOf("ALTURA", "PESO", "MÁS GRANDE", "MÁS PESADO", "PC1696", "83 / 83 PS",
               "44,77kg", "1,59m", "394", "3.600", "40.000", "…", "✓", "⇄ 1/2")
            .forEach { assertFalse("$it debería descartarse", ScanTextParser.isNameCandidate(it)) }
    }

    @Test
    fun `nombres reales y renombrados son candidatos`() {
        listOf("Ralts", "Gible", "Munna", "Sandslash", "Ralts93", "Sandslash71", "TIERRA", "Exhibiciones")
            .forEach { assertTrue("$it debería ser candidato", ScanTextParser.isNameCandidate(it)) }
    }

    // ── pickSpecies: prioridad por posición ────────────────────────

    @Test
    fun `ALTURA no roba el match a Ralts`() {
        val map = mapOf("ralts" to 280, "altaria" to 334)
        val lines = listOf(
            ScanTextParser.OcrLine("PC1204", 200f),
            ScanTextParser.OcrLine("Ralts", 800f),
            ScanTextParser.OcrLine("ALTURA", 1500f),
        )
        assertEquals("Ralts" to 280, ScanTextParser.pickSpecies(lines, map))
    }

    @Test
    fun `el nombre de la especie gana al SANDSHREW del rótulo de caramelos`() {
        val map = mapOf("sandslash" to 28, "sandshrew" to 27)
        val lines = listOf(
            ScanTextParser.OcrLine("SANDSHREW", 1400f),
            ScanTextParser.OcrLine("Sandslash", 800f),
        )
        assertEquals("Sandslash" to 28, ScanTextParser.pickSpecies(lines, map))
    }

    @Test
    fun `nombre renombrado con superíndice casa por fuzzy`() {
        val map = mapOf("ralts" to 280)
        val lines = listOf(ScanTextParser.OcrLine("Ralts93", 800f))
        assertEquals("Ralts93" to 280, ScanTextParser.pickSpecies(lines, map))
    }

    @Test
    fun `sin candidatas devuelve null`() {
        val map = mapOf("ralts" to 280)
        val lines = listOf(ScanTextParser.OcrLine("394", 300f), ScanTextParser.OcrLine("ALTURA", 800f))
        assertNull(ScanTextParser.pickSpecies(lines, map))
    }
}
