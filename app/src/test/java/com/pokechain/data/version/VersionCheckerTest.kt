package com.pokechain.data.version

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionCheckerTest {

    private val checker = VersionChecker()

    // ── Basic comparisons ──────────────────────────────────────────

    @Test
    fun `equal versions return 0`() {
        assertEquals(0, checker.compareVersions("1.7.1", "1.7.1"))
    }

    @Test
    fun `higher patch version is greater`() {
        assertTrue(checker.compareVersions("1.7.2", "1.7.1") > 0)
    }

    @Test
    fun `lower minor version is less`() {
        assertTrue(checker.compareVersions("1.6.9", "1.7.0") < 0)
    }

    @Test
    fun `higher major version is greater`() {
        assertTrue(checker.compareVersions("2.0.0", "1.99.99") > 0)
    }

    // ── Length normalization ───────────────────────────────────────

    @Test
    fun `shorter version padded with zeros`() {
        // 1.7 vs 1.7.0 → equal after padding
        assertEquals(0, checker.compareVersions("1.7", "1.7.0"))
    }

    @Test
    fun `1_7_1 vs 1_8_0 returns negative`() {
        assertTrue(checker.compareVersions("1.7.1", "1.8.0") < 0)
    }

    @Test
    fun `1_8_0 vs 1_7_1 returns positive`() {
        assertTrue(checker.compareVersions("1.8.0", "1.7.1") > 0)
    }

    // ── Pre-release suffix is stripped (always treats as release) ──
    // The current impl splits on "-" and takes the first part,
    // so "1.7.1-rc1" is compared as "1.7.1".

    @Test
    fun `prerelease suffix stripped to release`() {
        assertEquals(0, checker.compareVersions("1.7.1-rc1", "1.7.1"))
    }

    @Test
    fun `prerelease vs lower release`() {
        // "1.8.0-beta" → 1.8.0 vs 1.7.1 → greater
        assertTrue(checker.compareVersions("1.8.0-beta", "1.7.1") > 0)
    }

    // ── Non-numeric parts default to 0 ─────────────────────────────

    @Test
    fun `non-numeric part treated as 0`() {
        // "1.x.0" → [1, 0, 0] vs "1.0.0" → [1, 0, 0]
        assertEquals(0, checker.compareVersions("1.x.0", "1.0.0"))
    }

    @Test
    fun `non-numeric vs numeric higher`() {
        // "1.x.0" → [1, 0, 0] vs "1.2.0" → [1, 2, 0] → less
        assertTrue(checker.compareVersions("1.x.0", "1.2.0") < 0)
    }

    // ── v-prefix handled by caller, not compareVersions ────────────
    // compareVersions itself does NOT strip "v" — the caller does.
    // "v1.7.1".split(".")[0] = "v1" → toIntOrNull() = 0

    @Test
    fun `v-prefix without stripping breaks numeric comparison`() {
        // Documenting current behaviour: caller must strip "v".
        // "v1.7.1" → ["v1", "7", "1"] → [0, 7, 1]
        // "1.7.1"  → ["1", "7", "1"]  → [1, 7, 1]
        // So compareVersions("v1.7.1", "1.7.1") < 0 (left's first part = 0)
        assertTrue(checker.compareVersions("v1.7.1", "1.7.1") < 0)
    }
}
