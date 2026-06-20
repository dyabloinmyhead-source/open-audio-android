package com.dyablo.openaudio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dyablo.openaudio.data.AppSettings
import com.dyablo.openaudio.data.SearchResult
import com.dyablo.openaudio.data.SourceMode
import com.dyablo.openaudio.data.Track

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenAudioApp(viewModel: OpenAudioViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.loadLocal()
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                if (state.nowPlaying == null) {
                    TopAppBar(title = { Text("OpenAudio", maxLines = 1) })
                } else {
                    PlayerTopBar(
                        title = state.nowPlaying.orEmpty(),
                        isPlaying = state.isPlaying,
                        positionMs = state.positionMs,
                        durationMs = state.durationMs,
                        onPause = viewModel::pause,
                        onResume = viewModel::resume,
                        onSeek = viewModel::seekTo,
                        onRewind = viewModel::rewind,
                        onForward = viewModel::forward,
                        onStop = viewModel::stop,
                    )
                }
            },
            bottomBar = {
                state.activeInfo?.let { SwarmHealthBar(result = it) }
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Library") },
                        icon = { Icon(Icons.Default.LibraryMusic, contentDescription = null) },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Open Search") },
                        icon = { Icon(Icons.Default.Search, contentDescription = null) },
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Settings") },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    )
                }

                when (selectedTab) {
                    0 -> LocalLibrary(
                        tracks = state.pendingDownloads + state.localTracks,
                        onPlay = viewModel::play,
                    )

                    1 -> OpenSearch(
                            query = state.query,
                            searchHistory = state.searchHistory,
                            results = state.results,
                            isSearching = state.isSearching,
                            error = state.error,
                            onQueryChange = viewModel::setQuery,
                            onSearch = viewModel::search,
                            onPlay = viewModel::play,
                            onDownload = viewModel::saveOffline,
                            onOpenInfo = viewModel::openInfo,
                        )

                    else -> SettingsScreen(
                        settings = state.settings,
                        onInternetArchiveModeChange = viewModel::setInternetArchiveMode,
                        onRutrackerModeChange = viewModel::setRutrackerMode,
                        onYouTubeModeChange = viewModel::setYouTubeMode,
                        onVkModeChange = viewModel::setVkMode,
                        onYandexModeChange = viewModel::setYandexMusicMode,
                        onYouTubeApiKeyChange = viewModel::setYouTubeApiKey,
                        onVkAccessTokenChange = viewModel::setVkAccessToken,
                        onYandexAccessTokenChange = viewModel::setYandexAccessToken,
                        onMusicFolderChange = viewModel::setMusicFolder,
                        onTorrentFolderChange = viewModel::setTorrentFolder,
                    )
                }
            }
        }

        state.selectedInfo?.let { result ->
            InfoDialog(
                result = result,
                onDismiss = viewModel::closeInfo,
                onDownload = viewModel::saveOffline,
                onTorrentDownload = viewModel::saveTorrentFile,
                onTestQueue = viewModel::queueInfoDownloadTest,
            )
        }
    }
}

@Composable
private fun SettingsScreen(
    settings: AppSettings,
    onInternetArchiveModeChange: (SourceMode) -> Unit,
    onRutrackerModeChange: (SourceMode) -> Unit,
    onYouTubeModeChange: (SourceMode) -> Unit,
    onVkModeChange: (SourceMode) -> Unit,
    onYandexModeChange: (SourceMode) -> Unit,
    onYouTubeApiKeyChange: (String) -> Unit,
    onVkAccessTokenChange: (String) -> Unit,
    onYandexAccessTokenChange: (String) -> Unit,
    onMusicFolderChange: (String) -> Unit,
    onTorrentFolderChange: (String) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Sources", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            SourceSettings(
                title = "Internet Archive",
                subtitle = "Open audio and direct offline files",
                mode = settings.internetArchiveMode,
                onModeChange = onInternetArchiveModeChange,
                authLabel = "No authorization required",
            )
            SourceSettings(
                title = "RuTracker",
                subtitle = "Metadata only; no direct file access",
                mode = settings.rutrackerMode,
                onModeChange = onRutrackerModeChange,
                authLabel = "Public metadata access",
            )
            SourceSettings(
                title = "YouTube Music",
                subtitle = "Official Data API; Open filters Creative Commons",
                mode = settings.youtubeMode,
                onModeChange = onYouTubeModeChange,
                authLabel = if (settings.youtubeApiKey.isBlank()) "API key required" else "API key saved locally",
                credential = settings.youtubeApiKey,
                credentialLabel = "YouTube Data API key",
                onCredentialChange = onYouTubeApiKeyChange,
            )
            SourceSettings(
                title = "VK Music",
                subtitle = "Official catalog link; provider terms apply",
                mode = settings.vkMode,
                onModeChange = onVkModeChange,
                authLabel = if (settings.vkAccessToken.isBlank()) "Not authorized" else "Access token saved locally",
                credential = settings.vkAccessToken,
                credentialLabel = "VK access token",
                onCredentialChange = onVkAccessTokenChange,
            )
            SourceSettings(
                title = "Yandex Music",
                subtitle = "Official catalog link; provider terms apply",
                mode = settings.yandexMusicMode,
                onModeChange = onYandexModeChange,
                authLabel = if (settings.yandexAccessToken.isBlank()) "Not authorized" else "OAuth token saved locally",
                credential = settings.yandexAccessToken,
                credentialLabel = "Yandex OAuth token",
                onCredentialChange = onYandexAccessTokenChange,
            )

            Text(
                "Download folders",
                modifier = Modifier.padding(top = 22.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedTextField(
                value = settings.musicFolder,
                onValueChange = onMusicFolderChange,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                singleLine = true,
                label = { Text("Audio folder under Music") },
            )
            Text("Current path: Music/${settings.musicFolder}", style = MaterialTheme.typography.labelSmall)

            OutlinedTextField(
                value = settings.torrentFolder,
                onValueChange = onTorrentFolderChange,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                singleLine = true,
                label = { Text("Torrent folder under Downloads") },
            )
            Text("Current path: Downloads/${settings.torrentFolder}", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun SourceSettings(
    title: String,
    subtitle: String,
    mode: SourceMode,
    onModeChange: (SourceMode) -> Unit,
    authLabel: String,
    credential: String? = null,
    credentialLabel: String? = null,
    onCredentialChange: ((String) -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SourceMode.entries.forEach { option ->
                FilterChip(
                    selected = mode == option,
                    onClick = { onModeChange(option) },
                    label = { Text(option.name) },
                )
            }
        }
        Text(authLabel, style = MaterialTheme.typography.labelSmall)
        if (credential != null && credentialLabel != null && onCredentialChange != null) {
            OutlinedTextField(
                value = credential,
                onValueChange = onCredentialChange,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                singleLine = true,
                label = { Text(credentialLabel) },
                visualTransformation = PasswordVisualTransformation(),
            )
        }
        HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun LocalLibrary(tracks: List<Track>, onPlay: (Track) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(tracks) { track ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(track.title, fontWeight = FontWeight.SemiBold)
                    Text(track.artist, style = MaterialTheme.typography.bodySmall)
                    track.license?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
                }
                IconButton(onClick = { onPlay(track) }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                }
            }
        }
    }
}

@Composable
private fun OpenSearch(
    query: String,
    searchHistory: List<String>,
    results: List<SearchResult>,
    isSearching: Boolean,
    error: String?,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onPlay: (SearchResult) -> Unit,
    onDownload: (SearchResult) -> Unit,
    onOpenInfo: (SearchResult) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Search open music") },
            )
            Button(onClick = onSearch, enabled = !isSearching) {
                Icon(Icons.Default.Search, contentDescription = null)
                Text(if (isSearching) "Searching" else "Search")
            }
        }

        if (searchHistory.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(searchHistory) { historicalQuery ->
                    AssistChip(
                        onClick = { onQueryChange(historicalQuery) },
                        label = { Text(historicalQuery, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 12.dp)) {
            items(results) { result ->
                val canDownload = result.isVerifiedOpen && result.downloadUrl != null && !result.isInfoOnly
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(result.title, fontWeight = FontWeight.SemiBold)
                        Text("${result.artist} - ${result.sourceName}", style = MaterialTheme.typography.bodySmall)
                        result.metadata?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
                        Text(result.license, style = MaterialTheme.typography.labelSmall)
                    }
                    Row {
                        if (result.isInfoOnly || !result.isVerifiedOpen) {
                            AssistChip(
                                onClick = { onOpenInfo(result) },
                                label = { Text("Info") },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                            )
                        }
                        if (!result.isInfoOnly) {
                            IconButton(onClick = { onPlay(result) }, enabled = result.streamUrl != null) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                            }
                            if (canDownload) {
                                IconButton(onClick = { onDownload(result) }) {
                                    Icon(Icons.Default.Download, contentDescription = "Save offline")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerTopBar(
    title: String,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSeek: (Long) -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onStop: () -> Unit,
) {
    Surface(shadowElevation = 3.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("OpenAudio", style = MaterialTheme.typography.labelSmall)
                    Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onRewind) {
                        Icon(Icons.Default.Replay10, contentDescription = "Rewind 10 seconds")
                    }
                    IconButton(onClick = if (isPlaying) onPause else onResume) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Resume",
                        )
                    }
                    IconButton(onClick = onForward) {
                        Icon(Icons.Default.Forward30, contentDescription = "Forward 30 seconds")
                    }
                    IconButton(onClick = onStop) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop")
                    }
                }
            }

            Slider(
                value = positionMs.coerceIn(0L, durationMs.coerceAtLeast(0L)).toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
                enabled = durationMs > 0L,
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(positionMs), style = MaterialTheme.typography.labelSmall)
                Text(formatTime(durationMs), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun SwarmHealthBar(result: SearchResult) {
    val seeds = result.seeds ?: 0
    val leeches = result.leeches ?: 0
    val total = (seeds + leeches).coerceAtLeast(1)
    val health = seeds.toFloat() / total.toFloat()

    Surface(shadowElevation = 4.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Swarm health", style = MaterialTheme.typography.labelSmall)
                    Text(result.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                }
                Text("S $seeds / L $leeches", style = MaterialTheme.typography.labelSmall)
            }
            LinearProgressIndicator(
                progress = { health },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

@Composable
private fun InfoDialog(
    result: SearchResult,
    onDismiss: () -> Unit,
    onDownload: (SearchResult) -> Unit,
    onTorrentDownload: (SearchResult) -> Unit,
    onTestQueue: (SearchResult) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val canDownload = result.isVerifiedOpen && !result.isInfoOnly && result.downloadUrl != null
    val canDownloadTorrent = result.isVerifiedOpen && !result.isInfoOnly && result.torrentUrl != null
    val canTestQueue = result.seeds != null || result.leeches != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(result.title, maxLines = 3, overflow = TextOverflow.Ellipsis)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.widthIn(max = 520.dp)) {
                Text("${result.artist} - ${result.sourceName}", style = MaterialTheme.typography.bodyMedium)
                result.metadata?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                Text("Seeds: ${result.seeds ?: 0}  Leeches: ${result.leeches ?: 0}", style = MaterialTheme.typography.bodySmall)
                Text("Verified-open: ${if (result.isVerifiedOpen) "yes" else "no"}", style = MaterialTheme.typography.bodySmall)
                Text(result.license, style = MaterialTheme.typography.bodySmall)
                if (canTestQueue && !canDownload && !canDownloadTorrent) {
                    Text("Test queue updates the app UI without downloading torrent files.", style = MaterialTheme.typography.labelSmall)
                }
                result.infoUrl?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canDownloadTorrent) {
                    Button(onClick = { onTorrentDownload(result) }) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Text("Save torrent")
                    }
                }
                when {
                    canDownload -> Button(onClick = { onDownload(result) }) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Text("Save offline")
                    }
                    canTestQueue -> Button(onClick = { onTestQueue(result) }) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Text("Test queue")
                    }
                    result.infoUrl != null -> Button(onClick = { uriHandler.openUri(result.infoUrl) }) {
                        Text("Open source")
                    }
                }
            }
        },
    )
}
