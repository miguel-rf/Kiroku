package com.kiroku.app.data.repository

import androidx.paging.PagingData
import com.kiroku.app.core.model.RecentSearch
import com.kiroku.app.core.model.SearchSpec
import com.kiroku.app.core.model.SeriesDetails
import com.kiroku.app.core.model.SeriesSummary
import kotlinx.coroutines.flow.Flow

interface CatalogueRepository {
    fun search(spec: SearchSpec): Flow<PagingData<SeriesSummary>>

    fun observeRecentSearches(): Flow<List<RecentSearch>>

    suspend fun recordRecentSearch(query: String)

    suspend fun clearRecentSearches()

    fun observeSeries(seriesId: Long): Flow<SeriesDetails?>

    suspend fun refreshSeries(seriesId: Long)
}
