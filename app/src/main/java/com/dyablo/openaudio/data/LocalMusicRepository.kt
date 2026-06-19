package com.dyablo.openaudio.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore

class LocalMusicRepository(private val context: Context) {
    fun loadTracks(): List<Track> {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
        )
        val sort = "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        val tracks = mutableListOf<Track>()

        context.contentResolver.query(collection, projection, null, null, sort)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val uri = ContentUris.withAppendedId(collection, id)
                tracks += Track(
                    id = id.toString(),
                    title = cursor.getString(titleColumn) ?: "Untitled",
                    artist = cursor.getString(artistColumn) ?: "Unknown artist",
                    album = cursor.getString(albumColumn),
                    uri = uri,
                    source = TrackSource.Local,
                )
            }
        }

        return tracks
    }
}
