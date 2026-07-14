package com.kiroku.app.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SeriesSearchRequestDto(
    val search: String,
    val stype: String = "title",
    val type: List<String>? = null,
    val filters: List<String>? = null,
    val page: Int,
    val perpage: Int = 25,
)

@Serializable
data class SeriesSearchResponseDto(
    @SerialName("total_hits")
    val totalHits: Int? = null,
    val page: Int? = null,
    @SerialName("per_page")
    val perPage: Int? = null,
    val results: List<SeriesSearchResultDto>? = null,
)

@Serializable
data class SeriesSearchResultDto(
    val record: SeriesRecordDto? = null,
    @SerialName("hit_title")
    val hitTitle: String? = null,
)

@Serializable
data class SeriesRecordDto(
    @SerialName("series_id")
    val seriesId: Long? = null,
    val title: String? = null,
    val url: String? = null,
    val description: String? = null,
    val image: ImageDto? = null,
    val type: String? = null,
    val year: String? = null,
    @SerialName("bayesian_rating")
    val bayesianRating: Double? = null,
    @SerialName("rating_votes")
    val ratingVotes: Int? = null,
    val genres: List<GenreDto>? = null,
    @SerialName("latest_chapter")
    val latestChapter: Int? = null,
    @SerialName("last_updated")
    val lastUpdated: ApiTimeDto? = null,
)

@Serializable
data class SeriesDetailDto(
    @SerialName("series_id")
    val seriesId: Long? = null,
    val title: String? = null,
    val url: String? = null,
    val associated: List<AssociatedTitleDto>? = null,
    val description: String? = null,
    val image: ImageDto? = null,
    val type: String? = null,
    val year: String? = null,
    @SerialName("bayesian_rating")
    val bayesianRating: Double? = null,
    @SerialName("rating_votes")
    val ratingVotes: Int? = null,
    val genres: List<GenreDto>? = null,
    val categories: List<CategoryDto>? = null,
    @SerialName("latest_chapter")
    val latestChapter: Int? = null,
    val status: String? = null,
    val licensed: Boolean? = null,
    val completed: Boolean? = null,
    val authors: List<ContributorDto>? = null,
    val publishers: List<PublisherDto>? = null,
    @SerialName("last_updated")
    val lastUpdated: ApiTimeDto? = null,
)

@Serializable
data class ImageDto(
    val url: ImageUrlsDto? = null,
    val height: Int? = null,
    val width: Int? = null,
)

@Serializable
data class ImageUrlsDto(
    val original: String? = null,
    val thumb: String? = null,
)

@Serializable
data class GenreDto(
    val genre: String? = null,
)

@Serializable
data class AssociatedTitleDto(
    val title: String? = null,
)

@Serializable
data class CategoryDto(
    val category: String? = null,
)

@Serializable
data class ContributorDto(
    val name: String? = null,
    @SerialName("author_id")
    val authorId: Long? = null,
    val url: String? = null,
    val type: String? = null,
)

@Serializable
data class PublisherDto(
    @SerialName("publisher_name")
    val publisherName: String? = null,
    @SerialName("publisher_id")
    val publisherId: Long? = null,
    val url: String? = null,
    val type: String? = null,
    val notes: String? = null,
)

@Serializable
data class ApiTimeDto(
    val timestamp: Long? = null,
    @SerialName("as_rfc3339")
    val asRfc3339: String? = null,
    @SerialName("as_string")
    val asString: String? = null,
)

@Serializable
data class ApiErrorDto(
    val status: String,
    val reason: String,
    val context: JsonElement? = null,
)
