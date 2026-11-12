package com.pokechain.data.dialgadex

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Tests de la descarga real del motor (ruta asíncrona con OkHttp).
 *
 * Comprueban lo que en el móvil fallaba: que la llamada termine SIEMPRE
 * (descarga, error HTTP o timeout) y nunca deje la pantalla cargando.
 */
class MaxBattleScrapingEngineNetworkTest {

    private lateinit var server: MockWebServer

    private val rowHtml = """
        <li role="row" aria-rowindex="2">
          <div role="cell" style="--col-w:4px"><div class="spacer"></div></div>
          <div role="cell" class="ftc"> <p>1</p></div>
          <div role="cell" class="ftc">
            <a href="/pokemon-go/pokedex/cinderace-gigantamax">
              <div aria-label="Gigantamax"><svg><use href="/images/pokemon-go/max-form-icons.svg#gigantamax"></use></svg></div>
              <div><img loading="lazy" alt="Gigantamax Cinderace" src="https://assets.dittobase.com/go/optimized/v1/pokemon/815-cinderace-gigantamax-128.webp"/></div>
              <p>G-Max Cinderace</p><p>Gigantamax Cinderace</p>
            </a>
          </div>
          <div role="cell" class="ftc">
            <img src="/images/go/attacker-cards/type-fire-128.webp" alt="fire"/>
            <p>G-Max Fireball</p>
          </div>
          <div role="cell" class="ftc"> <p>238</p></div>
          <div role="cell" class="ftc"> <p>580</p></div>
          <div role="cell" class="ftc"> <p>129.3</p></div>
          <div role="cell" class="ftc"> <p>100.0%</p></div>
        </li>
    """.trimIndent()

    private val pageHtml = """
        <html><body><ul role="rowgroup">$rowHtml</ul></body></html>
    """.trimIndent()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        try {
            server.shutdown()
        } catch (e: IOException) {
            // el servidor puede seguir con respuestas retrasadas: no es crítico
        }
    }

    private fun engine(timeoutMs: Long = 5_000L) = MaxBattleScrapingEngine(
        baseUrl = server.url("/pokemon-go/best-attackers/max-battles").toString(),
        timeoutMs = timeoutMs
    )

    @Test
    fun `downloads and parses the ranking`() = runBlocking {
        server.enqueue(MockResponse().setBody(pageHtml))

        val entries = engine().computeMaxByType("fire", 10)

        assertEquals(1, entries.size)
        assertEquals("Cinderace", entries[0].name)
        assertEquals(815, entries[0].id)
        assertEquals("G-Max Fireball", entries[0].cm)
        assertEquals(580.0, entries[0].rat, 0.001)

        val request = server.takeRequest()
        assertEquals("/pokemon-go/best-attackers/max-battles/fire", request.path)
    }

    @Test
    fun `slow response fails with timeout instead of hanging forever`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setBody(pageHtml)
                .setBodyDelay(2, TimeUnit.SECONDS)
        )

        val started = System.currentTimeMillis()
        try {
            engine(timeoutMs = 500L).computeMaxByType("fire", 10)
            fail("Debe fallar por timeout")
        } catch (e: IOException) {
            val elapsed = System.currentTimeMillis() - started
            assertTrue("Debe fallar rápido (${elapsed}ms)", elapsed < 5_000)
            assertTrue(e.message!!.contains("Timeout"))
        }
    }

    @Test
    fun `http error is reported as IOException`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404).setBody("<html>nope</html>"))

        try {
            engine().computeMaxByType("fire", 10)
            fail("Debe fallar con error HTTP")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("404"))
        }
    }

    @Test
    fun `empty body is reported as IOException`() = runBlocking {
        server.enqueue(MockResponse().setBody(""))

        try {
            engine().computeMaxByType("fire", 10)
            fail("Debe fallar con respuesta vacía")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("vacía"))
        }
    }

    @Test
    fun `successful response is cached per type`() = runBlocking {
        server.enqueue(MockResponse().setBody(pageHtml))

        val engine = engine()
        val first = engine.computeMaxByType("fire", 10)
        val second = engine.computeMaxByType("fire", 10)

        assertEquals(first, second)
        assertEquals(1, server.requestCount) // la segunda vez usa la cache
    }

    @Test
    fun `probe reports the http status`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))

        assertEquals("HTTP 200", engine().probe())
    }

    @Test
    fun `probe reports a failing status`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503))

        assertEquals("HTTP 503", engine().probe())
    }
}
