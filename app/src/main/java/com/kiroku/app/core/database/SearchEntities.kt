package com.kiroku.app.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "search_queries")
data class SearchQueryEntity(
    @androidx.room.PrimaryKey
    val cacheKey: String,
    val query: String,
    val types: List<String>,
    val filters: List<String>,
    val totalHits: Int?,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "search_results",
    primaryKeys = ["cacheKey", "seriesId"],
    foreignKeys = [
        ForeignKey(
            entity = SearchQueryEntity::class,
            parentColumns = ["cacheKey"],
            childColumns = ["cacheKey"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SeriesSummaryEntity::class,
            parentColumns = ["seriesId"],
            childColumns = ["seriesId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("cacheKey"),
        Index("seriesId"),
        Index(value = ["cacheKey", "position"], unique = true),
    ],
)
data class SearchResultEntity(
    val cacheKey: String,
    val seriesId: Long,
    val position: Int,
)

@Entity(
    tableName = "search_remote_keys",
    foreignKeys = [
        ForeignKey(
            entity = SearchQueryEntity::class,
            parentColumns = ["cacheKey"],
            childColumns = ["cacheKey"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SearchRemoteKeyEntity(
    @androidx.room.PrimaryKey
    val cacheKey: String,
    val nextPage: Int?,
    val endOfPaginationReached: Boolean,
    val updatedAtEpochMillis: Long,
)

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @androidx.room.PrimaryKey
    val normalizedQuery: String,
    val displayQuery: String,
    val searchedAtEpochMillis: Long,
)
