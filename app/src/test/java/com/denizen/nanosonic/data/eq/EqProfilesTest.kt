package com.denizen.nanosonic.data.eq

import com.denizen.nanosonic.core.model.EqProfile
import com.denizen.nanosonic.core.model.ParametricEq
import org.junit.Assert.assertEquals
import org.junit.Test

class EqProfilesTest {
    private fun profile(id: String, name: String = id, source: String = "src", custom: Boolean = false, ts: Long = 0, preamp: Double = 0.0) =
        EqProfile(id, name, name, source, "rig", ParametricEq(preamp, emptyList()), custom, ts)

    @Test
    fun `re-importing an AutoEQ profile replaces it and keeps its id`() {
        val existing = EqProfiles(listOf(profile("hd600_0", "HD 600", ts = 5)), activeId = "hd600_0")
        val result = existing.upsertAutoEq(listOf(profile("hd600_3", "HD 600", ts = 9, preamp = -2.0)))
        val saved = result.profiles.single()
        assertEquals("hd600_0", saved.id)
        assertEquals(5, saved.addedTimestamp)
        assertEquals(-2.0, saved.eq.preampDb, 0.0)
        assertEquals("hd600_0", result.active?.id)
    }

    @Test
    fun `new profiles get a unique id`() {
        val existing = EqProfiles(listOf(profile("x_0", "Other")), null)
        val result = existing.upsertAutoEq(listOf(profile("x_0", "New")))
        assertEquals(listOf("x_0", "x_0_2"), result.profiles.map { it.id })
    }

    @Test
    fun `custom profiles are never replaced by imports`() {
        val existing = EqProfiles(listOf(profile("c", "HD 600", custom = true)), null)
        assertEquals(2, existing.upsertAutoEq(listOf(profile("a", "HD 600"))).profiles.size)
    }

    @Test
    fun `sorted puts AutoEQ first then custom, newest first`() {
        val profiles = EqProfiles(
            listOf(profile("c1", custom = true, ts = 1), profile("a1", ts = 1), profile("c2", custom = true, ts = 2), profile("a2", ts = 2)),
            null,
        )
        assertEquals(listOf("a2", "a1", "c2", "c1"), profiles.sorted.map { it.id })
    }
}
