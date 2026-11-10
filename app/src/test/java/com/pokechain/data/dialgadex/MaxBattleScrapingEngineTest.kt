package com.pokechain.data.dialgadex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests para el parser HTML de MaxBattleScrapingEngine (fuente Dittobase).
 *
 * El HTML replica la estructura real de
 * https://www.dittobase.com/pokemon-go/best-attackers/max-battles/{type}:
 * tabla FlexTable de Chakra UI con filas <li role="row">.
 *
 * La web HA CAMBIADO de estructura (oct/2026): añadió un celda espaciadora
 * al inicio (8 celdas) y las sprites pasaron a /go/optimized/v1/pokemon/
 * ({dex}-{slug}-128.webp). El parser se ancla en la celda del sprite, así
 * que debe parsear AMBAS estructuras:
 *   - 7 celdas: # | Pokémon | Max Move | Attack | Max damage | MMW | % of best
 *   - 8 celdas: [spacer] | # | Pokémon | Max Move | Attack | Max damage | MMW | % of best
 */
class MaxBattleScrapingEngineTest {

    private val engine = MaxBattleScrapingEngine()

    // Fragmentos HTML reales extraídos de la página de Dittobase (fire)
    private val gmaxRow = """
        <li class="group chakra-list__item css-1ir99ro" role="row" aria-rowindex="2">
          <div role="cell" class="css-5at65a"><div class="css-11x5ang"><p class="css-10ifi89">1</p></div></div>
          <div role="cell" class="css-ze8euo"><a class="chakra-link css-197xo1u" href="/pokemon-go/pokedex/cinderace-gigantamax"><div class="css-435avb"><div class="css-15nqf75"><div aria-label="Gigantamax" class="css-1jf4mr1"><svg viewBox="0 0 50 50" fill="currentColor" xmlns="http://www.w3.org/2000/svg" width="100%" height="100%"><use href="/images/pokemon-go/max-form-icons.svg#gigantamax"></use></svg></div><div class="css-p5ldym"><img class="chakra-image css-ffbzxd" loading="lazy" alt="Gigantamax Cinderace" src="https://assets.dittobase.com/go/pokemon/815-cinderace-gigantamax.png"/></div></div><p class="css-1p9xdme">G-Max Cinderace</p><p class="css-1ty953g">Gigantamax Cinderace</p></div></a></div>
          <div role="cell" class="css-10eqzgn"><div class="css-ywexnr"><img class="chakra-image css-4xw2h0" src="https://assets.dittobase.com/go/types/fire.png" alt="fire"/><p class="css-wtsr4h">G-Max Fireball</p></div></div>
          <div role="cell" class="css-4flxuw"><p class="css-qfv7e4">238</p></div>
          <div role="cell" class="css-1ek4n97"><p class="css-xalvlj">582</p></div>
          <div role="cell" class="css-1ah40ol"><p class="css-qfv7e4">128.9</p></div>
          <div role="cell" class="css-4i5svj"><p class="css-qfv7e4">100.0<!-- -->%</p></div>
        </li>
    """.trimIndent()

    private val dynamaxRow = """
        <li class="group chakra-list__item css-1ir99ro" role="row" aria-rowindex="5">
          <div role="cell" class="css-5at65a"><div class="css-11x5ang"><p class="css-10ifi89">4</p></div></div>
          <div role="cell" class="css-ze8euo"><a class="chakra-link css-197xo1u" href="/pokemon-go/pokedex/moltres-dynamax"><div class="css-435avb"><div class="css-15nqf75"><div aria-label="Dynamax" class="css-1jf4mr1"><svg viewBox="0 0 50 50" fill="currentColor" xmlns="http://www.w3.org/2000/svg" width="100%" height="100%"><use href="/images/pokemon-go/max-form-icons.svg#dynamax"></use></svg></div><div class="css-p5ldym"><img class="chakra-image css-ffbzxd" loading="lazy" alt="Dynamax Moltres" src="https://assets.dittobase.com/go/pokemon/146-moltres.png"/></div></div><p class="css-1p9xdme">Moltres</p><p class="css-1ty953g">Moltres</p></div></a></div>
          <div role="cell" class="css-10eqzgn"><div class="css-ywexnr"><img class="chakra-image css-4xw2h0" src="https://assets.dittobase.com/go/types/fire.png" alt="fire"/><p class="css-wtsr4h">Fire</p></div></div>
          <div role="cell" class="css-4flxuw"><p class="css-qfv7e4">251</p></div>
          <div role="cell" class="css-1ek4n97"><p class="css-xalvlj">476</p></div>
          <div role="cell" class="css-1ah40ol"><p class="css-qfv7e4">157.6</p></div>
          <div role="cell" class="css-4i5svj"><p class="css-qfv7e4">81.8<!-- -->%</p></div>
        </li>
    """.trimIndent()

    private val tableHtml = """
        <html><body>
        <ul role="rowgroup" class="chakra-list__root css-eam4a5">
          <div role="row" aria-rowindex="1"><div role="columnheader">#</div><div role="columnheader">Pokémon</div><div role="columnheader">Max Move</div><div role="columnheader">Attack</div><div role="columnheader">Max damage</div><div role="columnheader">MMW</div><div role="columnheader">% of best</div></div>
          $gmaxRow
          $dynamaxRow
        </ul>
        </body></html>
    """.trimIndent()

    // ── Estructura ACTUAL (oct/2026): 8 celdas con espaciador y sprites .webp ──

    private val newLayoutGmaxRow = """
        <li role="row" aria-rowindex="2">
          <div role="cell" style="--col-w:4px"><div class="spacer"></div></div>
          <div role="cell" style="--col-w:1.75rem" class="ftc"> <p>1</p></div>
          <div role="cell" style="--col-w:11rem;flex:1 0 11rem" class="ftc">
            <a href="/pokemon-go/pokedex/cinderace-gigantamax">
              <div aria-label="Gigantamax"><svg viewBox="0 0 50 50"><use href="/images/pokemon-go/max-form-icons.svg#gigantamax"></use></svg></div>
              <div><img loading="lazy" alt="Gigantamax Cinderace" src="https://assets.dittobase.com/go/optimized/v1/pokemon/815-cinderace-gigantamax-128.webp"/></div>
              <p>G-Max Cinderace</p><p>Gigantamax Cinderace</p>
            </a>
          </div>
          <div role="cell" style="--col-w:11rem" class="ftc">
            <img src="/images/go/attacker-cards/type-fire-128.webp" alt="fire"/>
            <p>G-Max Fireball</p>
          </div>
          <div role="cell" class="ftc ftc--align_end"> <p>238</p></div>
          <div role="cell" class="ftc ftc--align_end"> <p>580</p></div>
          <div role="cell" class="ftc ftc--align_end"> <p>129.3</p></div>
          <div role="cell" class="ftc ftc--align_end"> <p>100.0%</p></div>
        </li>
    """.trimIndent()

    private val newLayoutDynamaxRow = """
        <li role="row" aria-rowindex="3">
          <div role="cell" style="--col-w:4px"><div class="spacer"></div></div>
          <div role="cell" style="--col-w:1.75rem" class="ftc"> <p>3</p></div>
          <div role="cell" style="--col-w:11rem;flex:1 0 11rem" class="ftc">
            <a href="/pokemon-go/pokedex/darmanitan-standard-dynamax">
              <div aria-label="Dynamax"><svg viewBox="0 0 50 50"><use href="/images/pokemon-go/max-form-icons.svg#dynamax"></use></svg></div>
              <div><img loading="lazy" alt="Dynamax Darmanitan Standard" src="https://assets.dittobase.com/go/optimized/v1/pokemon/555-darmanitan-standard-128.webp"/></div>
              <p>Darmanitan Standard</p><p>Darmanitan Standard</p>
            </a>
          </div>
          <div role="cell" style="--col-w:11rem" class="ftc">
            <img src="/images/go/attacker-cards/type-fire-128.webp" alt="fire"/>
            <p>Fire</p>
          </div>
          <div role="cell" class="ftc ftc--align_end"> <p>263</p></div>
          <div role="cell" class="ftc ftc--align_end"> <p>496</p></div>
          <div role="cell" class="ftc ftc--align_end"> <p>151.2</p></div>
          <div role="cell" class="ftc ftc--align_end"> <p>86.1%</p></div>
        </li>
    """.trimIndent()

    private val newLayoutHtml = """
        <html><body>
        <ul role="rowgroup">
          $newLayoutGmaxRow
          $newLayoutDynamaxRow
        </ul>
        </body></html>
    """.trimIndent()

    // ── Parsing de filas ────────────────────────────────────────────

    @Test
    fun `parses gmax row with name, form, dex, sprite and moves`() {
        val entries = engine.parseHtml(tableHtml)
        assertEquals(2, entries.size) // el header (div role=row) se ignora

        val gmax = entries[0]
        assertEquals(1, gmax.originalRank)
        assertEquals("Cinderace", gmax.name) // alt sin prefijo de forma
        assertEquals("Gigantamax", gmax.form)
        assertEquals(815, gmax.id) // dex desde el src
        assertEquals("https://assets.dittobase.com/go/pokemon/815-cinderace-gigantamax.png", gmax.spriteUrl)
        assertEquals("G-Max Fireball", gmax.cm) // signature move tal cual
        assertEquals(582.0, gmax.rat, 0.001)
        assertEquals(false, gmax.shadow)
    }

    @Test
    fun `parses current dittobase layout with spacer cell and webp sprites`() {
        val entries = engine.parseHtml(newLayoutHtml)
        assertEquals(2, entries.size) // antes devolvía 0: cells[0] era el espaciador

        val gmax = entries[0]
        assertEquals(1, gmax.originalRank)
        assertEquals("Cinderace", gmax.name)
        assertEquals("Gigantamax", gmax.form)
        assertEquals(815, gmax.id) // dex desde /go/optimized/v1/pokemon/815-...webp
        assertEquals("https://assets.dittobase.com/go/optimized/v1/pokemon/815-cinderace-gigantamax-128.webp", gmax.spriteUrl)
        assertEquals("G-Max Fireball", gmax.cm)
        assertEquals(580.0, gmax.rat, 0.001)

        val dmax = entries[1]
        assertEquals(3, dmax.originalRank)
        assertEquals("Darmanitan Standard", dmax.name)
        assertEquals("Dynamax", dmax.form)
        assertEquals(555, dmax.id)
        assertEquals("Max Flare", dmax.cm) // derivado del tipo "fire"
        assertEquals(496.0, dmax.rat, 0.001)
    }

    @Test
    fun `derives max move name from type for dynamax rows`() {
        val entries = engine.parseHtml(tableHtml)

        val dmax = entries[1]
        assertEquals(4, dmax.originalRank)
        assertEquals("Moltres", dmax.name)
        assertEquals("Dynamax", dmax.form)
        assertEquals(146, dmax.id)
        assertEquals("https://assets.dittobase.com/go/pokemon/146-moltres.png", dmax.spriteUrl)
        assertEquals("Max Flare", dmax.cm) // derivado del tipo "fire"
        assertEquals(476.0, dmax.rat, 0.001)
    }

    // ── Mapeo tipo -> Max Move ─────────────────────────────────────

    @Test
    fun `max move by type covers all 18 types`() {
        assertEquals(18, MaxBattleScrapingEngine.MAX_MOVE_BY_TYPE.size)
        assertEquals("Max Strike", MaxBattleScrapingEngine.MAX_MOVE_BY_TYPE["normal"])
        assertEquals("Max Mindstorm", MaxBattleScrapingEngine.MAX_MOVE_BY_TYPE["psychic"])
        assertEquals("Max Starfall", MaxBattleScrapingEngine.MAX_MOVE_BY_TYPE["fairy"])
    }

    // ── Casos límite ───────────────────────────────────────────────

    @Test
    fun `malformed rows are skipped`() {
        val html = """
            <html><body><ul role="rowgroup">
              <li role="row"><div role="cell">1</div><div role="cell">incomplete</div></li>
              $gmaxRow
            </ul></body></html>
        """.trimIndent()

        val entries = engine.parseHtml(html)
        assertEquals(1, entries.size)
        assertEquals("Cinderace", entries[0].name)
    }

    @Test
    fun `rows without damage value are skipped`() {
        val html = """
            <html><body><ul role="rowgroup">
              <li role="row">
                <div role="cell"><p>2</p></div>
                <div role="cell"><img alt="Dynamax Moltres" src="https://assets.dittobase.com/go/pokemon/146-moltres.png"/></div>
                <div role="cell"><p>Fire</p></div>
                <div role="cell"><p>251</p></div>
                <div role="cell"><p>not-a-number</p></div>
                <div role="cell"><p>157.6</p></div>
                <div role="cell"><p>81.8%</p></div>
              </li>
            </ul></body></html>
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
        // VALID_TYPES son los slugs de Dittobase (lowercase)
        val expected = setOf(
            "normal", "fire", "water", "electric", "grass", "ice",
            "fighting", "poison", "ground", "flying", "psychic", "bug",
            "rock", "ghost", "dragon", "dark", "steel", "fairy"
        )
        assertEquals(expected, MaxBattleScrapingEngine.VALID_TYPES.toSet())
    }
}