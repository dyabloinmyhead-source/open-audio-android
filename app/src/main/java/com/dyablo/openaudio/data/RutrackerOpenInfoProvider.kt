package com.dyablo.openaudio.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.encodeURLParameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class RutrackerOpenInfoProvider(
    private val client: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    },
) : MusicSourceProvider {
    override val name: String = "RuTracker Open Info"

    override suspend fun search(query: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        val encoded = query.encodeURLParameter()
        val url = "https://torapi.vercel.app/api/search/title/rutracker?query=$encoded"
        val response: List<RutrackerInfoItem> = client.get(url).body()

        return response.take(25).map { item ->
            val seeds = item.seeds.toIntOrNull()
            val peers = item.peers.toIntOrNull()
            val metadata = listOfNotNull(
                item.size?.takeIf { it.isNotBlank() }?.let { "Size $it" },
                seeds?.let { "Seeds $it" },
                peers?.let { "Leeches $it" },
                item.date?.takeIf { it.isNotBlank() }?.let { "Date $it" },
            ).joinToString("  |  ")

            SearchResult(
                id = item.id,
                title = item.name,
                artist = item.category.ifBlank { "Public index metadata" },
                license = "Info only - verify rights before use",
                sourceName = name,
                streamUrl = null,
                downloadUrl = null,
                torrentUrl = null,
                infoUrl = item.url,
                metadata = metadata.ifBlank { null },
                isInfoOnly = true,
                seeds = seeds,
                leeches = peers,
            )
        }
    }
}

@Serializable
private data class RutrackerInfoItem(
    @SerialName("Name") val name: String = "Untitled",
    @SerialName("Id") val id: String = "",
    @SerialName("Url") val url: String? = null,
    @SerialName("Size") val size: String? = null,
    @SerialName("Category") val category: String = "",
    @SerialName("Seeds") val seeds: String? = null,
    @SerialName("Peers") val peers: String? = null,
    @SerialName("Date") val date: String? = null,
)
