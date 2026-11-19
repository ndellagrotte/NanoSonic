package com.denizen.nanosonic.data.library

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.core.net.toUri

object MediaStoreUris {
    private val ALBUM_ART = "content://media/external/audio/albumart".toUri()

    fun audio(trackId: Long): Uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, trackId)

    fun albumArt(albumId: Long): Uri = ContentUris.withAppendedId(ALBUM_ART, albumId)
}
