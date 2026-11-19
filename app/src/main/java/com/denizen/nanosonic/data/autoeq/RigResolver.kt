package com.denizen.nanosonic.data.autoeq

/**
 * Determines the measurement rig of an entry:
 * 1. the rig named in the form directory (e.g. `711 in-ear`);
 * 2. otherwise `measurements/<source>/name_index.tsv` (name → rig, last row wins, `ignore` skipped);
 * 3. otherwise `HMS II.3` for the two legacy sources that only used that rig;
 * 4. otherwise `Unknown`.
 */
class RigResolver(private val assets: AssetSource) {
    private val nameIndexes = HashMap<String, Map<String, String>>()

    fun resolve(source: String, formDirectory: String, label: String): String {
        parseFormDirectory(formDirectory).rig?.let { return it }
        nameIndex(source)[label]?.takeUnless { it == UNKNOWN }?.let { return it }
        return if (source in HMS_SOURCES) "HMS II.3" else "Unknown"
    }

    private fun nameIndex(source: String): Map<String, String> = nameIndexes.getOrPut(source) {
        try {
            assets.open("${AutoEqIndexBuilder.MEASUREMENTS_DIR}/$source/name_index.tsv").bufferedReader().useLines { lines ->
                val map = HashMap<String, String>()
                lines.drop(1).forEach { line ->
                    val columns = line.split('\t')
                    if (columns.size >= 5) {
                        val name = columns[2].trim()
                        val rig = columns[4].trim()
                        if (name.isNotEmpty() && rig.isNotEmpty() && rig != "ignore") map[name] = rig
                    }
                }
                map
            }
        } catch (_: java.io.IOException) {
            emptyMap()
        }
    }

    data class FormAndRig(val form: String, val rig: String?)

    companion object {
        private const val UNKNOWN = "unknown"
        private val FORMS = listOf("in-ear", "over-ear", "earbud")
        private val HMS_SOURCES = setOf("Headphone.com Legacy", "Innerfidelity")

        /** Splits e.g. `Bruel & Kjaer 5128 over-ear` into form `over-ear` and rig `Bruel & Kjaer 5128`. */
        fun parseFormDirectory(formDirectory: String): FormAndRig {
            val form = FORMS.firstOrNull { formDirectory.contains(it, ignoreCase = true) } ?: UNKNOWN
            val rig = formDirectory.replace(form, "", ignoreCase = true).trim()
            return FormAndRig(form, rig.takeUnless { it.isEmpty() || it == UNKNOWN })
        }
    }
}
