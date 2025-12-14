package com.example.nanosonicproject.ui.screens.wizard.databaseUtil.models

import kotlinx.serialization.Serializable

/**
 * Filter type enumeration for parametric EQ
 * Matches AutoEQ preset format conventions
 */
@Serializable
enum class FilterType {
    /** Peaking filter - boosts or cuts around a center frequency */
    PK,
    /** Low-shelf filter - affects frequencies below the cutoff */
    LSC,
    /** High-shelf filter - affects frequencies above the cutoff */
    HSC,
    /** Low-pass filter - attenuates frequencies above the cutoff */
    LPQ,
    /** High-pass filter - attenuates frequencies below the cutoff */
    HPQ
}