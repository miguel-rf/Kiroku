package com.kiroku.app.core.model

data class CoverImage(
    val originalUrl: String?,
    val thumbnailUrl: String?,
    val width: Int?,
    val height: Int?,
)

data class SeriesSummary(
    val id: Long,
    val title: String,
    val url: String?,
    val description: String?,
    val cover: CoverImage?,
    val type: String?,
    val year: String?,
    val rating: Double?,
    val ratingVotes: Int?,
    val genres: List<String>,
    val latestChapter: Int?,
    val lastUpdatedEpochSeconds: Long?,
)

data class Contributor(
    val id: Long?,
    val name: String,
    val role: String,
    val url: String?,
)

data class Publisher(
    val id: Long?,
    val name: String,
    val type: String,
    val notes: String?,
    val url: String?,
)

data class SeriesDetails(
    val summary: SeriesSummary,
    val alternativeTitles: List<String>,
    val categories: List<String>,
    val contributors: List<Contributor>,
    val publishers: List<Publisher>,
    val status: String?,
    val licensed: Boolean?,
    val completed: Boolean?,
    val isFullDetailCached: Boolean,
)

data class RecentSearch(
    val query: String,
    val searchedAtEpochMillis: Long,
)
