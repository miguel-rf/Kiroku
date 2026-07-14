package com.kiroku.app.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "series_summaries")
data class SeriesSummaryEntity(
    @androidx.room.PrimaryKey
    val seriesId: Long,
    val title: String,
    val url: String?,
    val description: String?,
    val imageOriginalUrl: String?,
    val imageThumbnailUrl: String?,
    val imageWidth: Int?,
    val imageHeight: Int?,
    val type: String?,
    val year: String?,
    val rating: Double?,
    val ratingVotes: Int?,
    val genres: List<String>,
    val latestChapter: Int?,
    val apiLastUpdatedEpochSeconds: Long?,
    val cachedAtEpochMillis: Long,
)

@Entity(
    tableName = "series_details",
    foreignKeys = [
        ForeignKey(
            entity = SeriesSummaryEntity::class,
            parentColumns = ["seriesId"],
            childColumns = ["seriesId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SeriesDetailEntity(
    @androidx.room.PrimaryKey
    val seriesId: Long,
    val status: String?,
    val licensed: Boolean?,
    val completed: Boolean?,
    val cachedAtEpochMillis: Long,
)

@Entity(
    tableName = "series_alternative_titles",
    primaryKeys = ["seriesId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = SeriesSummaryEntity::class,
            parentColumns = ["seriesId"],
            childColumns = ["seriesId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("seriesId")],
)
data class AlternativeTitleEntity(
    val seriesId: Long,
    val position: Int,
    val title: String,
)

@Entity(
    tableName = "series_categories",
    primaryKeys = ["seriesId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = SeriesSummaryEntity::class,
            parentColumns = ["seriesId"],
            childColumns = ["seriesId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("seriesId")],
)
data class CategoryEntity(
    val seriesId: Long,
    val position: Int,
    val name: String,
)

@Entity(
    tableName = "series_contributors",
    primaryKeys = ["seriesId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = SeriesSummaryEntity::class,
            parentColumns = ["seriesId"],
            childColumns = ["seriesId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("seriesId")],
)
data class ContributorEntity(
    val seriesId: Long,
    val position: Int,
    val contributorId: Long?,
    val name: String,
    val role: String,
    val url: String?,
)

@Entity(
    tableName = "series_publishers",
    primaryKeys = ["seriesId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = SeriesSummaryEntity::class,
            parentColumns = ["seriesId"],
            childColumns = ["seriesId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("seriesId")],
)
data class PublisherEntity(
    val seriesId: Long,
    val position: Int,
    val publisherId: Long?,
    val name: String,
    val type: String,
    val notes: String?,
    val url: String?,
)
