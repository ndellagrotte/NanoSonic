package com.denizen.nanosonic.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denizen.nanosonic.R
import com.denizen.nanosonic.core.model.GaplessMode
import com.denizen.nanosonic.core.model.ThemeMode
import com.denizen.nanosonic.ui.components.OptionCard
import com.denizen.nanosonic.ui.components.RadioOptionDialog
import com.denizen.nanosonic.ui.permission.rememberAudioPermissionActions

@Composable
fun SettingsDialog(onDismiss: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var picker by remember { mutableStateOf<Picker?>(null) }
    val permission = rememberAudioPermissionActions { viewModel.onPermissionChanged() }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(32.dp)) },
        title = { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Section(stringResource(R.string.settings_appearance)) {
                    SettingRow(Icons.Default.Palette, stringResource(R.string.settings_theme), themeLabel(state.themeMode), onClick = { picker = Picker.THEME })
                }
                Section(stringResource(R.string.settings_playback)) {
                    SettingRow(Icons.Default.MusicNote, stringResource(R.string.settings_gapless), gaplessLabel(state.gaplessMode), onClick = { picker = Picker.GAPLESS })
                }
                Section(stringResource(R.string.settings_permissions)) {
                    // A permission can't be revoked from inside the app, so switching off opens system settings.
                    val toggle = { if (state.hasAudioPermission) permission.openAppSettings() else permission.request() }
                    SettingRow(
                        icon = Icons.Default.Folder,
                        title = stringResource(R.string.settings_music_folder),
                        subtitle = stringResource(if (state.hasAudioPermission) R.string.settings_access_granted else R.string.settings_access_denied),
                        onClick = toggle,
                        trailing = { Switch(checked = state.hasAudioPermission, onCheckedChange = { toggle() }) },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) } },
    )

    when (picker) {
        Picker.THEME -> RadioOptionDialog(
            title = stringResource(R.string.settings_theme),
            icon = Icons.Default.Palette,
            options = ThemeMode.entries,
            selected = state.themeMode,
            label = { themeLabel(it) },
            onSelect = { viewModel.setThemeMode(it); picker = null },
            onDismiss = { picker = null },
        )
        Picker.GAPLESS -> RadioOptionDialog(
            title = stringResource(R.string.settings_gapless),
            icon = Icons.Default.MusicNote,
            description = stringResource(R.string.settings_gapless_description),
            options = GaplessMode.entries,
            selected = state.gaplessMode,
            label = { gaplessLabel(it) },
            onSelect = { viewModel.setGaplessMode(it); picker = null },
            onDismiss = { picker = null },
        )
        null -> Unit
    }
}

private enum class Picker { THEME, GAPLESS }

@Composable
private fun themeLabel(mode: ThemeMode) = stringResource(
    when (mode) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    },
)

@Composable
private fun gaplessLabel(mode: GaplessMode) = stringResource(
    when (mode) {
        GaplessMode.ALBUMS_ONLY -> R.string.gapless_albums_only
        GaplessMode.ENABLED -> R.string.gapless_enabled
        GaplessMode.DISABLED -> R.string.gapless_disabled
    },
)

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        content()
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = { Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp)) },
) {
    OptionCard(onClick = onClick, leading = { Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp)) }, trailing = trailing) {
        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall)
    }
}
