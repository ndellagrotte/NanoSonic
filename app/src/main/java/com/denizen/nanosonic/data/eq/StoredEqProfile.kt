package com.denizen.nanosonic.data.eq

import com.denizen.nanosonic.core.model.EqBand
import com.denizen.nanosonic.core.model.EqProfile
import com.denizen.nanosonic.core.model.FilterType
import com.denizen.nanosonic.core.model.ParametricEq
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/*
 * On-disk format of saved EQ profiles (SharedPreferences "nanosonic_eq_profiles", key "eq_profiles").
 *
 * These classes mirror the JSON written by every previous release. Field names and default values are
 * part of that format: the encoder omits fields equal to their default, so changing a default would
 * silently change the meaning of existing data. Do not rename, reorder semantics, or change defaults.
 */

@Serializable
internal data class StoredEqBand(
    val frequency: Double,
    val gain: Double,
    val q: Double = 1.41,
    val filterType: FilterType = FilterType.PK,
    val enabled: Boolean = true,
)

@Serializable
internal data class StoredEqProfile(
    val id: String,
    val name: String,
    val deviceModel: String,
    val source: String,
    val rig: String,
    val bands: List<StoredEqBand>,
    val preamp: Double = 0.0,
    val isCustom: Boolean = false,
    /** Legacy, always false. The active profile is stored separately under "active_profile_id". */
    val isActive: Boolean = false,
    val addedTimestamp: Long = System.currentTimeMillis(),
)

/** Converts between the stored JSON array and [EqProfile]s. */
object EqProfileCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun decode(text: String): List<EqProfile> =
        json.decodeFromString<List<StoredEqProfile>>(text).map { it.toDomain() }

    fun encode(profiles: List<EqProfile>): String =
        json.encodeToString(profiles.map { it.toStored() })

    private fun StoredEqProfile.toDomain() = EqProfile(
        id = id,
        name = name,
        deviceModel = deviceModel,
        source = source,
        rig = rig,
        eq = ParametricEq(
            preampDb = preamp,
            bands = bands.map { EqBand(it.frequency, it.gain, it.q, it.filterType, it.enabled) },
        ),
        isCustom = isCustom,
        addedTimestamp = addedTimestamp,
    )

    private fun EqProfile.toStored() = StoredEqProfile(
        id = id,
        name = name,
        deviceModel = deviceModel,
        source = source,
        rig = rig,
        bands = eq.bands.map { StoredEqBand(it.frequency, it.gain, it.q, it.filterType, it.enabled) },
        preamp = eq.preampDb,
        isCustom = isCustom,
        isActive = false,
        addedTimestamp = addedTimestamp,
    )
}
