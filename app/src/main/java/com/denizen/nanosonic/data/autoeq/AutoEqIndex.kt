package com.denizen.nanosonic.data.autoeq

/** Immutable, searchable view of the AutoEQ database. */
class AutoEqIndex(val entries: List<AutoEqEntry>) {

    val brands: List<Brand>
    private val modelsByBrand: Map<String, List<Model>>
    private val variantsByModel: Map<Pair<String, String>, List<AutoEqEntry>>

    init {
        val byBrand = entries.groupBy { it.brandKey }
        brands = byBrand.map { (key, brandEntries) ->
            Brand(
                key = key,
                name = mostCommon(brandEntries.map { BrandExtractor.brandOf(it.label) }),
                modelCount = brandEntries.distinctBy { it.modelKey }.size,
            )
        }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

        variantsByModel = entries.groupBy { it.brandKey to it.modelKey }
            .mapValues { (_, list) -> list.sortedWith(compareBy({ it.source }, { it.label }, { it.formDirectory })) }

        modelsByBrand = variantsByModel.entries
            .groupBy({ it.key.first }) { (key, variants) ->
                Model(
                    key = key.second,
                    brandKey = key.first,
                    name = mostCommon(variants.map { ModelNames.displayName(it.label) }),
                    variantCount = variants.size,
                )
            }
            .mapValues { (_, models) -> models.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }) }
    }

    fun searchBrands(query: String): List<Brand> = rank(brands, query) { it.name }

    fun searchModels(brandKey: String, query: String): List<Model> {
        val models = modelsByBrand[brandKey].orEmpty()
        val brandName = brands.firstOrNull { it.key == brandKey }?.name.orEmpty()
        // Match against the model part of the name, so "hd" finds "Sennheiser HD 600" by prefix.
        return rank(models, query) { it.name.removePrefix(brandName).trim() }
    }

    fun variants(brandKey: String, modelKey: String): List<AutoEqEntry> =
        variantsByModel[brandKey to modelKey].orEmpty()

    private fun <T> rank(items: List<T>, query: String, text: (T) -> String): List<T> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return items
        return items
            .mapNotNull { item ->
                val t = text(item).lowercase()
                val score = when {
                    t == q -> 0
                    t.startsWith(q) -> 1
                    t.split(' ', '-').any { it.startsWith(q) } -> 2
                    t.contains(q) -> 3
                    else -> return@mapNotNull null
                }
                score to item
            }
            .sortedBy { it.first } // stable: keeps alphabetical order within a score
            .map { it.second }
    }

    private fun mostCommon(values: List<String>): String =
        values.groupingBy { it }.eachCount().maxWith(compareBy<Map.Entry<String, Int>> { it.value }.thenByDescending { it.key }).key
}

/** Builds an [AutoEqIndex] from directory listings only; no profile file is opened. */
class AutoEqIndexBuilder(private val assets: AssetSource) {

    fun build(): AutoEqIndex {
        val rigs = RigResolver(assets)
        val entries = ArrayList<AutoEqEntry>(9_000)
        for (source in assets.list(RESULTS_DIR)) {
            if (source.endsWith(".md")) continue
            for (formDirectory in assets.list("$RESULTS_DIR/$source")) {
                val form = RigResolver.parseFormDirectory(formDirectory).form
                for (label in assets.list("$RESULTS_DIR/$source/$formDirectory")) {
                    entries += AutoEqEntry(
                        label = label,
                        source = source,
                        form = form,
                        rig = rigs.resolve(source, formDirectory, label),
                        formDirectory = formDirectory,
                        brandKey = BrandExtractor.keyOf(BrandExtractor.brandOf(label)),
                        modelKey = ModelNames.key(label),
                    )
                }
            }
        }
        return AutoEqIndex(entries)
    }

    companion object {
        const val RESULTS_DIR = "autoeqDB/results"
        const val MEASUREMENTS_DIR = "autoeqDB/measurements"
    }
}
