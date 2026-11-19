package com.denizen.nanosonic.data.autoeq

import com.denizen.nanosonic.data.eq.LegacyProfileIds

/** Groups measurement variants such as `X (ANC on)` and `X (ANC off)` under one model. */
object ModelNames {
    private val PARENTHESISED = Regex("""\([^)]*\)""")
    private val WHITESPACE = Regex("""\s+""")

    /** The label without any parenthesised variant details. */
    fun displayName(label: String): String = label.replace(PARENTHESISED, " ").replace(WHITESPACE, " ").trim()

    /** Stable key; identical to the model id earlier releases used for profile ids. */
    fun key(label: String): String = LegacyProfileIds.modelId(label)
}
