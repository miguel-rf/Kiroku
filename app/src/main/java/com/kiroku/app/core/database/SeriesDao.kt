package com.kiroku.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SeriesDao {
    @Upsert
    suspend fun upsertSummaries(summaries: List<SeriesSummaryEntity>)

    @Upsert
    suspend fun upsertSummary(summary: SeriesSummaryEntity)

    @Query("SELECT * FROM series_summaries WHERE seriesId = :seriesId")
    suspend fun getSummary(seriesId: Long): SeriesSummaryEntity?

    @Query("SELECT * FROM series_summaries WHERE seriesId IN (:seriesIds)")
    suspend fun getSummaries(seriesIds: List<Long>): List<SeriesSummaryEntity>

    @Transaction
    @Query("SELECT * FROM series_summaries WHERE seriesId = :seriesId")
    fun observeDetail(seriesId: Long): Flow<SeriesDetailRecord?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetail(detail: SeriesDetailEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlternativeTitles(titles: List<AlternativeTitleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContributors(contributors: List<ContributorEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPublishers(publishers: List<PublisherEntity>)

    @Query("DELETE FROM series_alternative_titles WHERE seriesId = :seriesId")
    suspend fun deleteAlternativeTitles(seriesId: Long)

    @Query("DELETE FROM series_categories WHERE seriesId = :seriesId")
    suspend fun deleteCategories(seriesId: Long)

    @Query("DELETE FROM series_contributors WHERE seriesId = :seriesId")
    suspend fun deleteContributors(seriesId: Long)

    @Query("DELETE FROM series_publishers WHERE seriesId = :seriesId")
    suspend fun deletePublishers(seriesId: Long)
}
