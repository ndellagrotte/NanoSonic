package com.denizen.nanosonic.data.autoeq

/**
 * Extracts the manufacturer from an AutoEq headphone name. The database has no brand column, so this
 * uses the naming conventions of AutoEq: the brand is the first word, except for brands whose second
 * word is a generic company suffix ("64 Audio", "Kiwi Ears", "IMR Acoustics") and a few known
 * multi-word brands. Hyphenated names such as "Audio-Technica" stay whole.
 */
object BrandExtractor {
    private val GENERIC_SECOND_WORDS = setOf(
        "audio", "ears", "acoustics", "hifi", "hi-fi", "lab", "labs", "technology", "acous", "electronics",
    )

    private val MULTI_WORD_BRANDS = listOf(
        "Dan Clark Audio",
        "Bang & Olufsen", "Bowers & Wilkins", "Astell & Kern", "Master & Dynamic",
        "Alpha & Delta", "Allen & Heath", "Shozy & Neo",
        "Tansio Mirai", "Unique Melody", "Eternal Melody", "Turtle Beach", "Custom Art",
        "Acoustic Research", "Canal Works", "SoftEars Studio", "Lark Studio",
        "Cat Ear", "Spiral Ear", "Flicker Ear", "Just Ear",
    ).sortedByDescending { it.length }

    private val WHITESPACE = Regex("""\s+""")

    fun brandOf(label: String): String {
        val name = label.trim()
        MULTI_WORD_BRANDS.firstOrNull { name.startsWithWord(it) }?.let { return name.substring(0, it.length) }

        val words = name.split(WHITESPACE)
        // "Audio Zenith PMx2": a generic first word is never a brand on its own.
        if (words.size >= 2 && words[0].lowercase() in GENERIC_SECOND_WORDS) return "${words[0]} ${words[1]}"
        if (words.size >= 3 && words[1].lowercase() in GENERIC_SECOND_WORDS) return "${words[0]} ${words[1]}"
        return words.first()
    }

    /** Case-insensitive key used to merge spellings like `1More` and `1MORE`. */
    fun keyOf(brand: String): String = brand.lowercase()

    private fun String.startsWithWord(prefix: String): Boolean =
        startsWith(prefix, ignoreCase = true) && (length == prefix.length || this[prefix.length].isWhitespace())
}
