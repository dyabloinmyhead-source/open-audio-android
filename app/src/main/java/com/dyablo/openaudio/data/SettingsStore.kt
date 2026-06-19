package com.dyablo.openaudio.data

import android.content.Context

data class AppSettings(
    val internetArchiveEnabled: Boolean = true,
    val internetArchiveVerifiedOpen: Boolean = true,
    val rutrackerInfoEnabled: Boolean = true,
    val rutrackerVerifiedOpen: Boolean = false,
    val musicFolder: String = "OpenAudio",
    val torrentFolder: String = "OpenAudio",
)

class SettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("open_audio_settings", Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        internetArchiveEnabled = preferences.getBoolean(KEY_INTERNET_ARCHIVE_ENABLED, true),
        internetArchiveVerifiedOpen = preferences.getBoolean(KEY_INTERNET_ARCHIVE, true),
        rutrackerInfoEnabled = preferences.getBoolean(KEY_RUTRACKER_INFO, true),
        rutrackerVerifiedOpen = preferences.getBoolean(KEY_RUTRACKER_VERIFIED_OPEN, false),
        musicFolder = preferences.getString(KEY_MUSIC_FOLDER, "OpenAudio").orEmpty().ifBlank { "OpenAudio" },
        torrentFolder = preferences.getString(KEY_TORRENT_FOLDER, "OpenAudio").orEmpty().ifBlank { "OpenAudio" },
    )

    fun save(settings: AppSettings) {
        preferences.edit()
            .putBoolean(KEY_INTERNET_ARCHIVE_ENABLED, settings.internetArchiveEnabled)
            .putBoolean(KEY_INTERNET_ARCHIVE, settings.internetArchiveVerifiedOpen)
            .putBoolean(KEY_RUTRACKER_INFO, settings.rutrackerInfoEnabled)
            .putBoolean(KEY_RUTRACKER_VERIFIED_OPEN, settings.rutrackerVerifiedOpen)
            .putString(KEY_MUSIC_FOLDER, settings.musicFolder.cleanFolderName())
            .putString(KEY_TORRENT_FOLDER, settings.torrentFolder.cleanFolderName())
            .apply()
    }

    private companion object {
        const val KEY_INTERNET_ARCHIVE_ENABLED = "internet_archive_enabled"
        const val KEY_INTERNET_ARCHIVE = "internet_archive_verified_open"
        const val KEY_RUTRACKER_INFO = "rutracker_info_enabled"
        const val KEY_RUTRACKER_VERIFIED_OPEN = "rutracker_verified_open"
        const val KEY_MUSIC_FOLDER = "music_folder"
        const val KEY_TORRENT_FOLDER = "torrent_folder"
    }
}

fun String.cleanFolderName(): String =
    replace(Regex("[^A-Za-z0-9 ._/-]"), "_")
        .trim('/', ' ', '.')
        .ifBlank { "OpenAudio" }
