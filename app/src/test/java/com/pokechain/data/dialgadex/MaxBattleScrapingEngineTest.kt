package com.pokechain.data.dialgadex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests para el parser HTML de MaxBattleScrapingEngine.
 *
 * El HTML replica la estructura de tabla de
 * https://db.pokemongohub.net/pokemon-list/best-dynamax-per-type/{type}:
 *   # | Name (+badge/imagen) | Fast Attack | Charged Attack (Max Move) | Max Move Damage | Max Phases
 *
 * La celda de nombre contiene un <a> con la imagen sprite (src thumb/{dex}_{form}.webp)
 * y el nombre visible ("Gigantamax Cinderace", "Dynamax Charizard", "Zacian Crowned Sword"...).
 */
class MaxBattleScrapingEngineTest {

    private val engine = MaxBattleScrapingEngine()

    private fun rowHtml(
        rank: Int,
        spriteFile: String,
        visibleName: String,
        fastMove: String,
        maxMove: String,
        damage: String,
        phases: Int
    ): String = """
        <tr>
          <td>$rank.</td>
          <td><a href="https://db.pokemongohub.net/pokemon/x"><img src="https://db.pokemongohub.net/images/official/thumb/$spriteFile" alt="$visibleName Pokémon GO">$visibleName<img src="https://db.pokemongohub.net/_next/image?url=%2Fimages%2Ficons%2Fgigantamax.png" alt="Gigantamax Badge"></a></td>
          <td><a href="https://db.pokemongohub.net/move/y">$fastMove</a></td>
          <td><a href="https://db.pokemongohub.net/move/z">$maxMove</a></td>
          <td>$damage</td>
          <td>$phases</td>
        </tr>
    """.trimIndent()

    private val tableHtml = """
        <html><body>
        <table>
          <thead><tr><th>#</th><th>Name</th><th>Fast Attack</th><th>Charged Attack</th><th>Max Move Damage</th><th>Max Phases</th></tr></thead>
          <tbody>
            ${rowHtml(1, "815_gmax.webp", "Gigantamax Cinderace", "Fire Spin", "G-Max Fireball", "373.47", 14)}
            ${rowHtml(2, "006_dynamax.webp", "Dynamax Charizard", "Air Slash", "Max Flare", "273.52", 19)}
            ${rowHtml(3, "888_crowned_sword.webp", "Zacian Crowned Sword", "Quick Attack", "Behemoth Blade", "250.00", 20)}
          </tbody>
        </table>
        </body></html>
    """.trimIndent()

    // ── Parsing de filas ────────────────────────────────────────────

    @Test
    fun `parses gmax row with name, form, dex and moves`() {
        val entries = engine.parseHtml(tableHtml)
        assertEquals(3, entries.size)

        val gmax = entries[0]
        assertEquals("Cinderace", gmax.name)
        assertEquals("Gigantamax", gmax.form)
        assertEquals(815, gmax.id)
        assertEquals("Fire Spin", gmax.fm)
        assertEquals("G-Max Fireball", gmax.cm)
        assertEquals(373.47, gmax.rat, 0.001)
        assertEquals(1, gmax.originalRank)
        assertEquals(false, gmax.shadow)
    }

    @Test
    fun `parses dynamax row via sprite src hint`() {
        val entries = engine.parseHtml(tableHtml)

        val dmax = entries[1]
        assertEquals("Charizard", dmax.name)
        assertEquals("Dynamax", dmax.form)
        assertEquals(6, dmax.id) // "006" -> 6
        assertEquals("Air Slash", dmax.fm)
        assertEquals("Max Flare", dmax.cm)
        assertEquals(273.52, dmax.rat, 0.001)
        assertEquals(2, dmax.originalRank)
    }

    @Test
    fun `parses crowned sword row via visible text`() {
        val entries = engine.parseHtml(tableHtml)

        val crowned = entries[2]
        assertEquals("Zacian", crowned.name)
        assertEquals("Crowned Sword", crowned.form)
        assertEquals(888, crowned.id) // sufijo con guion bajo también extrae dex
        assertEquals("Behemoth Blade", crowned.cm)
    }

    // ── Casos límite ───────────────────────────────────────────────

    @Test
    fun `malformed rows are skipped`() {
        val html = """
            <html><body><table><tbody>
              <tr><td>nope</td><td>incomplete</td></tr>
              ${rowHtml(1, "815_gmax.webp", "Gigantamax Cinderace", "Fire Spin", "G-Max Fireball", "373.47", 14)}
            </tbody></table></body></html>
        """.trimIndent()

        val entries = engine.parseHtml(html)
        assertEquals(1, entries.size)
        assertEquals("Cinderace", entries[0].name)
    }

    @Test
    fun `rows without damage value are skipped`() {
        val html = """
            <html><body><table><tbody>
              <tr>
                <td>1.</td>
                <td><a><img src="https://db.pokemongohub.net/images/official/thumb/006_dynamax.webp">Dynamax Charizard</a></td>
                <td><a>Air Slash</a></td>
                <td><a>Max Flare</a></td>
                <td>not-a-number</td>
                <td>19</td>
              </tr>
            </tbody></table></body></html>
        """.trimIndent()

        assertTrue(engine.parseHtml(html).isEmpty())
    }

    @Test
    fun `empty html returns empty list`() {
        assertTrue(engine.parseHtml("").isEmpty())
        assertTrue(engine.parseHtml("<html><body></body></html>").isEmpty())
    }

    @Test
    fun `type cache keys use lowercase type names`() {
        // VALID_TYPES son las URLs de pokemongohub (lowercase) — deben casar con PokemonType.nameEn.lowercase()
        val expected = com.pokechain.data.models.PokemonType.entries.map { it.nameEn.lowercase() }.toSet()
        assertEquals(expected, MaxBattleScrapingEngine.VALID_TYPES.toSet())
    }
}