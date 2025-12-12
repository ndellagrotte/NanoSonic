package com.example.nanosonicproject.data

import android.content.Context
import android.content.SharedPreferences
import com.example.nanosonicproject.ui.screens.wizard.databaseUtil.models.GraphicEQ
import com.example.nanosonicproject.ui.screens.wizard.databaseUtil.models.GraphicEQBand
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

/**
 * Saved EQ Profile with metadata
 */
@Serializable
data class SavedEQProfile(
    val id: String,                    // Unique identifier
    val name: String,                  // Display name
    val deviceModel: String,           // e.g., "Sony WH-1000XM4" or "Custom"
    val source: String,                // e.g., "oratory1990" or "Custom Import"
    val rig: String,                   // e.g., "HMS II.3" or "N/A"
    val bands: List<GraphicEQBand>,    // EQ bands
    val preamp: Double = 0.0,          // Preamp gain in dB
    val isCustom: Boolean = false,     // Whether this is a custom imported profile
    val isActive: Boolean = false,     // Whether this profile is currently active
    val addedTimestamp: Long = System.currentTimeMillis()
)

/**
 * Repository for managing EQ profiles
 * Handles saving, loading, and activating EQ profiles
 */
@Singleton
class EQProfileRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "nanosonic_eq_profiles",
        Context.MODE_PRIVATE
    )

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val _profiles = MutableStateFlow<List<SavedEQProfile>>(emptyList())
    val profiles: StateFlow<List<SavedEQProfile>> = _profiles.asStateFlow()

    private val _activeProfile = MutableStateFlow<SavedEQProfile?>(null)
    val activeProfile: StateFlow<SavedEQProfile?> = _activeProfile.asStateFlow()

    companion object {
        private const val KEY_PROFILES = "eq_profiles"
        private const val KEY_ACTIVE_PROFILE_ID = "active_profile_id"
        private const val KEY_WIZARD_COMPLETED = "wizard_completed"
    }

    init {
        loadProfiles()
    }

    /**
     * Load all saved profiles from SharedPreferences
     */
    private fun loadProfiles() {
        try {
            val profilesJson = prefs.getString(KEY_PROFILES, null)
            if (profilesJson != null) {
                val loadedProfiles = json.decodeFromString<List<SavedEQProfile>>(profilesJson)
                _profiles.value = loadedProfiles

                // Load active profile
                val activeId = prefs.getString(KEY_ACTIVE_PROFILE_ID, null)
                _activeProfile.value = loadedProfiles.find { it.id == activeId }
            }
        } catch (e: Exception) {
            println("Error loading EQ profiles: ${e.message}")
            _profiles.value = emptyList()
            _activeProfile.value = null
        }
    }

    /**
     * Save a new EQ profile
     */
    suspend fun saveProfile(profile: SavedEQProfile) = withContext(Dispatchers.IO) {
        val currentProfiles = _profiles.value.toMutableList()

        // Check if profile with same ID already exists
        val existingIndex = currentProfiles.indexOfFirst { it.id == profile.id }

        if (existingIndex >= 0) {
            // Update existing profile
            currentProfiles[existingIndex] = profile
        } else {
            // Add new profile
            currentProfiles.add(profile)
        }

        // Save to SharedPreferences
        val profilesJson = json.encodeToString<List<SavedEQProfile>>(currentProfiles)
        prefs.edit { putString(KEY_PROFILES, profilesJson) }

        _profiles.value = currentProfiles
    }

    /**
     * Save multiple profiles at once (useful for wizard)
     */
    suspend fun saveProfiles(profiles: List<SavedEQProfile>) = withContext(Dispatchers.IO) {
        val currentProfiles = _profiles.value.toMutableList()

        profiles.forEach { newProfile ->
            val existingIndex = currentProfiles.indexOfFirst { it.id == newProfile.id }
            if (existingIndex >= 0) {
                currentProfiles[existingIndex] = newProfile
            } else {
                currentProfiles.add(newProfile)
            }
        }

        val profilesJson = json.encodeToString<List<SavedEQProfile>>(currentProfiles)
        prefs.edit { putString(KEY_PROFILES, profilesJson) }

        _profiles.value = currentProfiles
    }

    /**
     * Delete a profile
     */
    suspend fun deleteProfile(profileId: String) = withContext(Dispatchers.IO) {
        val currentProfiles = _profiles.value.toMutableList()
        currentProfiles.removeAll { it.id == profileId }

        val profilesJson = json.encodeToString<List<SavedEQProfile>>(currentProfiles)
        prefs.edit { putString(KEY_PROFILES, profilesJson) }

        // If deleted profile was active, clear active profile
        if (_activeProfile.value?.id == profileId) {
            _activeProfile.value = null
            prefs.edit { remove(KEY_ACTIVE_PROFILE_ID) }
        }

        _profiles.value = currentProfiles
    }

    /**
     * Set a profile as active (only one profile can be active at a time)
     * Pass null to deactivate all profiles
     */
    suspend fun setActiveProfile(profileId: String?) = withContext(Dispatchers.IO) {
        val currentProfiles = _profiles.value

        if (profileId == null) {
            // Deactivate all profiles
            _activeProfile.value = null
            prefs.edit { remove(KEY_ACTIVE_PROFILE_ID) }
        } else {
            val profile = currentProfiles.find { it.id == profileId }
            _activeProfile.value = profile
            prefs.edit { putString(KEY_ACTIVE_PROFILE_ID, profileId) }
        }
    }

    /**
     * Get all saved profiles
     */
    fun getAllProfiles(): List<SavedEQProfile> {
        return _profiles.value
    }

    /**
     * Get active profile
     */
    fun getActiveProfile(): SavedEQProfile? {
        return _activeProfile.value
    }

    /**
     * Check if wizard has been completed
     */
    fun isWizardCompleted(): Boolean {
        return prefs.getBoolean(KEY_WIZARD_COMPLETED, false)
    }

    /**
     * Mark wizard as completed
     */
    suspend fun markWizardCompleted() = withContext(Dispatchers.IO) {
        prefs.edit { putBoolean(KEY_WIZARD_COMPLETED, true) }
    }

    /**
     * Clear all profiles and reset wizard state (for testing/debugging)
     */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        prefs.edit { clear() }
        _profiles.value = emptyList()
        _activeProfile.value = null
    }

    /**
     * Convert GraphicEQ to SavedEQProfile
     */
    fun createSavedProfile(
        id: String,
        name: String,
        deviceModel: String,
        source: String,
        rig: String,
        graphicEQ: GraphicEQ,
        preamp: Double = 0.0,
        isCustom: Boolean = false
    ): SavedEQProfile {
        return SavedEQProfile(
            id = id,
            name = name,
            deviceModel = deviceModel,
            source = source,
            rig = rig,
            bands = graphicEQ.bands,
            preamp = preamp,
            isCustom = isCustom,
            isActive = false
        )
    }

    /**
     * Import a custom EQ profile from GraphicEQ data (legacy)
     */
    suspend fun importCustomProfile(
        name: String,
        graphicEQ: GraphicEQ
    ) = withContext(Dispatchers.IO) {
        // Generate unique ID for custom profile
        val id = "custom_${System.currentTimeMillis()}_${name.hashCode()}"

        val customProfile = SavedEQProfile(
            id = id,
            name = name,
            deviceModel = "Custom",
            source = "Custom Import",
            rig = "N/A",
            bands = graphicEQ.bands,
            preamp = 0.0,
            isCustom = true,
            isActive = false
        )

        saveProfile(customProfile)
    }

    /**
     * Import a custom EQ profile from FixedBandEQ data
     */
    suspend fun importCustomProfileFromFixedBandEQ(
        name: String,
        fixedBandEQ: com.example.nanosonicproject.ui.screens.wizard.databaseUtil.models.FixedBandEQ
    ) = withContext(Dispatchers.IO) {
        // Generate unique ID for custom profile
        val id = "custom_${System.currentTimeMillis()}_${name.hashCode()}"

        val customProfile = SavedEQProfile(
            id = id,
            name = name,
            deviceModel = "Custom",
            source = "Custom Import",
            rig = "N/A",
            bands = fixedBandEQ.bands.map { GraphicEQBand(it.frequency, it.gain) },
            preamp = fixedBandEQ.preamp,
            isCustom = true,
            isActive = false
        )

        saveProfile(customProfile)
    }

    /**
     * Get profiles sorted by type: AutoEQ first, then custom profiles
     * Within each group, sort by timestamp (newest first)
     */
    fun getSortedProfiles(): List<SavedEQProfile> {
        val allProfiles = _profiles.value

        // Separate AutoEQ and custom profiles
        val autoEqProfiles = allProfiles.filter { !it.isCustom }
            .sortedByDescending { it.addedTimestamp }

        val customProfiles = allProfiles.filter { it.isCustom }
            .sortedByDescending { it.addedTimestamp }

        // Return AutoEQ profiles first, then custom profiles
        return autoEqProfiles + customProfiles
    }
}