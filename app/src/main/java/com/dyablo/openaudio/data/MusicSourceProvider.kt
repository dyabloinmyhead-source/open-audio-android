package com.dyablo.openaudio.data

interface MusicSourceProvider {
    val name: String
    suspend fun search(query: String): List<SearchResult>
}
