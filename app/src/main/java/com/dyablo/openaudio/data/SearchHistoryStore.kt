package com.dyablo.openaudio.data

import android.content.Context

class SearchHistoryStore(context: Context) {
    private val preferences = context.getSharedPreferences("open_audio_search", Context.MODE_PRIVATE)

    fun load(): List<String> =
        preferences.getString(KEY_QUERIES, null)
            ?.split(SEPARATOR)
            ?.map(String::trim)
            ?.filter(String::isNotBlank)
            .orEmpty()

    fun save(query: String): List<String> {
        val normalized = query.trim()
        if (normalized.isBlank()) return load()

        val next = (listOf(normalized) + load().filterNot { it.equals(normalized, ignoreCase = true) })
            .take(MAX_HISTORY)
        preferences.edit().putString(KEY_QUERIES, next.joinToString(SEPARATOR)).apply()
        return next
    }

    private companion object {
        const val KEY_QUERIES = "queries"
        const val SEPARATOR = "\n"
        const val MAX_HISTORY = 12
    }
}
