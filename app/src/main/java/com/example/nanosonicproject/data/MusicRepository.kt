package com.example.nanosonicproject.data

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface MusicRepository {
    suspend fun getAllTracks(): List<Track>

    /**
     * Retrieves a list of tracks based on a list of their IDs,
     * preserving the order of the provided IDs.
     *
     * @param trackIds The list of track IDs to fetch.
     * @return A list of [Track] objects matching the IDs.
     */
    suspend fun getTracksByIds(trackIds: List<String>): List<Track>
}

@Singleton
class MusicRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : MusicRepository {

    override suspend fun getAllTracks(): List<Track> {
        return withContext(Dispatchers.IO) {
            val tracks = mutableListOf<Track>()
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.SIZE
            )

            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
            val cursor: Cursor? = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use { c ->
                val idColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val filePathColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val dateAddedColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val sizeColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

                while (c.moveToNext()) {
                    val id = c.getLong(idColumn)
                    val albumId = c.getLong(albumIdColumn)
                    val artworkUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    ).toString()

                    tracks.add(
                        Track(
                            id = id.toString(),
                            title = c.getString(titleColumn),
                            artist = c.getString(artistColumn),
                            album = c.getString(albumColumn),
                            albumId = albumId.toString(),
                            duration = c.getLong(durationColumn),
                            filePath = c.getString(filePathColumn),
                            artworkUri = artworkUri,
                            dateAdded = c.getLong(dateAddedColumn),
                            size = c.getLong(sizeColumn),
                            albumArtUri = artworkUri // For compatibility
                        )
                    )
                }
            }
            tracks
        }
    }

    override suspend fun getTracksByIds(trackIds: List<String>): List<Track> = withContext(Dispatchers.IO) {
        // First, get all available tracks. In a real-world app, you might cache this.
        val allTracks = getAllTracks()

        // Create a map for efficient lookups (ID -> Track)
        val tracksByIdMap = allTracks.associateBy { it.id }

        // Map the incoming list of IDs to the actual Track objects.
        // `mapNotNull` will safely ignore any IDs that don't have a matching track
        // and preserve the order from the original trackIds list.
        trackIds.mapNotNull { id -> tracksByIdMap[id] }
    }
}
