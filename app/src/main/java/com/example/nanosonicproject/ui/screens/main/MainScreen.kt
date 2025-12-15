package com.example.nanosonicproject.ui.screens.main

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.media3.common.util.UnstableApi
import com.example.nanosonicproject.data.Track
import com.example.nanosonicproject.service.MusicPlayerViewModel
import com.example.nanosonicproject.ui.components.NanoSonicBottomNavigationBar
import com.example.nanosonicproject.ui.components.NowPlayingPanel
import com.example.nanosonicproject.ui.screens.albums.AlbumScreen
import com.example.nanosonicproject.ui.screens.eq.EqScreen
import com.example.nanosonicproject.ui.screens.library.LibraryScreen
import com.example.nanosonicproject.ui.theme.NanoSonicProjectTheme

/**
 * Main screen with bottom navigation
 * Contains 3 tabs: Library, Albums, EQ
 */

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(checkNotNull(LocalViewModelStoreOwner.current) {
                "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
            }, null),
    musicPlayerViewModel: MusicPlayerViewModel = hiltViewModel(),
    onNavigateToWizard: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val playbackState by musicPlayerViewModel.playbackState.collectAsStateWithLifecycle()

    MainScreenContent(
        selectedTab = state.selectedTab,
        onTabSelected = viewModel::onTabSelected,
        playbackState = playbackState,
        onPlayPauseClick = { musicPlayerViewModel.togglePlayPause() },
        onNextClick = { musicPlayerViewModel.next() },
        onPreviousClick = { musicPlayerViewModel.previous() },
        onSeek = { musicPlayerViewModel.seekTo(it) },
        onPlayTrack = { track, queue, mode -> musicPlayerViewModel.playTrack(track, queue, mode) },
        onNavigateToWizard = onNavigateToWizard
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MainScreenContent(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    playbackState: com.example.nanosonicproject.service.PlaybackState,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onPlayTrack: (Track, List<Track>, com.example.nanosonicproject.service.PlaybackMode) -> Unit,
    onNavigateToWizard: () -> Unit
) {
    // Create pager state with 3 pages (one for each tab)
    val pagerState = rememberPagerState(
        initialPage = selectedTab.ordinal,
        pageCount = { 3 }
    )

    // Sync pager state with selected tab
    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab.ordinal) {
            pagerState.animateScrollToPage(selectedTab.ordinal)
        }
    }

    // Sync selected tab with pager state
    // We only update the ViewModel when scrolling has finished to avoid
    // race conditions during animation (e.g. jumping from 0 to 2 passes through 1)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.isScrollInProgress to pagerState.currentPage }
            .collect { (isScrolling, page) ->
                if (!isScrolling) {
                    val newTab = MainTab.entries.getOrNull(page)
                    if (newTab != null && newTab != selectedTab) {
                        onTabSelected(newTab)
                    }
                }
            }
    }

    Scaffold(
        bottomBar = {
            // Column to stack NowPlayingPanel above BottomNavigationBar
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Now Playing Panel above the bottom nav
                NowPlayingPanel(
                    playbackState = playbackState,
                    onPlayPauseClick = onPlayPauseClick,
                    onNextClick = onNextClick,
                    onPreviousClick = onPreviousClick,
                    onSeek = onSeek
                )

                // Bottom Navigation Bar
                NanoSonicBottomNavigationBar(
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected
                )
            }
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { page ->
            when (page) {
                0 -> LibraryScreen(onPlayTrack = onPlayTrack)
                1 -> AlbumScreen(onPlayTrack = onPlayTrack)
                2 -> EqScreen(onNavigateToWizard = onNavigateToWizard)
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════════
// PREVIEW
// ═══════════════════════════════════════════════════════════════

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
    NanoSonicProjectTheme {
        MainScreenContent(
            selectedTab = MainTab.LIBRARY,
            onTabSelected = {},
            playbackState = com.example.nanosonicproject.service.PlaybackState(),
            onPlayPauseClick = {},
            onNextClick = {},
            onPreviousClick = {},
            onSeek = {},
            onPlayTrack = { _, _, _ -> },
            onNavigateToWizard = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenEQPreview() {
    NanoSonicProjectTheme {
        MainScreenContent(
            selectedTab = MainTab.EQ,
            onTabSelected = {},
            playbackState = com.example.nanosonicproject.service.PlaybackState(),
            onPlayPauseClick = {},
            onNextClick = {},
            onPreviousClick = {},
            onSeek = {},
            onPlayTrack = { _, _, _ -> },
            onNavigateToWizard = {}
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MainScreenPreviewDark() {
    NanoSonicProjectTheme {
        MainScreenContent(
            selectedTab = MainTab.LIBRARY,
            onTabSelected = {},
            playbackState = com.example.nanosonicproject.service.PlaybackState(),
            onPlayPauseClick = {},
            onNextClick = {},
            onPreviousClick = {},
            onSeek = {},
            onPlayTrack = { _, _, _ -> },
            onNavigateToWizard = {}
        )
    }
}
