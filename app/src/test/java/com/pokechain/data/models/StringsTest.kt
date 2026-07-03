package com.pokechain.data.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exhaustive check: every league × every language returns a
 * non-blank, non-null label. Guards against a forgotten `else`
 * when a new league is added to the enum.
 */
class StringsTest {

    @Test
    fun `leagueName returns non-blank for every league in EN`() {
        for (league in PvPLeague.entries) {
            val name = Strings.leagueName(league, AppLanguage.EN)
            assertNotNull("league $league returned null in EN", name)
            assertTrue("league $league blank in EN", name.isNotBlank())
        }
    }

    @Test
    fun `leagueName returns non-blank for every league in ES`() {
        for (league in PvPLeague.entries) {
            val name = Strings.leagueName(league, AppLanguage.ES)
            assertNotNull("league $league returned null in ES", name)
            assertTrue("league $league blank in ES", name.isNotBlank())
        }
    }

    @Test
    fun `leagueName covers every league — no branch missing`() {
        // If a league is added to the enum but not to leagueName,
        // Kotlin's `when` will throw at runtime (exhaustive when).
        // This test just exercises all branches; a missing branch
        // throws NoWhenBranchMatchedException → test fails.
        for (league in PvPLeague.entries) {
            Strings.leagueName(league, AppLanguage.EN)
            Strings.leagueName(league, AppLanguage.ES)
        }
    }

    @Test
    fun `master league is Master in both languages`() {
        // "Master" is the same in EN and ES
        assertEquals("Master", Strings.leagueName(PvPLeague.MASTER, AppLanguage.EN))
        assertEquals("Master", Strings.leagueName(PvPLeague.MASTER, AppLanguage.ES))
    }

    @Test
    fun `great league is Great in EN and Super in ES`() {
        assertEquals("Great", Strings.leagueName(PvPLeague.GREAT, AppLanguage.EN))
        assertEquals("Super", Strings.leagueName(PvPLeague.GREAT, AppLanguage.ES))
    }
}
