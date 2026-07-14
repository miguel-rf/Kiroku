package com.kiroku.app.feature.series

import com.kiroku.app.core.common.markdownToPlainText
import com.kiroku.app.core.model.SeriesDetails

data class SeriesPublisherUiModel(
    val name: String,
    val type: String,
    val notes: String?,
)

data class SeriesDetailsUiModel(
    val id: Long,
    val title: String,
    val coverUrl: String?,
    val description: String?,
    val type: String?,
    val year: String?,
    val rating: Double?,
    val ratingVotes: Int?,
    val genres: List<String>,
    val latestChapter: Int?,
    val alternativeTitles: List<String>,
    val categories: List<String>,
    val authors: List<String>,
    val artists: List<String>,
    val otherContributors: List<String>,
    val publishers: List<SeriesPublisherUiModel>,
    val status: String?,
    val licensed: Boolean?,
    val completed: Boolean?,
    val isFullDetailCached: Boolean,
)

fun SeriesDetails.toUiModel(): SeriesDetailsUiModel = SeriesDetailsUiModel(
    id = summary.id,
    title = summary.title,
    coverUrl = summary.cover?.originalUrl ?: summary.cover?.thumbnailUrl,
    description = summary.description?.let(::markdownToPlainText)?.takeIf(String::isNotBlank),
    type = summary.type,
    year = summary.year,
    rating = summary.rating,
    ratingVotes = summary.ratingVotes,
    genres = summary.genres,
    latestChapter = summary.latestChapter,
    alternativeTitles = alternativeTitles,
    categories = categories,
    authors =
    contributors
        .filter { it.role.equals("Author", ignoreCase = true) }
        .map { it.name },
    artists =
    contributors
        .filter { it.role.equals("Artist", ignoreCase = true) }
        .map { it.name },
    otherContributors =
    contributors
        .filterNot {
            it.role.equals("Author", ignoreCase = true) ||
                it.role.equals("Artist", ignoreCase = true)
        }.map { it.name },
    publishers =
    publishers.map {
        SeriesPublisherUiModel(
            name = it.name,
            type = it.type,
            notes = it.notes?.let(::markdownToPlainText)?.takeIf(String::isNotBlank),
        )
    },
    status = status?.let(::markdownToPlainText)?.takeIf(String::isNotBlank),
    licensed = licensed,
    completed = completed,
    isFullDetailCached = isFullDetailCached,
)
