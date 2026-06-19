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

class InternetArchiveProvider(
    private val client: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    },
) : MusicSourceProvider {
    override val name: String = "Internet Archive"

    override suspend fun search(query: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        val encoded = query.encodeURLParameter()
        val url = "https://archive.org/advancedsearch.php?q=$encoded%20AND%20mediatype:audio&fl[]=identifier&fl[]=title&fl[]=creator&fl[]=licenseurl&rows=20&output=json"
        val response: ArchiveSearchResponse = client.get(url).body()

        return response.response.docs.mapNotNull { doc ->
            val identifier = doc.identifier ?: return@mapNotNull null
            val title = doc.title ?: identifier
            val artist = doc.creator ?: "Unknown artist"
            val metadataUrl = "https://archive.org/metadata/$identifier"
            val files = runCatching { client.get(metadataUrl).body<ArchiveMetadata>().files }.getOrDefault(emptyList())
            val audioFile = files.firstOrNull { it.name.endsWith(".mp3", ignoreCase = true) }
                ?: files.firstOrNull { it.name.endsWith(".ogg", ignoreCase = true) }

            val downloadUrl = audioFile?.name?.let { fileName ->
                "https://archive.org/download/$identifier/${fileName.encodeURLParameter()}"
            }

            SearchResult(
                id = identifier,
                title = title,
                artist = artist,
                license = doc.licenseUrl ?: "Check source license",
                sourceName = name,
                streamUrl = downloadUrl,
                downloadUrl = downloadUrl,
                torrentUrl = "https://archive.org/download/$identifier/${identifier}_archive.torrent",
                isVerifiedOpen = true,
            )
        }
    }
}

@Serializable
private data class ArchiveSearchResponse(
    val response: ArchiveResponse = ArchiveResponse(),
)

@Serializable
private data class ArchiveResponse(
    val docs: List<ArchiveDoc> = emptyList(),
)

@Serializable
private data class ArchiveDoc(
    val identifier: String? = null,
    val title: String? = null,
    val creator: String? = null,
    @SerialName("licenseurl") val licenseUrl: String? = null,
)

@Serializable
private data class ArchiveMetadata(
    val files: List<ArchiveFile> = emptyList(),
)

@Serializable
private data class ArchiveFile(
    val name: String = "",
)
