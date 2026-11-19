package com.denizen.nanosonic.ui.wizard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denizen.nanosonic.core.model.EqProfile
import com.denizen.nanosonic.data.autoeq.AutoEqDatabase
import com.denizen.nanosonic.data.autoeq.AutoEqEntry
import com.denizen.nanosonic.data.autoeq.Brand
import com.denizen.nanosonic.data.autoeq.Model
import com.denizen.nanosonic.data.eq.EqProfileRepository
import com.denizen.nanosonic.data.eq.LegacyProfileIds
import com.denizen.nanosonic.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class WizardStep { BRAND, MODEL, PROFILES }

data class WizardUiState(
    val step: WizardStep = WizardStep.BRAND,
    val isDatabaseLoading: Boolean = true,
    val brandQuery: String = "",
    val brands: List<Brand> = emptyList(),
    val brand: Brand? = null,
    val modelQuery: String = "",
    val models: List<Model> = emptyList(),
    val model: Model? = null,
    val variants: List<AutoEqEntry> = emptyList(),
    val selected: Set<AutoEqEntry> = emptySet(),
    val isSaving: Boolean = false,
    val error: WizardError? = null,
    val isFinished: Boolean = false,
) {
    val canComplete: Boolean get() = selected.isNotEmpty() && !isSaving
}

sealed interface WizardError {
    data object DatabaseUnavailable : WizardError
    data class SaveFailed(val reason: String) : WizardError
}

/** Brand → model → profile selection over the bundled AutoEQ database. */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class WizardViewModel @Inject constructor(
    private val database: AutoEqDatabase,
    private val profiles: EqProfileRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(WizardUiState())
    val state: StateFlow<WizardUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                database.index()
                _state.update { it.copy(isDatabaseLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isDatabaseLoading = false, error = WizardError.DatabaseUnavailable) }
            }
        }
        viewModelScope.launch {
            _state.map { it.brandQuery }
                .distinctUntilChanged()
                .debounce { if (it.isEmpty()) 0 else SEARCH_DEBOUNCE_MS }
                .mapLatest { database.searchBrands(it) }
                .collect { brands -> _state.update { it.copy(brands = brands) } }
        }
        viewModelScope.launch {
            _state.map { it.brand?.key to it.modelQuery }
                .distinctUntilChanged()
                .debounce { (_, query) -> if (query.isEmpty()) 0 else SEARCH_DEBOUNCE_MS }
                .mapLatest { (brandKey, query) -> brandKey?.let { database.searchModels(it, query) }.orEmpty() }
                .collect { models -> _state.update { it.copy(models = models) } }
        }
    }

    fun onBrandQueryChanged(query: String) = _state.update { it.copy(brandQuery = query) }

    fun onBrandSelected(brand: Brand) = _state.update {
        it.copy(step = WizardStep.MODEL, brand = brand, modelQuery = "", models = emptyList())
    }

    fun onModelQueryChanged(query: String) = _state.update { it.copy(modelQuery = query) }

    fun onModelSelected(model: Model) {
        viewModelScope.launch {
            val variants = database.variants(model.brandKey, model.key)
            _state.update { it.copy(step = WizardStep.PROFILES, model = model, variants = variants, selected = emptySet()) }
        }
    }

    fun onVariantToggled(entry: AutoEqEntry) = _state.update {
        it.copy(selected = if (entry in it.selected) it.selected - entry else it.selected + entry)
    }

    /** @return false when already on the first step. */
    fun back(): Boolean {
        val current = _state.value
        when (current.step) {
            WizardStep.BRAND -> return false
            WizardStep.MODEL -> _state.update { it.copy(step = WizardStep.BRAND, brand = null) }
            WizardStep.PROFILES -> _state.update { it.copy(step = WizardStep.MODEL, model = null, variants = emptyList(), selected = emptySet()) }
        }
        return true
    }

    fun complete() {
        val current = _state.value
        val model = current.model ?: return
        if (!current.canComplete) return
        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val newProfiles = current.variants.withIndex()
                    .filter { (_, entry) -> entry in current.selected }
                    .map { (index, entry) ->
                        EqProfile(
                            id = LegacyProfileIds.autoEqProfileId(model.key, index),
                            name = entry.label,
                            deviceModel = entry.label,
                            source = entry.source,
                            rig = entry.rig,
                            eq = database.loadEq(entry),
                            isCustom = false,
                            addedTimestamp = now,
                        )
                    }
                profiles.upsertAutoEq(newProfiles)
                settings.setAppInitialized(true)
                _state.update { it.copy(isSaving = false, isFinished = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = WizardError.SaveFailed(e.message ?: e.javaClass.simpleName)) }
            }
        }
    }

    fun onErrorShown() = _state.update { it.copy(error = null) }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 250L
    }
}
