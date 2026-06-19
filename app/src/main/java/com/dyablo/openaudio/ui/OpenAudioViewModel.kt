package com.dyablo.openaudio.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dyablo.openaudio.data.InternetArchiveProvider
import com.dyablo.openaudio.data.LocalMusicRepository
import com.dyablo.openaudio.data.OfflineDownloadManager
import com.dyablo.openaudio.data.SearchResult
import com.dyablo.openaudio.data.Track
import com.dyablo.openaudio.data.TrackSource
import com.dyablo.openaudio.playback.AudioPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OpenAudioViewModel(application: Application) : AndroidViewModel(application) {
    private val localMusicRepository = LocalMusicRepository(application)
    private val sourceProvider = InternetArchiveProvider()
    private val downloadManager = OfflineDownloadManager(application)
    private val player = AudioPlayer(application)

    private val _state = MutableStateFlow(OpenAudioState())
    val state: StateFlow<OpenAudioState> = _state

    fun loadLocal() {
        _state.update { it.copy(localTracks = localMusicRepository.loadTracks()) }
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query) }
    }

    fun search() {
        val query = state.value.query
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true, error = null) }
            runCatching { sourceProvider.search(query) }
                .onSuccess { results -> _state.update { it.copy(results = results, isSearching = false) } }
                .onFailure { error -> _state.update { it.copy(error = error.message, isSearching = false) } }
        }
    }

    fun play(track: Track) {
        player.play(track)
        _state.update { it.copy(nowPlaying = track.title) }
    }

    fun play(result: SearchResult) {
        val url = result.streamUrl ?: return
        player.playUrl(url)
        _state.update { it.copy(nowPlaying = result.title) }
    }

    fun saveOffline(result: SearchResult) {
        val id = downloadManager.download(result)
        _state.update {
            it.copy(
                lastDownloadId = id,
                savedPreviewTrack = result.downloadUrl?.let { url ->
                    Track(
                        id = result.id,
                        title = result.title,
                        artist = result.artist,
                        uri = Uri.parse(url),
                        source = TrackSource.OpenCatalog,
                        license = result.license,
                    )
                },
            )
        }
    }

    override fun onCleared() {
        player.release()
    }
}

data class OpenAudioState(
    val query: String = "",
    val localTracks: List<Track> = emptyList(),
    val results: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val nowPlaying: String? = null,
    val lastDownloadId: Long? = null,
    val savedPreviewTrack: Track? = null,
    val error: String? = null,
)
