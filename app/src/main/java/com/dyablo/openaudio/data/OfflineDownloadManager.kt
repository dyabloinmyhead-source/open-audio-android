package com.dyablo.openaudio.data

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment

class OfflineDownloadManager(private val context: Context) {
    fun download(result: SearchResult): Long? {
        val url = result.downloadUrl ?: return null
        val fileName = buildString {
            append(result.artist.sanitizeFileName())
            append(" - ")
            append(result.title.sanitizeFileName())
            append(".mp3")
        }

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(result.title)
            .setDescription("${result.sourceName} - ${result.license}")
            .setMimeType(mimeTypeFor(url))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, "OpenAudio/$fileName")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)

        return context.getSystemService(DownloadManager::class.java).enqueue(request)
    }

    fun downloadTorrentFile(result: SearchResult): Long? {
        val url = result.torrentUrl ?: return null
        val fileName = "${result.artist.sanitizeFileName()} - ${result.title.sanitizeFileName()}.torrent"

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("${result.title}.torrent")
            .setDescription("${result.sourceName} torrent metadata")
            .setMimeType("application/x-bittorrent")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "OpenAudio/$fileName")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)

        return context.getSystemService(DownloadManager::class.java).enqueue(request)
    }
}

private fun mimeTypeFor(url: String): String = when {
    url.endsWith(".ogg", ignoreCase = true) -> "audio/ogg"
    url.endsWith(".flac", ignoreCase = true) -> "audio/flac"
    else -> "audio/mpeg"
}

private fun String.sanitizeFileName(): String =
    replace(Regex("[^A-Za-z0-9 ._'-]"), "_").take(80).ifBlank { "track" }
