package com.kiroku.app.core.model

import java.security.MessageDigest
import java.util.Locale

enum class SeriesTypeFilter(
    val apiValue: String,
) {
    MANGA("Manga"),
    MANHWA("Manhwa"),
    MANHUA("Manhua"),
    NOVEL("Novel"),
}

enum class SearchFilter(
    val apiValue: String,
) {
    COMPLETED("completed"),
    SOME_RELEASES("some_releases"),
}

data class SearchSpec(
    val query: String,
    val types: Set<SeriesTypeFilter> = emptySet(),
    val filters: Set<SearchFilter> = emptySet(),
) {
    val normalizedQuery: String = query.trim()

    val cacheKey: String by lazy(LazyThreadSafetyMode.NONE) {
        val canonical =
            buildString {
                append(normalizedQuery.lowercase(Locale.ROOT))
                append('\u001f')
                append(types.map(SeriesTypeFilter::apiValue).sorted().joinToString("\u001e"))
                append('\u001f')
                append(filters.map(SearchFilter::apiValue).sorted().joinToString("\u001e"))
            }
        MessageDigest
            .getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(Locale.ROOT, byte) }
    }

    val isSearchable: Boolean
        get() = normalizedQuery.length >= MIN_QUERY_LENGTH

    companion object {
        const val MIN_QUERY_LENGTH = 2
    }
}
