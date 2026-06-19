package com.dyablo.openaudio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
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
                    title = { Text("OpenAudio") },
                    actions = {
                        state.nowPlaying?.let { Text("Playing: $it", modifier = Modifier.padding(end = 16.dp)) }
                    },
                )
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
                    LocalLibrary(tracks = state.localTracks, onPlay = viewModel::play)
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
                    )
                }
            }
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
                        Text(result.license, style = MaterialTheme.typography.labelSmall)
                    }
                    Row {
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
