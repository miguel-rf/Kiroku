package com.kiroku.app.core.database

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchDao {
    @Query(
        """
        SELECT series_summaries.*
        FROM search_results
        INNER JOIN series_summaries
            ON search_results.seriesId = series_summaries.seriesId
        WHERE search_results.cacheKey = :cacheKey
        ORDER BY search_results.position ASC
        """,
    )
    fun pagingSource(cacheKey: String): PagingSource<Int, SeriesSummaryEntity>

    @Upsert
    suspend fun insertQuery(query: SearchQueryEntity)

    @Query("SELECT * FROM search_queries WHERE cacheKey = :cacheKey")
    suspend fun getQuery(cacheKey: String): SearchQueryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResults(results: List<SearchResultEntity>)

    @Query("DELETE FROM search_results WHERE cacheKey = :cacheKey")
    suspend fun deleteResults(cacheKey: String)

    @Upsert
    suspend fun insertRemoteKey(key: SearchRemoteKeyEntity)

    @Query("SELECT * FROM search_remote_keys WHERE cacheKey = :cacheKey")
    suspend fun getRemoteKey(cacheKey: String): SearchRemoteKeyEntity?

    @Upsert
    suspend fun insertRecentSearch(search: RecentSearchEntity)

    @Query(
        """
        SELECT * FROM recent_searches
        ORDER BY searchedAtEpochMillis DESC
        LIMIT :limit
        """,
    )
    fun observeRecentSearches(limit: Int): Flow<List<RecentSearchEntity>>

    @Query(
        """
        DELETE FROM recent_searches
        WHERE normalizedQuery NOT IN (
            SELECT normalizedQuery
            FROM recent_searches
            ORDER BY searchedAtEpochMillis DESC
            LIMIT :limit
        )
        """,
    )
    suspend fun trimRecentSearches(limit: Int)

    @Query("DELETE FROM recent_searches")
    suspend fun clearRecentSearches()
}
