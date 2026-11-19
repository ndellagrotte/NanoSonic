package com.denizen.nanosonic.core.model

/** Filter types from the Equalizer APO / AutoEq format. Persisted by name; never rename entries. */
enum class FilterType {
    /** Peaking */
    PK,
    /** Low shelf */
    LSC,
    /** High shelf */
    HSC,
    /** Low pass */
    LPQ,
    /** High pass */
    HPQ,
}

data class EqBand(
    val frequency: Double,
    val gain: Double,
    val q: Double,
    val filterType: FilterType,
    val enabled: Boolean = true,
)

data class ParametricEq(
    val preampDb: Double,
    val bands: List<EqBand>,
)

data class EqProfile(
    val id: String,
    val name: String,
    val deviceModel: String,
    val source: String,
    val rig: String,
    val eq: ParametricEq,
    val isCustom: Boolean,
    val addedTimestamp: Long,
)
