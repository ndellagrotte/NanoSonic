package com.denizen.nanosonic.ui.eq

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.denizen.nanosonic.R
import com.denizen.nanosonic.core.model.EqProfile
import com.denizen.nanosonic.data.eq.EqProfileRepository
import com.denizen.nanosonic.ui.components.ConfirmDialog
import com.denizen.nanosonic.ui.components.OptionCard

@Composable
fun EqPage(
    state: EqUiState,
    onSelect: (String?) -> Unit,
    onDelete: (EqProfile) -> Unit,
) {
    val autoEqColor = MaterialTheme.colorScheme.primary
    val customColor = MaterialTheme.colorScheme.secondary
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp), // room for the FAB
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            OptionCard(
                onClick = { onSelect(null) },
                selected = state.activeProfileId == null,
                modifier = Modifier.padding(horizontal = 16.dp),
                leading = { RadioButton(selected = state.activeProfileId == null, onClick = null) },
            ) {
                Text(
                    stringResource(R.string.eq_disabled_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (state.activeProfileId == null) FontWeight.SemiBold else FontWeight.Normal,
                )
                Spacer(Modifier.height(2.dp))
                Text(stringResource(R.string.eq_disabled_message), style = MaterialTheme.typography.bodySmall)
            }
        }

        profileSection(R.string.eq_section_autoeq, autoEqColor, state.autoEqProfiles, state.activeProfileId, onSelect, onDelete)
        profileSection(R.string.eq_section_custom, customColor, state.customProfiles, state.activeProfileId, onSelect, onDelete)

        if (state.profileCount == 0) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Default.Equalizer, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.eq_empty_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.eq_empty_message), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun LazyListScope.profileSection(
    titleRes: Int,
    titleColor: Color,
    profiles: List<EqProfile>,
    activeId: String?,
    onSelect: (String?) -> Unit,
    onDelete: (EqProfile) -> Unit,
) {
    if (profiles.isEmpty()) return
    item(key = titleRes) {
        Text(
            stringResource(titleRes),
            style = MaterialTheme.typography.labelLarge,
            color = titleColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
    items(profiles, key = { it.id }) { profile ->
        ProfileCard(profile, selected = profile.id == activeId, onSelect = { onSelect(profile.id) }, onDelete = { onDelete(profile) })
    }
}

@Composable
private fun ProfileCard(profile: EqProfile, selected: Boolean, onSelect: () -> Unit, onDelete: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    OptionCard(
        onClick = onSelect,
        selected = selected,
        modifier = Modifier.padding(horizontal = 16.dp),
        leading = { RadioButton(selected = selected, onClick = null) },
        trailing = {
            IconButton(onClick = { confirmDelete = true }) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.eq_delete_profile), tint = MaterialTheme.colorScheme.error)
            }
        },
    ) {
        Text(
            profile.deviceModel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
        if (profile.source != "unknown" && profile.source != EqProfileRepository.CUSTOM_SOURCE) {
            Spacer(Modifier.height(2.dp))
            Text(stringResource(R.string.eq_profile_source, profile.source, profile.rig), style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(2.dp))
        Text(
            pluralStringResource(R.plurals.eq_band_count, profile.eq.bands.size, profile.eq.bands.size),
            style = MaterialTheme.typography.bodySmall,
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.eq_delete_title),
            message = stringResource(R.string.eq_delete_message, profile.name),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = onDelete,
            onDismiss = { confirmDelete = false },
        )
    }
}

/** "+" button with a menu to import from the AutoEQ wizard or from a file. */
@Composable
fun EqImportFab(onImportViaWizard: () -> Unit, onImportFile: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FloatingActionButton(
            onClick = { expanded = true },
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary,
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.eq_import))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, properties = PopupProperties(focusable = true)) {
            MenuRow(Icons.Default.AutoFixHigh, MaterialTheme.colorScheme.primary, stringResource(R.string.eq_import_wizard)) {
                expanded = false
                onImportViaWizard()
            }
            MenuRow(Icons.Default.Upload, MaterialTheme.colorScheme.secondary, stringResource(R.string.eq_import_custom)) {
                expanded = false
                onImportFile()
            }
        }
    }
}

@Composable
private fun MenuRow(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, text: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(icon, contentDescription = null, tint = tint)
                Text(text)
            }
        },
        onClick = onClick,
    )
}
