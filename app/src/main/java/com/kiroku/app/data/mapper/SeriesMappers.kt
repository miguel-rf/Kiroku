package com.kiroku.app.data.mapper

import com.kiroku.app.core.database.AlternativeTitleEntity
import com.kiroku.app.core.database.CategoryEntity
import com.kiroku.app.core.database.ContributorEntity
import com.kiroku.app.core.database.PublisherEntity
import com.kiroku.app.core.database.RecentSearchEntity
import com.kiroku.app.core.database.SeriesDetailEntity
import com.kiroku.app.core.database.SeriesDetailRecord
import com.kiroku.app.core.database.SeriesSummaryEntity
import com.kiroku.app.core.model.Contributor
import com.kiroku.app.core.model.CoverImage
import com.kiroku.app.core.model.Publisher
import com.kiroku.app.core.model.RecentSearch
import com.kiroku.app.core.model.SeriesDetails
import com.kiroku.app.core.model.SeriesSummary
import com.kiroku.app.core.network.SeriesDetailDto
import com.kiroku.app.core.network.SeriesRecordDto
import com.kiroku.app.core.network.invalidPayload
import java.net.URI

data class MappedSeriesDetail(
    val summary: SeriesSummaryEntity,
    val detail: SeriesDetailEntity,
    val alternativeTitles: List<AlternativeTitleEntity>,
    val categories: List<CategoryEntity>,
    val contributors: List<ContributorEntity>,
    val publishers: List<PublisherEntity>,
)

fun SeriesRecordDto.toSearchSummaryEntity(
    cachedAtEpochMillis: Long,
    existing: SeriesSummaryEntity?,
): SeriesSummaryEntity {
    val id = requiredPositiveId(seriesId, "Search series ID")
    val mappedTitle = title.requiredText("Search series title")
    if (existing != null && existing.seriesId != id) {
        invalidPayload("Search cache record does not match the response series ID")
    }

    return SeriesSummaryEntity(
        seriesId = id,
        title = mappedTitle,
        url = url.httpsUrlOrNull() ?: existing?.url,
        description = description.cleanText() ?: existing?.description,
        imageOriginalUrl = image?.url?.original.httpsUrlOrNull() ?: existing?.imageOriginalUrl,
        imageThumbnailUrl = image?.url?.thumb.httpsUrlOrNull() ?: existing?.imageThumbnailUrl,
        imageWidth = image?.width.positiveOrNull() ?: existing?.imageWidth,
        imageHeight = image?.height.positiveOrNull() ?: existing?.imageHeight,
        type = type.cleanText() ?: existing?.type,
        year = year.cleanText() ?: existing?.year,
        rating = bayesianRating.validRatingOrNull() ?: existing?.rating,
        ratingVotes = ratingVotes.nonNegativeOrNull() ?: existing?.ratingVotes,
        genres = genres?.mapRequiredText("Search genre") { it.genre } ?: existing?.genres.orEmpty(),
        latestChapter = latestChapter.nonNegativeOrNull() ?: existing?.latestChapter,
        apiLastUpdatedEpochSeconds =
        lastUpdated?.timestamp.positiveLongOrNull() ?: existing?.apiLastUpdatedEpochSeconds,
        cachedAtEpochMillis = cachedAtEpochMillis,
    )
}

fun SeriesDetailDto.toMappedDetail(cachedAtEpochMillis: Long): MappedSeriesDetail {
    val id = requiredPositiveId(seriesId, "Series detail ID")
    val mappedSummary =
        SeriesSummaryEntity(
            seriesId = id,
            title = title.requiredText("Series detail title"),
            url = url.httpsUrlOrNull(),
            description = description.cleanText(),
            imageOriginalUrl = image?.url?.original.httpsUrlOrNull(),
            imageThumbnailUrl = image?.url?.thumb.httpsUrlOrNull(),
            imageWidth = image?.width.positiveOrNull(),
            imageHeight = image?.height.positiveOrNull(),
            type = type.cleanText(),
            year = year.cleanText(),
            rating = bayesianRating.validRatingOrNull(),
            ratingVotes = ratingVotes.nonNegativeOrNull(),
            genres = genres?.mapRequiredText("Series genre") { it.genre }.orEmpty(),
            latestChapter = latestChapter.nonNegativeOrNull(),
            apiLastUpdatedEpochSeconds = lastUpdated?.timestamp.positiveLongOrNull(),
            cachedAtEpochMillis = cachedAtEpochMillis,
        )

    return MappedSeriesDetail(
        summary = mappedSummary,
        detail =
        SeriesDetailEntity(
            seriesId = id,
            status = status.cleanText(),
            licensed = licensed,
            completed = completed,
            cachedAtEpochMillis = cachedAtEpochMillis,
        ),
        alternativeTitles =
        associated
            ?.mapIndexed { index, value ->
                AlternativeTitleEntity(
                    seriesId = id,
                    position = index,
                    title = value.title.requiredText("Alternative title at index $index"),
                )
            }.orEmpty(),
        categories =
        categories
            ?.mapIndexed { index, value ->
                CategoryEntity(
                    seriesId = id,
                    position = index,
                    name = value.category.requiredText("Category at index $index"),
                )
            }.orEmpty(),
        contributors =
        authors
            ?.mapIndexed { index, value ->
                ContributorEntity(
                    seriesId = id,
                    position = index,
                    contributorId = value.authorId.positiveLongOrNull(),
                    name = value.name.requiredText("Contributor name at index $index"),
                    role = value.type.requiredText("Contributor role at index $index"),
                    url = value.url.httpsUrlOrNull(),
                )
            }.orEmpty(),
        publishers =
        publishers
            ?.mapIndexed { index, value ->
                PublisherEntity(
                    seriesId = id,
                    position = index,
                    publisherId = value.publisherId.positiveLongOrNull(),
                    name = value.publisherName.requiredText("Publisher name at index $index"),
                    type = value.type.requiredText("Publisher type at index $index"),
                    notes = value.notes.cleanText(),
                    url = value.url.httpsUrlOrNull(),
                )
            }.orEmpty(),
    )
}

fun SeriesSummaryEntity.toDomain(): SeriesSummary = SeriesSummary(
    id = seriesId,
    title = title,
    url = url,
    description = description,
    cover =
    if (
        imageOriginalUrl == null &&
        imageThumbnailUrl == null &&
        imageWidth == null &&
        imageHeight == null
    ) {
        null
    } else {
        CoverImage(
            originalUrl = imageOriginalUrl,
            thumbnailUrl = imageThumbnailUrl,
            width = imageWidth,
            height = imageHeight,
        )
    },
    type = type,
    year = year,
    rating = rating,
    ratingVotes = ratingVotes,
    genres = genres,
    latestChapter = latestChapter,
    lastUpdatedEpochSeconds = apiLastUpdatedEpochSeconds,
)

fun SeriesDetailRecord.toDomain(): SeriesDetails = SeriesDetails(
    summary = summary.toDomain(),
    alternativeTitles = alternativeTitles.sortedBy { it.position }.map { it.title },
    categories = categories.sortedBy { it.position }.map { it.name },
    contributors =
    contributors.sortedBy { it.position }.map {
        Contributor(
            id = it.contributorId,
            name = it.name,
            role = it.role,
            url = it.url,
        )
    },
    publishers =
    publishers.sortedBy { it.position }.map {
        Publisher(
            id = it.publisherId,
            name = it.name,
            type = it.type,
            notes = it.notes,
            url = it.url,
        )
    },
    status = detail?.status,
    licensed = detail?.licensed,
    completed = detail?.completed,
    isFullDetailCached = detail != null,
)

fun RecentSearchEntity.toDomain(): RecentSearch = RecentSearch(
    query = displayQuery,
    searchedAtEpochMillis = searchedAtEpochMillis,
)

private fun requiredPositiveId(
    value: Long?,
    label: String,
): Long = value?.takeIf { it > 0L } ?: invalidPayload("$label must be a positive integer")

private fun String?.requiredText(label: String): String = cleanText() ?: invalidPayload("$label must be a non-blank string")

private fun String?.cleanText(): String? = this?.trim()?.takeIf(String::isNotEmpty)

private fun String?.httpsUrlOrNull(): String? {
    val value = cleanText() ?: return null
    val uri = runCatching { URI(value) }.getOrNull() ?: return null
    return value.takeIf {
        uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
    }
}

private fun Int?.positiveOrNull(): Int? = this?.takeIf { it > 0 }

private fun Int?.nonNegativeOrNull(): Int? = this?.takeIf { it >= 0 }

private fun Long?.positiveLongOrNull(): Long? = this?.takeIf { it > 0L }

private fun Double?.validRatingOrNull(): Double? = this?.takeIf { it.isFinite() && it >= 0.0 }

private inline fun <T> List<T>.mapRequiredText(
    label: String,
    value: (T) -> String?,
): List<String> = mapIndexed { index, item -> value(item).requiredText("$label at index $index") }
