package com.denizen.nanosonic.ui.eq

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denizen.nanosonic.core.model.EqProfile
import com.denizen.nanosonic.data.eq.EqFileImporter
import com.denizen.nanosonic.data.eq.EqProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EqUiState(
    val autoEqProfiles: List<EqProfile> = emptyList(),
    val customProfiles: List<EqProfile> = emptyList(),
    val activeProfileId: String? = null,
    val message: EqMessage? = null,
) {
    val profileCount: Int get() = autoEqProfiles.size + customProfiles.size
}

sealed interface EqMessage {
    data object ImportSucceeded : EqMessage
    data class ImportFailed(val reason: String) : EqMessage
}

@HiltViewModel
class EqViewModel @Inject constructor(
    private val profiles: EqProfileRepository,
    private val importer: EqFileImporter,
) : ViewModel() {

    private val message = MutableStateFlow<EqMessage?>(null)

    val state: StateFlow<EqUiState> = combine(profiles.state.filterNotNull(), message) { stored, msg ->
        val sorted = stored.sorted
        EqUiState(
            autoEqProfiles = sorted.filterNot { it.isCustom },
            customProfiles = sorted.filter { it.isCustom },
            activeProfileId = stored.active?.id,
            message = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EqUiState())

    /** Activates a profile, or disables EQ when [id] is null. The playback service applies it. */
    fun select(id: String?) {
        viewModelScope.launch { profiles.setActive(id) }
    }

    fun delete(id: String) {
        viewModelScope.launch { profiles.delete(id) }
    }

    fun import(uri: Uri) {
        viewModelScope.launch {
            message.value = try {
                val file = importer.read(uri)
                profiles.importCustom(file.name, file.eq)
                EqMessage.ImportSucceeded
            } catch (e: EqFileImporter.InvalidEqFileException) {
                EqMessage.ImportFailed(e.message.orEmpty())
            } catch (e: java.io.IOException) {
                EqMessage.ImportFailed(e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun onMessageShown() = message.update { null }
}
