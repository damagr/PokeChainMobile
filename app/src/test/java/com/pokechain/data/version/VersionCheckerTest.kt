package com.pokechain.data.version

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests para la selección de la última release en VersionChecker.
 *
 * Regresión del bug histórico: el código antiguo usaba
 * `maxByOrNull { compareVersions(tagName, "0") }` como key de maxByOrNull —
 * un comparador por pares contra la constante "0" que devuelve +1 para
 * cualquier versión con major >= 1. Resultado: se elegía la PRIMERA release
 * de la lista (que GitHub ordena por fecha de creación), no la de mayor
 * versión. Esto rompía la detección cuando una release heredaba un
 * created_at antiguo (ej. v1.9.0 publicada desde un draft del 08/09).
 */
class VersionCheckerTest {

    private val checker = VersionChecker()

    private fun release(tag: String, prerelease: Boolean = false) = GitHubRelease(
        tagName = tag,
        name = tag,
        htmlUrl = "https://github.com/damagr/PokeChainMobile/releases/tag/$tag",
        body = null,
        prerelease = prerelease,
    )

    // ── versionKey ──────────────────────────────────────────────────

    @Test
    fun `versionKey parses major, minor and patch`() {
        assertEquals(1_009_000L, versionKey("v1.9.0"))
        assertEquals(1_008_014L, versionKey("v1.8.14"))
        assertEquals(2_000_000L, versionKey("v2.0.0"))
    }

    @Test
    fun `versionKey comparison picks higher version`() {
        // 1.9.0 > 1.8.14 aunque el patch de 1.8.14 sea mayor
        assertTrue(versionKey("v1.9.0") > versionKey("v1.8.14"))
        assertTrue(versionKey("v2.0.0") > versionKey("v1.9.9"))
    }

    @Test
    fun `versionKey tolerates suffixes and malformed parts`() {
        assertEquals(1_009_000L, versionKey("1.9.0-beta"))
        assertEquals(1_000_000L, versionKey("v1.0"))
    }

    // ── compareVersions (pairwise — uso correcto, sin cambios) ─────

    @Test
    fun `compareVersions pairwise 1_9_0 is greater than 1_8_14`() {
        assertTrue(checker.compareVersions("1.9.0", "1.8.14") > 0)
        assertTrue(checker.compareVersions("1.8.14", "1.9.0") < 0)
        assertEquals(0, checker.compareVersions("1.9.0", "1.9.0"))
    }

    // ── Regresión: selectLatest ignora el orden de la lista ────────

    @Test
    fun `selectLatest picks highest version regardless of list order`() {
        // Orden real observado en GitHub: v1.9.0 heredó created_at del 08/09
        val releases = listOf(
            release("v1.8.14"),
            release("v1.8.13"),
            release("v1.8.12"),
            release("v1.9.0"),
            release("v1.8.11"),
        )
        assertEquals("v1.9.0", selectLatest(releases)?.tagName)
    }

    @Test
    fun `selectLatest with v1_9_1 first still picks it`() {
        val releases = listOf(
            release("v1.9.1"),
            release("v1.9.0"),
            release("v1.8.14"),
        )
        assertEquals("v1.9.1", selectLatest(releases)?.tagName)
    }

    @Test
    fun `selectLatest skips prereleases`() {
        val releases = listOf(
            release("v1.9.1", prerelease = true),
            release("v1.9.0"),
        )
        assertEquals("v1.9.0", selectLatest(releases)?.tagName)
    }

    @Test
    fun `selectLatest returns null on empty list`() {
        assertEquals(null, selectLatest(emptyList()))
    }

    // ── Documentación del bug antiguo (guard) ──────────────────────

    @Test
    fun `old buggy key selector picks first in list instead of highest`() {
        // El código antiguo: maxByOrNull { if (compareVersions(tag, "0") > 0) 1 else 0 }
        // Todas las releases con major >= 1 tienen clave +1 → gana la primera.
        val releases = listOf(release("v1.8.14"), release("v1.9.0"))
        val buggyPick = releases.maxByOrNull {
            if (checker.compareVersions(it.tagName.removePrefix("v"), "0") > 0) 1 else 0
        }
        assertEquals("v1.8.14", buggyPick?.tagName) // el bug elegía 1.8.14, no 1.9.0
    }
}