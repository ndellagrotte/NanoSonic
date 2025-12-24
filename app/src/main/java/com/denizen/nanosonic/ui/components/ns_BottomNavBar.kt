package com.denizen.nanosonic.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.denizen.nanosonic.ui.screens.main.MainTab

// ------------------------------
// Reusable Bottom Navigation Bar
// ------------------------------

@Composable
fun NanoSonicBottomNavigationBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp
    ) {
        // Library Tab
        NavigationBarItem(
            selected = selectedTab == MainTab.LIBRARY,
            onClick = { onTabSelected(MainTab.LIBRARY) },
            icon = {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Library"
                )
            },
            label = { Text("Library") },
            alwaysShowLabel = true,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        // Albums Tab
        NavigationBarItem(
            selected = selectedTab == MainTab.ALBUMS,
            onClick = { onTabSelected(MainTab.ALBUMS) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Album,
                    contentDescription = "Albums"
                )
            },
            label = { Text("Albums") },
            alwaysShowLabel = true,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        // EQ Tab
        NavigationBarItem(
            selected = selectedTab == MainTab.EQ,
            onClick = { onTabSelected(MainTab.EQ) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Equalizer,
                    contentDescription = "EQ"
                )
            },
            label = { Text("EQ") },
            alwaysShowLabel = true,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}