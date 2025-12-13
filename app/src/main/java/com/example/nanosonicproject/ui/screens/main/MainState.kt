package com.example.nanosonicproject.ui.screens.main
//-------------------------
// UI State for Main Screen
//-------------------------
data class MainState(
    val selectedTab: MainTab = MainTab.LIBRARY
)

/**
 * Available tabs in bottom navigation
 */
enum class MainTab {
    LIBRARY,
    ALBUMS,
    PLAYLISTS,
    EQ
}