package com.dyablo.openaudio.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dyablo.openaudio.data.AppSettings
import com.dyablo.openaudio.data.InternetArchiveProvider
import com.dyablo.openaudio.data.LocalMusicRepository
import com.dyablo.openaudio.data.MusicSourceProvider
import com.dyablo.openaudio.data.OfflineDownloadManager
import com.dyablo.openaudio.data.RutrackerOpenInfoProvider
import com.dyablo.openaudio.data.SearchResult
import com.dyablo.openaudio.data.SearchHistoryStore
import com.dyablo.openaudio.data.SettingsStore
import com.dyablo.openaudio.data.Track
import com.dyablo.openaudio.data.TrackSource
import com.dyablo.openaudio.playback.AudioPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OpenAudioViewModel(application: Application) : AndroidViewModel(application) {
    private val localMusicRepository = LocalMusicRepository(application)
    private val internetArchiveProvider = InternetArchiveProvider()
    private val rutrackerOpenInfoProvider = RutrackerOpenInfoProvider()
    private val downloadManager = OfflineDownloadManager(application)
    private val searchHistoryStore = SearchHistoryStore(application)
    private val settingsStore = SettingsStore(application)
    private val player = AudioPlayer(application)
    private var progressJob: Job? = null

    private val _state = MutableStateFlow(
        OpenAudioState(
            searchHistory = searchHistoryStore.load(),
            settings = settingsStore.load(),
        ),
    )
    val state: StateFlow<OpenAudioState> = _state

    fun loadLocal() {
        _state.update { it.copy(localTracks = localMusicRepository.loadTracks()) }
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query) }
    }

    fun search() {
        val query = state.value.query
        val settings = state.value.settings
        val sourceProviders: List<MusicSourceProvider> = listOfNotNull(
            internetArchiveProvider.takeIf { settings.internetArchiveVerifiedOpen },
            rutrackerOpenInfoProvider.takeIf { settings.rutrackerInfoEnabled },
        )
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true, error = null) }
            runCatching { sourceProviders.flatMap { provider -> provider.search(query) } }
                .onSuccess { results ->
                    _state.update {
                        it.copy(
                            results = results,
                            isSearching = false,
                            searchHistory = searchHistoryStore.save(query),
                        )
                    }
                }
                .onFailure { error -> _state.update { it.copy(error = error.message, isSearching = false) } }
        }
    }

    fun play(track: Track) {
        player.play(track)
        _state.update { it.copy(nowPlaying = track.title, isPlaying = true) }
        startProgressUpdates()
    }

    fun play(result: SearchResult) {
        val url = result.streamUrl ?: return
        player.playUrl(url)
        _state.update { it.copy(nowPlaying = result.title, isPlaying = true, activeInfo = result.takeIf { it.isInfoOnly }) }
        startProgressUpdates()
    }

    fun pause() {
        player.pause()
        _state.update { it.copy(isPlaying = false) }
    }

    fun resume() {
        player.resume()
        _state.update { it.copy(isPlaying = true) }
        startProgressUpdates()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _state.update { it.copy(positionMs = player.currentPositionMs(), durationMs = player.durationMs()) }
    }

    fun rewind() {
        player.seekBy(-10_000L)
        _state.update { it.copy(positionMs = player.currentPositionMs(), durationMs = player.durationMs()) }
    }

    fun forward() {
        player.seekBy(30_000L)
        _state.update { it.copy(positionMs = player.currentPositionMs(), durationMs = player.durationMs()) }
    }

    fun stop() {
        player.stop()
        _state.update { it.copy(isPlaying = false, positionMs = 0L) }
    }

    fun saveOffline(result: SearchResult) {
        val id = downloadManager.download(result, state.value.settings.musicFolder)
        val pendingTrack = result.downloadUrl?.let { url ->
            Track(
                id = "download-${result.id}",
                title = result.title,
                artist = result.artist,
                uri = Uri.parse(url),
                source = TrackSource.OpenCatalog,
                license = "Saved / pending - ${result.license}",
            )
        }
        _state.update {
            it.copy(
                lastDownloadId = id,
                savedPreviewTrack = pendingTrack,
                pendingDownloads = pendingTrack?.let { track -> (it.pendingDownloads + track).distinctBy(Track::id) }
                    ?: it.pendingDownloads,
            )
        }
    }

    fun saveTorrentFile(result: SearchResult) {
        val id = downloadManager.downloadTorrentFile(result, state.value.settings.torrentFolder)
        val pendingTrack = result.torrentUrl?.let { url ->
            Track(
                id = "torrent-file-${result.id}",
                title = "${result.title}.torrent",
                artist = result.artist,
                uri = Uri.parse(url),
                source = TrackSource.VerifiedOpenTorrent,
                license = "Torrent file download pending - ${result.license}",
            )
        }
        _state.update {
            it.copy(
                lastDownloadId = id,
                pendingDownloads = pendingTrack?.let { track -> (it.pendingDownloads + track).distinctBy(Track::id) }
                    ?: it.pendingDownloads,
                activeInfo = result,
            )
        }
    }

    fun queueInfoDownloadTest(result: SearchResult) {
        val testTrack = Track(
            id = "torrent-test-${result.id}",
            title = result.title,
            artist = result.artist,
            uri = Uri.parse(result.infoUrl ?: result.id),
            source = TrackSource.VerifiedOpenTorrent,
            license = "Torrent test queue - no file downloaded",
        )
        _state.update {
            it.copy(
                pendingDownloads = (it.pendingDownloads + testTrack).distinctBy(Track::id),
                activeInfo = result,
            )
        }
    }

    fun openInfo(result: SearchResult) {
        _state.update { it.copy(selectedInfo = result, activeInfo = result) }
    }

    fun closeInfo() {
        _state.update { it.copy(selectedInfo = null) }
    }

    fun setInternetArchiveVerifiedOpen(enabled: Boolean) {
        updateSettings { it.copy(internetArchiveVerifiedOpen = enabled) }
    }

    fun setRutrackerInfoEnabled(enabled: Boolean) {
        updateSettings { it.copy(rutrackerInfoEnabled = enabled) }
    }

    fun setMusicFolder(folder: String) {
        updateSettings { it.copy(musicFolder = folder) }
    }

    fun setTorrentFolder(folder: String) {
        updateSettings { it.copy(torrentFolder = folder) }
    }

    override fun onCleared() {
        progressJob?.cancel()
        player.release()
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                _state.update {
                    it.copy(
                        positionMs = player.currentPositionMs(),
                        durationMs = player.durationMs(),
                    )
                }
                delay(750)
            }
        }
    }

    private fun updateSettings(reducer: (AppSettings) -> AppSettings) {
        val next = reducer(state.value.settings)
        settingsStore.save(next)
        _state.update { it.copy(settings = settingsStore.load()) }
    }
}

data class OpenAudioState(
    val query: String = "",
    val searchHistory: List<String> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val localTracks: List<Track> = emptyList(),
    val results: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val nowPlaying: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val lastDownloadId: Long? = null,
    val savedPreviewTrack: Track? = null,
    val pendingDownloads: List<Track> = emptyList(),
    val selectedInfo: SearchResult? = null,
    val activeInfo: SearchResult? = null,
    val error: String? = null,
)
