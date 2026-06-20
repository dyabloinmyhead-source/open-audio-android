package com.dyablo.openaudio.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class YouTubeMusicProvider(
    private val apiKey: String,
    private val openOnly: Boolean,
    private val client: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    },
) : MusicSourceProvider {
    override val name: String = "YouTube Music"

    override suspend fun search(query: String): List<SearchResult> {
        if (query.isBlank() || apiKey.isBlank()) return emptyList()
        val response: YouTubeSearchResponse = client.get("https://www.googleapis.com/youtube/v3/search") {
            parameter("part", "snippet")
            parameter("type", "video")
            parameter("videoCategoryId", "10")
            parameter("maxResults", "15")
            parameter("q", query)
            parameter("key", apiKey)
            if (openOnly) parameter("videoLicense", "creativeCommon")
        }.body()

        return response.items.mapNotNull { item ->
            val videoId = item.id.videoId ?: return@mapNotNull null
            SearchResult(
                id = "youtube-$videoId",
                title = item.snippet.title,
                artist = item.snippet.channelTitle,
                license = if (openOnly) "Creative Commons on YouTube" else "YouTube standard terms",
                sourceName = name,
                streamUrl = null,
                downloadUrl = null,
                torrentUrl = null,
                infoUrl = "https://www.youtube.com/watch?v=$videoId",
                metadata = "Official YouTube Data API result",
                isInfoOnly = true,
                isVerifiedOpen = openOnly,
            )
        }
    }
}

@Serializable
private data class YouTubeSearchResponse(val items: List<YouTubeSearchItem> = emptyList())

@Serializable
private data class YouTubeSearchItem(
    val id: YouTubeSearchId = YouTubeSearchId(),
    val snippet: YouTubeSnippet = YouTubeSnippet(),
)

@Serializable
private data class YouTubeSearchId(val videoId: String? = null)

@Serializable
private data class YouTubeSnippet(
    val title: String = "Untitled",
    val channelTitle: String = "Unknown channel",
)
