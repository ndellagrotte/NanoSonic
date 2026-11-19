package com.denizen.nanosonic.data.autoeq

/** One measured headphone in the bundled AutoEQ database (one `ParametricEQ.txt`). */
data class AutoEqEntry(
    /** Directory/headphone name, e.g. `Sony WH-1000XM4 (ANC on)`. */
    val label: String,
    /** Measurement source directory, e.g. `oratory1990`. */
    val source: String,
    /** `in-ear`, `over-ear`, `earbud` or `unknown`. */
    val form: String,
    val rig: String,
    /** The actual form directory name; needed to rebuild the asset path. */
    val formDirectory: String,
    val brandKey: String,
    val modelKey: String,
) {
    val assetPath: String
        get() = "${AutoEqIndexBuilder.RESULTS_DIR}/$source/$formDirectory/$label/$label ParametricEQ.txt"

}

data class Brand(val key: String, val name: String, val modelCount: Int)

data class Model(val key: String, val brandKey: String, val name: String, val variantCount: Int)
