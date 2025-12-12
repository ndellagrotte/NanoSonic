package com.example.nanosonicproject.ui.screens.main

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nanosonicproject.ui.theme.NanoSonicProjectTheme
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.example.nanosonicproject.ui.components.NanoSonicBottomNavigationBar
import com.example.nanosonicproject.ui.components.NowPlayingPanel
import com.example.nanosonicproject.ui.screens.albums.AlbumScreen
import com.example.nanosonicproject.ui.screens.library.LibraryScreen
import com.example.nanosonicproject.ui.screens.library.Track
import com.example.nanosonicproject.ui.screens.eq.EQScreen
import com.example.nanosonicproject.service.MusicPlayerViewModel

/**
 * Main screen with bottom navigation
 * Contains 4 tabs: Library, Albums, Playlists, EQ
 */

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
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
        onPlayTrack = { track, playlist -> musicPlayerViewModel.playTrack(track, playlist) },
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
    onPlayTrack: (Track, List<Track>) -> Unit,
    onNavigateToWizard: () -> Unit
) {
    // Create pager state with 4 pages (one for each tab)
    val pagerState = rememberPagerState(
        initialPage = selectedTab.ordinal,
        pageCount = { 4 }
    )
    val coroutineScope = rememberCoroutineScope()

    // Sync pager state with selected tab
    LaunchedEffect(selectedTab) {
        pagerState.animateScrollToPage(selectedTab.ordinal)
    }

    // Sync selected tab with pager state
    LaunchedEffect(pagerState.currentPage) {
        val newTab = MainTab.entries[pagerState.currentPage]
        if (newTab != selectedTab) {
            onTabSelected(newTab)
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
                    onTabSelected = { tab ->
                        onTabSelected(tab)
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(tab.ordinal)
                        }
                    }
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
                2 -> PlaylistsContent()
                3 -> EQScreen(onNavigateToWizard = onNavigateToWizard)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PLACEHOLDER CONTENT SCREENS (Replace with actual implementations)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PlaylistsContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Playlists Screen",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Your playlists will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
            onPlayTrack = { _, _ -> },
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
            onPlayTrack = { _, _ -> },
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
            onPlayTrack = { _, _ -> },
            onNavigateToWizard = {}
        )
    }
}