package com.denizen.nanosonic.data.autoeq

import com.denizen.nanosonic.core.model.ParametricEq
import com.denizen.nanosonic.di.DefaultDispatcher
import com.denizen.nanosonic.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** The AutoEQ profile database bundled in `assets/autoeqDB`. The index is built once and cached. */
@Singleton
class AutoEqDatabase @Inject constructor(
    private val assets: AssetSource,
    @IoDispatcher private val io: CoroutineDispatcher,
    @DefaultDispatcher private val cpu: CoroutineDispatcher,
) {
    private val mutex = Mutex()
    @Volatile private var cached: AutoEqIndex? = null

    suspend fun index(): AutoEqIndex = cached ?: mutex.withLock {
        cached ?: withContext(io) { AutoEqIndexBuilder(assets).build() }.also { cached = it }
    }

    suspend fun searchBrands(query: String): List<Brand> = index().let { withContext(cpu) { it.searchBrands(query) } }

    suspend fun searchModels(brandKey: String, query: String): List<Model> =
        index().let { withContext(cpu) { it.searchModels(brandKey, query) } }

    suspend fun variants(brandKey: String, modelKey: String): List<AutoEqEntry> = index().variants(brandKey, modelKey)

    suspend fun loadEq(entry: AutoEqEntry): ParametricEq = withContext(io) {
        assets.open(entry.assetPath).bufferedReader().use { ParametricEqParser.parse(it.readText()) }
    }
}
