package com.dyablo.openaudio.data

import android.net.Uri

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val uri: Uri,
    val source: TrackSource,
    val license: String? = null,
)

enum class TrackSource {
    Local,
    OpenCatalog,
    VerifiedOpenTorrent,
}

data class SearchResult(
    val id: String,
    val title: String,
    val artist: String,
    val license: String,
    val sourceName: String,
    val streamUrl: String?,
    val downloadUrl: String?,
    val torrentUrl: String?,
    val infoUrl: String? = null,
    val metadata: String? = null,
    val isInfoOnly: Boolean = false,
)
