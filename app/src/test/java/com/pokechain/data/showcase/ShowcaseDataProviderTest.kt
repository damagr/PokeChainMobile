package com.pokechain.data.showcase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

/**
 * Selección de forma por medidas con el showcase_data.json real del repo.
 *
 * Caso que rompió la lectura en pantalla (17/10/2026): Sandslash normal XXL
 * (1,59m / 44,77kg) se asignaba a la forma Alolan (base 1,2m/55kg más
 * "cercana" que la base normal 1,0m/29,5kg) → chip 710/1178 en vez de
 * 1056/1178.
 */
class ShowcaseDataProviderTest {

    private val provider: ShowcaseDataProvider by lazy {
        val file = listOf(
            File("src/main/assets/showcase_data.json"),
            File("app/src/main/assets/showcase_data.json")
        ).firstOrNull { it.exists() } ?: error("showcase_data.json no encontrado")
        ShowcaseDataProvider(file.readText())
    }

    // ── Forma base frente a variantes ──────────────────────────────

    @Test
    fun `Sandslash XXL de la forma base usa la forma base`() {
        val e = provider.getSizeByMeasurements(28, 1.59, 44.77)
        assertNotNull(e)
        assertEquals("", e!!.formKey)
        // Puntuación verificada contra la ficha: XXL + hundo
        // 800*1,59/1,75 + 150*44,77/66,375 + 50 + 178 = 1056
        val calc = ShowcaseCalculator.calculate(e.size, 1.59, 44.77, 15, 15, 15)
        assertEquals(1056, calc.total)
        assertEquals(true, calc.isXXL)
    }

    @Test
    fun `Sandslash XL normal sin IVs tampoco toma la variante`() {
        val e = provider.getSizeByMeasurements(28, 1.55, 50.0)
        assertNotNull(e)
        assertEquals("", e!!.formKey)
    }

    @Test
    fun `medida que excede el maximo de la base elige la variante`() {
        // 2,0m supera el maxHeight 1,75 de la forma base → solo Alolan lo abarca
        val e = provider.getSizeByMeasurements(28, 2.0, 70.0)
        assertNotNull(e)
        assertEquals("alolan", e!!.formKey)
    }

    @Test
    fun `individuo medio de la forma base usa la forma base`() {
        val e = provider.getSizeByMeasurements(28, 1.0, 29.5)
        assertNotNull(e)
        assertEquals("", e!!.formKey)
    }

    // ── Clases de tamaño sin forma base (Pumpkaboo) ────────────────

    @Test
    fun `Pumpkaboo mediano elige la clase average`() {
        val e = provider.getSizeByMeasurements(710, 0.4, 5.0)
        assertNotNull(e)
        assertEquals("average", e!!.formKey)
    }

    @Test
    fun `Pumpkaboo grande elige la clase large`() {
        val e = provider.getSizeByMeasurements(710, 0.55, 8.0)
        assertNotNull(e)
        assertEquals("large", e!!.formKey)
    }

    // ── Especies de una sola forma ─────────────────────────────────

    @Test
    fun `especie de una sola forma devuelve esa forma`() {
        // Sandygast: XL extremo (52,73kg vs base 1,5kg) pero sin variantes
        val e = provider.getSizeByMeasurements(769, 0.39, 52.73)
        assertNotNull(e)
        assertEquals("Sandygast", e!!.name)
    }

    // ── getForms (toggle ⇄ del chip) ───────────────────────────────

    @Test
    fun `getForms devuelve la base primero`() {
        val forms = provider.getForms(28)
        assertEquals(listOf("", "alolan"), forms)
    }

    @Test
    fun `getForms vacio para dex sin datos`() {
        assertEquals(emptyList<String>(), provider.getForms(99999))
    }
}
