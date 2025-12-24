package com.denizen.nanosonic.ui.screens.wizard.databaseUtil.crawlers

import com.denizen.nanosonic.ui.screens.wizard.databaseUtil.indexing.NameIndex
import com.denizen.nanosonic.ui.screens.wizard.databaseUtil.models.NameItem
import java.io.File

/**
 * Crawler for Innerfidelity measurements.
 * Corresponds to Python's InnerfidelityCrawler from innerfidelity_crawler.py:19-23
 *
 * This crawler generates a name index for the Innerfidelity source
 * which doesn't have a name_index.tsv file. All measurements are hardcoded
 * to use the "HMS II.3" rig.
 */
class InnerfidelityCrawler(
    measurementsPath: File
) : AbstractCrawler(measurementsPath, "Innerfidelity") {

    /**
     * Read the name index by scanning the data directory
     * All measurements use the hardcoded rig "HMS II.3"
     */
    override fun readNameIndex(): NameIndex {
        val nameIndex = NameIndex()
        val dataDir = File(measurementsPath, "data")

        if (!dataDir.exists() || !dataDir.isDirectory) {
            println("Warning: Data directory not found: ${dataDir.absolutePath}")
            return nameIndex
        }

        // Scan for all CSV files
        dataDir.walkTopDown()
            .filter { it.extension == "csv" }
            .forEach { csvFile ->
                try {
                    // Get headphone name from filename (remove .csv extension)
                    val name = csvFile.nameWithoutExtension

                    // Get form from parent directory name
                    // Directory structure: data/{form}/{name}.csv
                    val form = csvFile.parentFile?.name ?: "unknown"
                    // All Innerfidelity measurements use HMS II.3 rig (hardcoded)
                    val item = NameItem(
                        name = name,
                        form = form,
                        rig = "HMS II.3"
                    )

                    nameIndex.add(item)
                } catch (e: Exception) {
                    println("Warning: Failed to process file: ${csvFile.absolutePath}")
                    println("Error: ${e.message}")
                }
            }

        println("InnerfidelityCrawler: Loaded ${nameIndex.size()} measurements")
        return nameIndex
    }
}
