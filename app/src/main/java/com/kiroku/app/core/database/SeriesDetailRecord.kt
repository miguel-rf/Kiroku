package com.kiroku.app.core.database

import androidx.room.Embedded
import androidx.room.Relation

data class SeriesDetailRecord(
    @Embedded
    val summary: SeriesSummaryEntity,
    @Relation(
        parentColumn = "seriesId",
        entityColumn = "seriesId",
    )
    val detail: SeriesDetailEntity?,
    @Relation(
        parentColumn = "seriesId",
        entityColumn = "seriesId",
    )
    val alternativeTitles: List<AlternativeTitleEntity>,
    @Relation(
        parentColumn = "seriesId",
        entityColumn = "seriesId",
    )
    val categories: List<CategoryEntity>,
    @Relation(
        parentColumn = "seriesId",
        entityColumn = "seriesId",
    )
    val contributors: List<ContributorEntity>,
    @Relation(
        parentColumn = "seriesId",
        entityColumn = "seriesId",
    )
    val publishers: List<PublisherEntity>,
)
