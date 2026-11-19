package com.denizen.nanosonic.data.eq

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.denizen.nanosonic.core.model.EqProfile
import com.denizen.nanosonic.core.model.ParametricEq
import com.denizen.nanosonic.di.AppScope
import com.denizen.nanosonic.di.EqProfilePrefs
import com.denizen.nanosonic.di.IoDispatcher
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** A saved profile list plus the id of the active one, loaded from disk. */
data class EqProfiles(val profiles: List<EqProfile>, val activeId: String?) {
    val active: EqProfile? get() = profiles.firstOrNull { it.id == activeId }

    /** AutoEQ profiles first, then custom ones; newest first within each group. */
    val sorted: List<EqProfile>
        get() = profiles.sortedWith(compareBy<EqProfile> { it.isCustom }.thenByDescending { it.addedTimestamp })

    /**
     * Adds profiles imported from the AutoEQ database. A profile matching an existing AutoEQ profile
     * (same name, source and rig) replaces it and keeps its id and date, so re-importing never duplicates.
     */
    fun upsertAutoEq(incoming: List<EqProfile>): EqProfiles {
        val result = profiles.toMutableList()
        for (profile in incoming) {
            val index = result.indexOfFirst {
                !it.isCustom && it.name == profile.name && it.source == profile.source && it.rig == profile.rig
            }
            result += if (index >= 0) {
                val existing = result.removeAt(index)
                profile.copy(id = existing.id, addedTimestamp = existing.addedTimestamp)
            } else {
                profile.copy(id = uniqueId(profile.id, result))
            }
        }
        return copy(profiles = result)
    }

    private fun uniqueId(candidate: String, existing: List<EqProfile>): String {
        val taken = existing.mapTo(HashSet()) { it.id }
        if (candidate !in taken) return candidate
        return generateSequence(2) { it + 1 }.map { "${candidate}_$it" }.first { it !in taken }
    }
}

@Singleton
class EqProfileRepository @Inject constructor(
    @EqProfilePrefs private val prefs: SharedPreferences,
    @AppScope scope: CoroutineScope,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private val writeLock = Mutex()
    private val loaded = CompletableDeferred<Unit>()
    private val _state = MutableStateFlow<EqProfiles?>(null)

    /** Null until the stored profiles have been read. */
    val state: StateFlow<EqProfiles?> = _state.asStateFlow()

    /** The active profile, or null when EQ is disabled. Emits only after loading. */
    val activeProfile: Flow<EqProfile?> =
        _state.filterNotNull().map { it.active }.distinctUntilChanged()

    init {
        scope.launch {
            _state.value = withContext(io) { load() }
            loaded.complete(Unit)
        }
    }

    /**
     * Saves profiles imported from the AutoEQ database. A profile that matches an existing AutoEQ
     * profile (same name, source and rig) replaces it and keeps its id, so re-importing never duplicates.
     */
    suspend fun upsertAutoEq(newProfiles: List<EqProfile>) = mutate { it.upsertAutoEq(newProfiles) }

    suspend fun importCustom(name: String, eq: ParametricEq): EqProfile {
        val now = System.currentTimeMillis()
        val profile = EqProfile(
            id = LegacyProfileIds.customProfileId(name, now),
            name = name,
            deviceModel = name,
            source = CUSTOM_SOURCE,
            rig = CUSTOM_RIG,
            eq = eq,
            isCustom = true,
            addedTimestamp = now,
        )
        mutate { it.copy(profiles = it.profiles.filterNot { p -> p.id == profile.id } + profile) }
        return profile
    }

    suspend fun delete(id: String) = mutate { current ->
        EqProfiles(
            profiles = current.profiles.filterNot { it.id == id },
            activeId = current.activeId.takeUnless { it == id },
        )
    }

    /** Activates the profile with [id], or disables EQ when null. */
    suspend fun setActive(id: String?) = mutate { it.copy(activeId = id) }

    private suspend fun mutate(transform: (EqProfiles) -> EqProfiles) {
        loaded.await()
        writeLock.withLock {
            val before = _state.value ?: EqProfiles(emptyList(), null)
            val after = transform(before)
            withContext(io) { save(before, after) }
            _state.value = after
        }
    }

    private fun load(): EqProfiles = try {
        val profiles = prefs.getString(KEY_PROFILES, null)?.let(EqProfileCodec::decode).orEmpty()
        EqProfiles(profiles, prefs.getString(KEY_ACTIVE_PROFILE_ID, null))
    } catch (e: Exception) {
        // Keep the raw data on disk untouched; we just start with an empty list this session.
        Log.e(TAG, "Failed to read saved EQ profiles", e)
        EqProfiles(emptyList(), null)
    }

    private fun save(before: EqProfiles, after: EqProfiles) {
        prefs.edit {
            if (after.profiles != before.profiles) putString(KEY_PROFILES, EqProfileCodec.encode(after.profiles))
            if (after.activeId != before.activeId) {
                if (after.activeId == null) remove(KEY_ACTIVE_PROFILE_ID) else putString(KEY_ACTIVE_PROFILE_ID, after.activeId)
            }
        }
    }

    companion object {
        const val CUSTOM_SOURCE = "Custom Import"
        const val CUSTOM_RIG = "N/A"
        private const val TAG = "EqProfileRepository"
        private const val KEY_PROFILES = "eq_profiles"
        private const val KEY_ACTIVE_PROFILE_ID = "active_profile_id"
    }
}
