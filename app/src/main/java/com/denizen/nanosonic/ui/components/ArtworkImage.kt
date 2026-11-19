package com.denizen.nanosonic.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Album art with a music-note placeholder. The placeholder sits underneath the image, so it shows
 * while loading and when there is no artwork.
 */
@Composable
fun ArtworkImage(
    uri: Uri?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small,
    placeholderColor: Color = MaterialTheme.colorScheme.primaryContainer,
    placeholderIconColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    placeholderIconSize: Dp = 28.dp,
) {
    Box(
        modifier = modifier.clip(shape).background(placeholderColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = placeholderIconColor,
            modifier = Modifier.size(placeholderIconSize),
        )
        if (uri != null) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}
