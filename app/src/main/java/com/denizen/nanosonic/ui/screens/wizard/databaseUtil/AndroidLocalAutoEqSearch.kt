package com.denizen.nanosonic.ui.screens.wizard.databaseUtil

import android.content.Context
import com.denizen.nanosonic.ui.screens.wizard.databaseUtil.models.Entry
import com.denizen.nanosonic.ui.screens.wizard.databaseUtil.models.ParametricEQ
import com.denizen.nanosonic.ui.screens.wizard.databaseUtil.parsers.ParametricEQParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android-compatible search interface for local AutoEq data in assets folder.
 *
 * This class works with Android's AssetManager to access the bundled database.
 * The database should be located in assets/autoeqDB/results/
 */
class AndroidLocalAutoEqSearch(private val context: Context) {

    private val entries = mutableListOf<Entry>()
    private var isIndexed = false

    // Cache for name_index.tsv data: Map<source, Map<headphoneName, rig>>
    private val rigLookupCache = mutableMapOf<String, Map<String, String>>()

    companion object {
        private const val AUTOEQ_DB_PATH = "autoeqDB/results"
    }

    /**
     * Build the search index by scanning assets/autoeqDB/results directory.
     * This should be called once when your app starts (ideally in a background thread).
     */
    suspend fun buildIndex(): Boolean = withContext(Dispatchers.IO) {
        try {
            println("Scanning assets directory: $AUTOEQ_DB_PATH")

            entries.clear()

            // Scan all result directories in assets
            // Structure: autoeqDB/results/{source}/{form}/{headphone_name}/
            val sources = context.assets.list(AUTOEQ_DB_PATH) ?: emptyArray()

            sources.forEach { source ->
                if (source.endsWith(".md")) return@forEach // Skip INDEX.md and README.md

                val sourcePath = "$AUTOEQ_DB_PATH/$source"
                val forms = context.assets.list(sourcePath) ?: emptyArray()

                forms.forEach { form ->
                    val formPath = "$sourcePath/$form"
                    val headphones = context.assets.list(formPath) ?: emptyArray()

                    headphones.forEach { headphoneName ->
                        try {
                            // Check if ParametricEQ.txt exists for this headphone
                            val eqPath = "$formPath/$headphoneName/$headphoneName ParametricEQ.txt"

                            // Try to open the file to verify it exists
                            context.assets.open(eqPath).use { }

                            // Parse form and rig from the form directory name (Step 1)
                            val (rigFromDir, parsedForm) = parseFormAndRig(form)

                            // Apply 3-step rig detection logic
                            val finalRig = determineRig(
                                rigFromDirectory = rigFromDir,
                                source = source,
                                headphoneName = headphoneName
                            )

                            val entry = Entry(
                                label = headphoneName,
                                form = parsedForm,
                                rig = finalRig,
                                source = source,
                                formDirectory = form  // Store actual directory name
                            )
                            entries.add(entry)
                        } catch (e: Exception) {
                            // Skip entries that don't have ParametricEQ.txt
                        }
                    }
                }
            }

            isIndexed = true
            println("Indexed ${entries.size} entries from assets")
            true
        } catch (e: Exception) {
            println("Failed to build index: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Search for headphones by brand name.
     * Returns unique brands found in the database.
     */
    fun searchBrands(query: String, maxResults: Int = 50): List<String> {
        if (!isIndexed) {
            println("WARNING: Index not built. Call buildIndex() first.")
            return emptyList()
        }

        if (query.isBlank()) return emptyList()

        val lowerQuery = query.lowercase().trim()

        // Extract brand names from headphone labels
        // Most headphone names start with the brand: "Sony WH-1000XM4", "Sennheiser HD 600", etc.
        val brands = entries
            .map { entry ->
                // Try to extract brand from the label (first word usually)
                val firstWord = entry.label.split(" ", "-", "(")[0]
                firstWord.trim()
            }
            .distinct()
            .filter { it.lowercase().contains(lowerQuery) }
            .sortedWith(compareByDescending<String> { brand ->
                when {
                    brand.lowercase() == lowerQuery -> 1000
                    brand.lowercase().startsWith(lowerQuery) -> 500
                    else -> 100
                }
            }.thenBy { it })
            .take(maxResults)

        return brands
    }

    /**
     * Search for models by brand name.
     * Returns entries that match the brand.
     */
    fun searchModelsByBrand(brandName: String, modelQuery: String = ""): List<Entry> {
        if (!isIndexed) {
            println("WARNING: Index not built. Call buildIndex() first.")
            return emptyList()
        }

        if (brandName.isBlank()) return emptyList()

        val lowerBrand = brandName.lowercase().trim()
        val lowerModelQuery = modelQuery.lowercase().trim()

        return entries
            .filter { entry ->
                // Check if entry label starts with or contains the brand name
                val labelLower = entry.label.lowercase()
                val matchesBrand = labelLower.startsWith(lowerBrand) ||
                        labelLower.startsWith("$lowerBrand ") ||
                        labelLower.contains(" $lowerBrand ") ||
                        labelLower.contains("-$lowerBrand-")

                // If model query is provided, also filter by model (using normalized name)
                val matchesModel = if (lowerModelQuery.isNotEmpty()) {
                    val normalizedLabel = normalizeModelName(entry.label).lowercase()
                    normalizedLabel.contains(lowerModelQuery)
                } else {
                    true
                }

                matchesBrand && matchesModel
            }
            .sortedWith(compareByDescending<Entry> { entry ->
                val labelLower = entry.label.lowercase()
                when {
                    lowerModelQuery.isNotEmpty() && labelLower == "$lowerBrand $lowerModelQuery" -> 2000
                    lowerModelQuery.isNotEmpty() && labelLower.startsWith("$lowerBrand $lowerModelQuery") -> 1500
                    labelLower.startsWith("$lowerBrand ") -> 1000
                    else -> 100
                }
            }.thenBy { it.label })
    }

    /**
     * Normalize model name by removing variant information in parentheses.
     * E.g., "Sony WH-1000XM4 (ANC ON)" -> "Sony WH-1000XM4"
     */
    private fun normalizeModelName(modelName: String): String {
        // Remove content in parentheses and trim
        return modelName.replace(Regex("""\s*\([^)]*\)\s*"""), "").trim()
    }

    /**
     * Search for headphones matching the query.
     * Searches in model name, source, and rig.
     */
    fun search(query: String, maxResults: Int = 50): List<Entry> {
        if (!isIndexed) {
            println("WARNING: Index not built. Call buildIndex() first.")
            return emptyList()
        }

        if (query.isBlank()) return emptyList()

        val lowerQuery = query.lowercase().trim()

        return entries
            .filter { entry ->
                entry.label.lowercase().contains(lowerQuery) ||
                        entry.source.lowercase().contains(lowerQuery) ||
                        entry.rig.lowercase().contains(lowerQuery)
            }
            .sortedWith(compareByDescending<Entry> { entry ->
                // Prioritize exact matches
                when {
                    entry.label.lowercase() == lowerQuery -> 1000
                    entry.label.lowercase().startsWith(lowerQuery) -> 500
                    else -> 100
                }
            }.thenBy { it.label })
            .take(maxResults)
    }

    /**
     * Group entries by normalized model name (removing variant information).
     * This consolidates entries like "WH-1000XM4 (ANC ON)" and "WH-1000XM4 (ANC OFF)"
     * into a single model group.
     */
    fun groupEntriesByModel(entries: List<Entry>): Map<String, List<Entry>> {
        return entries.groupBy { normalizeModelName(it.label) }
    }

    /**
     * Get all entries for a specific headphone model.
     * Returns all variants (different sources, rigs, measurement conditions, etc.) of the same model.
     */
    fun getVariantsForModel(modelName: String): List<Entry> {
        val normalizedSearch = normalizeModelName(modelName)
        return entries.filter {
            normalizeModelName(it.label).equals(normalizedSearch, ignoreCase = true)
        }
    }

    /**
     * Load the parametric EQ for a selected entry
     */
    suspend fun loadEQ(entry: Entry): ParametricEQ? = withContext(Dispatchers.IO) {
        try {
            val eqPath = getEQPath(entry)
            val content = context.assets.open(eqPath).bufferedReader().use { it.readText() }
            ParametricEQParser.parseText(content)
        } catch (e: Exception) {
            println("Failed to load EQ for ${entry.label}: ${e.message}")
            null
        }
    }

    /**
     * Get all indexed entries
     */
    fun getAllEntries(): List<Entry> = entries.toList()

    /**
     * Filter by source
     */
    fun filterBySource(source: String): List<Entry> {
        return entries.filter { it.source.equals(source, ignoreCase = true) }
    }

    /**
     * Filter by form (in-ear, over-ear, earbud)
     */
    fun filterByForm(form: String): List<Entry> {
        return entries.filter { it.form.equals(form, ignoreCase = true) }
    }

    /**
     * Filter by rig
     */
    fun filterByRig(rig: String): List<Entry> {
        return entries.filter { it.rig.equals(rig, ignoreCase = true) }
    }

    /**
     * Get all unique sources available
     */
    fun getAllSources(): List<String> {
        return entries.map { it.source }.distinct().sorted()
    }

    /**
     * Get all unique forms available
     */
    fun getAllForms(): List<String> {
        return entries.map { it.form }.distinct().sorted()
    }

    /**
     * Get all unique rigs available
     */
    fun getAllRigs(): List<String> {
        return entries.map { it.rig }.distinct().sorted()
    }

    /**
     * Get index statistics
     */
    fun getStatistics(): Map<String, Any> {
        return mapOf(
            "total_entries" to entries.size,
            "unique_headphones" to entries.map { it.label }.distinct().size,
            "sources" to getAllSources(),
            "forms" to getAllForms(),
            "rigs" to getAllRigs()
        )
    }

    // Private helper methods

    /**
     * 3-step rig detection logic:
     * Step 1: Use rig from directory structure (already parsed)
     * Step 2: If unknown, look up in measurements/{source}/name_index.tsv
     * Step 3: If still unknown and source is special, use default rig
     */
    private fun determineRig(rigFromDirectory: String, source: String, headphoneName: String): String {
        // Step 1: If we got a valid rig from directory structure, use it
        if (rigFromDirectory != "unknown" && rigFromDirectory.isNotBlank()) {
            return rigFromDirectory
        }

        // Step 2: Look up in name_index.tsv
        val rigFromIndex = lookupRigInNameIndex(source, headphoneName)
        if (rigFromIndex != null && rigFromIndex != "unknown") {
            return rigFromIndex
        }

        // Step 3: Fallback logic for special sources
        return when (source) {
            "Headphone.com Legacy", "Innerfidelity" -> "HMS II.3"
            else -> "Unknown"
        }
    }

    /**
     * Look up rig name in measurements/{source}/name_index.tsv
     * Returns null if not found or file doesn't exist
     */
    private fun lookupRigInNameIndex(source: String, headphoneName: String): String? {
        try {
            // Check if we've already loaded this source's index
            if (!rigLookupCache.containsKey(source)) {
                loadNameIndexForSource(source)
            }

            // Look up the headphone name in the cached index
            return rigLookupCache[source]?.get(headphoneName)
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Load and parse name_index.tsv for a given source
     */
    private fun loadNameIndexForSource(source: String) {
        try {
            val indexPath = "autoeqDB/measurements/$source/name_index.tsv"
            val rigMap = mutableMapOf<String, String>()

            context.assets.open(indexPath).bufferedReader().use { reader ->
                // Skip header line
                reader.readLine()

                // Parse each line: url\tsource_name\tname\tform\trig
                reader.forEachLine { line ->
                    val columns = line.split("\t")
                    if (columns.size >= 5) {
                        val name = columns[2].trim()
                        val rig = columns[4].trim()

                        if (name.isNotBlank() && rig.isNotBlank() && rig != "ignore") {
                            rigMap[name] = rig
                        }
                    }
                }
            }

            rigLookupCache[source] = rigMap
        } catch (e: Exception) {
            // If file doesn't exist or can't be read, cache empty map
            rigLookupCache[source] = emptyMap()
        }
    }

    private fun parseFormAndRig(formRig: String): Pair<String, String> {
        val formKeywords = listOf("in-ear", "over-ear", "earbud")

        val foundForm = formKeywords.firstOrNull {
            formRig.contains(it, ignoreCase = true)
        } ?: "unknown"

        val rig = formRig.replace(foundForm, "", ignoreCase = true).trim()

        return Pair(rig.ifEmpty { "unknown" }, foundForm)
    }

    private fun getEQPath(entry: Entry): String {
        // Use the actual directory name stored during indexing
        // This ensures we match the exact filesystem structure
        return "$AUTOEQ_DB_PATH/${entry.source}/${entry.formDirectory}/${entry.label}/${entry.label} ParametricEQ.txt"
    }
}