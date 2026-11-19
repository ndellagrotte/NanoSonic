package com.denizen.nanosonic.data.eq

/**
 * Reproduces the id scheme earlier releases used for profiles, so ids stay comparable with stored data.
 *
 * Wizard profiles: `<modelId>_<variantIndex>`, where modelId is the AutoEq label with every
 * parenthesised group removed, lowercased, spaces turned into `_`, and anything outside `[a-z0-9_]` dropped.
 */
object LegacyProfileIds {
    private val PARENTHESISED = Regex("""\s*\([^)]*\)\s*""")
    private val NON_ID_CHARS = Regex("[^a-z0-9_]")

    fun modelId(label: String): String =
        label.replace(PARENTHESISED, "").trim()
            .lowercase()
            .replace(" ", "_")
            .replace(NON_ID_CHARS, "")

    fun autoEqProfileId(modelId: String, variantIndex: Int): String = "${modelId}_$variantIndex"

    fun customProfileId(name: String, nowMs: Long): String = "custom_${nowMs}_${name.hashCode()}"
}
