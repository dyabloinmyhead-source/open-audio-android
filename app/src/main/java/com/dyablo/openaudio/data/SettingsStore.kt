package com.dyablo.openaudio.data

import android.content.Context

enum class SourceMode {
    Off,
    Search,
    Open,
}

data class AppSettings(
    val internetArchiveMode: SourceMode = SourceMode.Open,
    val rutrackerMode: SourceMode = SourceMode.Search,
    val youtubeMode: SourceMode = SourceMode.Off,
    val vkMode: SourceMode = SourceMode.Off,
    val yandexMusicMode: SourceMode = SourceMode.Off,
    val youtubeApiKey: String = "",
    val vkAccessToken: String = "",
    val yandexAccessToken: String = "",
    val musicFolder: String = "OpenAudio",
    val torrentFolder: String = "OpenAudio",
)

class SettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("open_audio_settings", Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        internetArchiveMode = loadMode(
            KEY_INTERNET_ARCHIVE_MODE,
            legacyEnabled = preferences.getBoolean(KEY_INTERNET_ARCHIVE_ENABLED, true),
            legacyOpen = preferences.getBoolean(KEY_INTERNET_ARCHIVE_OPEN, true),
        ),
        rutrackerMode = loadMode(
            KEY_RUTRACKER_MODE,
            legacyEnabled = preferences.getBoolean(KEY_RUTRACKER_INFO, true),
            legacyOpen = preferences.getBoolean(KEY_RUTRACKER_OPEN, false),
        ),
        youtubeMode = loadMode(KEY_YOUTUBE_MODE, false, false),
        vkMode = loadMode(KEY_VK_MODE, false, false),
        yandexMusicMode = loadMode(KEY_YANDEX_MODE, false, false),
        youtubeApiKey = preferences.getString(KEY_YOUTUBE_API_KEY, "").orEmpty(),
        vkAccessToken = preferences.getString(KEY_VK_TOKEN, "").orEmpty(),
        yandexAccessToken = preferences.getString(KEY_YANDEX_TOKEN, "").orEmpty(),
        musicFolder = preferences.getString(KEY_MUSIC_FOLDER, "OpenAudio").orEmpty().ifBlank { "OpenAudio" },
        torrentFolder = preferences.getString(KEY_TORRENT_FOLDER, "OpenAudio").orEmpty().ifBlank { "OpenAudio" },
    )

    fun save(settings: AppSettings) {
        preferences.edit()
            .putString(KEY_INTERNET_ARCHIVE_MODE, settings.internetArchiveMode.name)
            .putString(KEY_RUTRACKER_MODE, settings.rutrackerMode.name)
            .putString(KEY_YOUTUBE_MODE, settings.youtubeMode.name)
            .putString(KEY_VK_MODE, settings.vkMode.name)
            .putString(KEY_YANDEX_MODE, settings.yandexMusicMode.name)
            .putString(KEY_YOUTUBE_API_KEY, settings.youtubeApiKey.trim())
            .putString(KEY_VK_TOKEN, settings.vkAccessToken.trim())
            .putString(KEY_YANDEX_TOKEN, settings.yandexAccessToken.trim())
            .putString(KEY_MUSIC_FOLDER, settings.musicFolder.cleanFolderName())
            .putString(KEY_TORRENT_FOLDER, settings.torrentFolder.cleanFolderName())
            .apply()
    }

    private fun loadMode(key: String, legacyEnabled: Boolean, legacyOpen: Boolean): SourceMode {
        val saved = preferences.getString(key, null)
        return saved?.let { runCatching { SourceMode.valueOf(it) }.getOrNull() }
            ?: when {
                !legacyEnabled -> SourceMode.Off
                legacyOpen -> SourceMode.Open
                else -> SourceMode.Search
            }
    }

    private companion object {
        const val KEY_INTERNET_ARCHIVE_MODE = "internet_archive_mode"
        const val KEY_RUTRACKER_MODE = "rutracker_mode"
        const val KEY_YOUTUBE_MODE = "youtube_mode"
        const val KEY_VK_MODE = "vk_mode"
        const val KEY_YANDEX_MODE = "yandex_music_mode"
        const val KEY_YOUTUBE_API_KEY = "youtube_api_key"
        const val KEY_VK_TOKEN = "vk_access_token"
        const val KEY_YANDEX_TOKEN = "yandex_access_token"
        const val KEY_INTERNET_ARCHIVE_ENABLED = "internet_archive_enabled"
        const val KEY_INTERNET_ARCHIVE_OPEN = "internet_archive_verified_open"
        const val KEY_RUTRACKER_INFO = "rutracker_info_enabled"
        const val KEY_RUTRACKER_OPEN = "rutracker_verified_open"
        const val KEY_MUSIC_FOLDER = "music_folder"
        const val KEY_TORRENT_FOLDER = "torrent_folder"
    }
}

fun String.cleanFolderName(): String =
    replace(Regex("[^A-Za-z0-9 ._/-]"), "_")
        .trim('/', ' ', '.')
        .ifBlank { "OpenAudio" }
