package com.denizen.nanosonic.ui.main

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denizen.nanosonic.R
import com.denizen.nanosonic.data.library.LibraryState
import com.denizen.nanosonic.ui.about.AboutDialog
import com.denizen.nanosonic.ui.components.AppTopBar
import com.denizen.nanosonic.ui.components.MessageDialog
import com.denizen.nanosonic.ui.components.NowPlayingBar
import com.denizen.nanosonic.ui.eq.EqImportFab
import com.denizen.nanosonic.ui.eq.EqMessage
import com.denizen.nanosonic.ui.eq.EqPage
import com.denizen.nanosonic.ui.eq.EqViewModel
import com.denizen.nanosonic.ui.library.AlbumsPage
import com.denizen.nanosonic.ui.library.LibraryPage
import com.denizen.nanosonic.ui.library.LibraryViewModel
import com.denizen.nanosonic.ui.permission.rememberAudioPermissionActions
import com.denizen.nanosonic.ui.player.PlayerViewModel
import com.denizen.nanosonic.ui.settings.SettingsDialog
import kotlinx.coroutines.launch

enum class MainTab(@StringRes val label: Int, val icon: ImageVector) {
    LIBRARY(R.string.tab_library, Icons.Default.MusicNote),
    ALBUMS(R.string.tab_albums, Icons.Default.Album),
    EQ(R.string.tab_eq, Icons.Default.Equalizer),
}

private enum class MainDialog { SETTINGS, ABOUT }

/** Library, Albums and EQ tabs (swipeable), with the mini player above the bottom navigation. */
@Composable
fun MainScreen(
    onOpenWizard: () -> Unit,
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    eqViewModel: EqViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
) {
    val library by libraryViewModel.state.collectAsStateWithLifecycle()
    val permissionDenied by libraryViewModel.permissionDenied.collectAsStateWithLifecycle()
    val eq by eqViewModel.state.collectAsStateWithLifecycle()
    val player by playerViewModel.state.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState { MainTab.entries.size }
    val tab = MainTab.entries[pagerState.targetPage]
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var dialog by rememberSaveable { mutableStateOf<MainDialog?>(null) }

    val permission = rememberAudioPermissionActions(libraryViewModel::onPermissionResult)
    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(eqViewModel::import)
    }

    // The permission may have been changed in system settings while we were in the background.
    LifecycleResumeEffect(Unit) {
        libraryViewModel.onPermissionResult(null)
        onPauseOrDispose { }
    }

    val successText = stringResource(R.string.eq_import_success)
    LaunchedEffect(eq.message) {
        if (eq.message == EqMessage.ImportSucceeded) {
            eqViewModel.onMessageShown()
            snackbarHostState.showSnackbar(successText)
        }
    }

    Scaffold(
        topBar = {
            val tracks = (library as? LibraryState.Loaded)?.tracks?.size
            val albums = (library as? LibraryState.Loaded)?.albums?.size
            AppTopBar(
                title = stringResource(
                    when (tab) {
                        MainTab.LIBRARY -> R.string.library_title
                        MainTab.ALBUMS -> R.string.albums_title
                        MainTab.EQ -> R.string.eq_title
                    },
                ),
                subtitle = when (tab) {
                    MainTab.LIBRARY -> tracks?.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.track_count, it, it) }
                    MainTab.ALBUMS -> albums?.let { pluralStringResource(R.plurals.album_count, it, it) }
                    MainTab.EQ -> pluralStringResource(R.plurals.profile_count, eq.profileCount, eq.profileCount)
                },
                onSettings = { dialog = MainDialog.SETTINGS },
                onAbout = { dialog = MainDialog.ABOUT },
            )
        },
        bottomBar = {
            Column(Modifier.fillMaxWidth()) {
                // Keep showing the last track while the bar slides out.
                var lastNowPlaying by remember { mutableStateOf(player.nowPlaying) }
                player.nowPlaying?.let { lastNowPlaying = it }
                AnimatedVisibility(
                    visible = player.nowPlaying != null,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it },
                ) {
                    lastNowPlaying?.let { nowPlaying ->
                        NowPlayingBar(
                            nowPlaying = nowPlaying,
                            state = player,
                            onPlayPause = playerViewModel::togglePlayPause,
                            onNext = playerViewModel::next,
                            onPrevious = playerViewModel::previous,
                            onSeek = playerViewModel::seekTo,
                        )
                    }
                }
                BottomNavBar(selected = tab, onSelect = { scope.launch { pagerState.animateScrollToPage(it.ordinal) } })
            }
        },
        floatingActionButton = {
            if (tab == MainTab.EQ) EqImportFab(onImportViaWizard = onOpenWizard, onImportFile = { importFile.launch("text/plain") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize().padding(padding)) { page ->
            when (MainTab.entries[page]) {
                MainTab.LIBRARY -> LibraryPage(
                    state = library,
                    permissionDenied = permissionDenied,
                    onRequestPermission = permission.request,
                    onOpenAppSettings = permission.openAppSettings,
                    onRefresh = libraryViewModel::refresh,
                    onPlay = libraryViewModel::play,
                )
                MainTab.ALBUMS -> AlbumsPage(state = library, onPlay = libraryViewModel::play)
                MainTab.EQ -> EqPage(state = eq, onSelect = eqViewModel::select, onDelete = { eqViewModel.delete(it.id) })
            }
        }
    }

    (eq.message as? EqMessage.ImportFailed)?.let {
        MessageDialog(stringResource(R.string.eq_import_error_title), it.reason, onDismiss = eqViewModel::onMessageShown)
    }
    when (dialog) {
        MainDialog.SETTINGS -> SettingsDialog(onDismiss = { dialog = null })
        MainDialog.ABOUT -> AboutDialog(onDismiss = { dialog = null })
        null -> Unit
    }
}

@Composable
private fun BottomNavBar(selected: MainTab, onSelect: (MainTab) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 3.dp) {
        MainTab.entries.forEach { tab ->
            val label = stringResource(tab.label)
            NavigationBarItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                icon = { Icon(tab.icon, contentDescription = label) },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}
