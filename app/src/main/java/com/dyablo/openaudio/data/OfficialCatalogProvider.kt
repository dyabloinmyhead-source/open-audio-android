package com.dyablo.openaudio.data

import io.ktor.http.encodeURLParameter

class OfficialCatalogProvider(
    override val name: String,
    private val searchUrl: (String) -> String,
    private val authorized: Boolean,
) : MusicSourceProvider {
    override suspend fun search(query: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        val encoded = query.encodeURLParameter()
        return listOf(
            SearchResult(
                id = "${name.lowercase().replace(' ', '-')}:$query",
                title = "Search for $query",
                artist = name,
                license = "Playback and access follow $name terms",
                sourceName = name,
                streamUrl = null,
                downloadUrl = null,
                torrentUrl = null,
                infoUrl = searchUrl(encoded),
                metadata = if (authorized) "Authorization saved locally" else "Open official catalog",
                isInfoOnly = true,
                isVerifiedOpen = false,
            ),
        )
    }
}
