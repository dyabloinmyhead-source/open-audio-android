package com.dyablo.openaudio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dyablo.openaudio.data.SearchResult
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
                TopAppBar(
                    title = {
                        Column {
                            Text("OpenAudio", maxLines = 1)
                            state.nowPlaying?.let {
                                Text(
                                    text = "Playing: $it",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    },
                )
            },
            bottomBar = {
                state.nowPlaying?.let {
                    MiniPlayer(
                        title = it,
                        isPlaying = state.isPlaying,
                        onPause = viewModel::pause,
                        onResume = viewModel::resume,
                    )
                }
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
                }

                if (selectedTab == 0) {
                    LocalLibrary(
                        tracks = state.pendingDownloads + state.localTracks,
                        onPlay = viewModel::play,
                    )
                } else {
                    OpenSearch(
                        query = state.query,
                        results = state.results,
                        isSearching = state.isSearching,
                        error = state.error,
                        onQueryChange = viewModel::setQuery,
                        onSearch = viewModel::search,
                        onPlay = viewModel::play,
                        onDownload = viewModel::saveOffline,
                        onOpenInfo = viewModel::openInfo,
                    )
                }
            }
        }

        state.selectedInfo?.let { result ->
            InfoDialog(result = result, onDismiss = viewModel::closeInfo)
        }
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

        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 12.dp)) {
            items(results) { result ->
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
                        if (result.isInfoOnly) {
                            AssistChip(
                                onClick = { onOpenInfo(result) },
                                label = { Text("Info") },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                            )
                        } else {
                            IconButton(onClick = { onPlay(result) }, enabled = result.streamUrl != null) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                            }
                            IconButton(onClick = { onDownload(result) }, enabled = result.downloadUrl != null) {
                                Icon(Icons.Default.Download, contentDescription = "Save offline")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniPlayer(
    title: String,
    isPlaying: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Now playing", style = MaterialTheme.typography.labelSmall)
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
        }
        IconButton(onClick = if (isPlaying) onPause else onResume) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Resume",
            )
        }
    }
}

@Composable
private fun InfoDialog(result: SearchResult, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(result.title, maxLines = 3, overflow = TextOverflow.Ellipsis)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.widthIn(max = 520.dp)) {
                Text("${result.artist} - ${result.sourceName}", style = MaterialTheme.typography.bodyMedium)
                result.metadata?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                Text(result.license, style = MaterialTheme.typography.bodySmall)
                result.infoUrl?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}
