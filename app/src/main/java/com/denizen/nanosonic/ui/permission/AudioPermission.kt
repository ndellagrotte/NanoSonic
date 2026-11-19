package com.denizen.nanosonic.ui.permission

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.denizen.nanosonic.data.library.MediaStoreLibraryRepository

/** Launches the audio permission request and the app's system settings page. */
class AudioPermissionActions(
    val request: () -> Unit,
    val openAppSettings: () -> Unit,
)

/**
 * @param onResult called with the grant result, and with null after returning from system settings
 *   (where the user may have changed the permission).
 */
@Composable
fun rememberAudioPermissionActions(onResult: (granted: Boolean?) -> Unit): AudioPermissionActions {
    val context = LocalContext.current
    val requestLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onResult(it) }
    val settingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { onResult(null) }
    return remember(requestLauncher, settingsLauncher) {
        AudioPermissionActions(
            request = { requestLauncher.launch(MediaStoreLibraryRepository.AUDIO_PERMISSION) },
            openAppSettings = {
                settingsLauncher.launch(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                )
            },
        )
    }
}
