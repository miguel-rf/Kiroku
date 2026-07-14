package com.kiroku.app.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.withTransaction
import com.kiroku.app.core.common.Clock
import com.kiroku.app.core.database.KirokuDatabase
import com.kiroku.app.core.database.RecentSearchEntity
import com.kiroku.app.core.model.RecentSearch
import com.kiroku.app.core.model.SearchSpec
import com.kiroku.app.core.model.SeriesDetails
import com.kiroku.app.core.model.SeriesSummary
import com.kiroku.app.core.network.MangaUpdatesRemoteDataSource
import com.kiroku.app.core.network.invalidPayload
import com.kiroku.app.data.mapper.toDomain
import com.kiroku.app.data.mapper.toMappedDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

class OfflineFirstCatalogueRepository(
    private val database: KirokuDatabase,
    private val remoteDataSource: MangaUpdatesRemoteDataSource,
    private val clock: Clock,
) : CatalogueRepository {
    private val searchDao = database.searchDao()
    private val seriesDao = database.seriesDao()

    @OptIn(ExperimentalPagingApi::class)
    override fun search(spec: SearchSpec): Flow<PagingData<SeriesSummary>> {
        require(spec.isSearchable) { "A searchable query is required" }
        return Pager(
            config =
            PagingConfig(
                pageSize = SeriesSearchRemoteMediator.NETWORK_PAGE_SIZE,
                initialLoadSize = SeriesSearchRemoteMediator.NETWORK_PAGE_SIZE,
                prefetchDistance = 5,
                enablePlaceholders = false,
                maxSize = 200,
            ),
            remoteMediator =
            SeriesSearchRemoteMediator(
                spec = spec,
                database = database,
                remoteDataSource = remoteDataSource,
                clock = clock,
            ),
            pagingSourceFactory = { searchDao.pagingSource(spec.cacheKey) },
        ).flow.map { pagingData -> pagingData.map { entity -> entity.toDomain() } }
    }

    override fun observeRecentSearches(): Flow<List<RecentSearch>> = searchDao.observeRecentSearches(RECENT_SEARCH_LIMIT).map { searches ->
        searches.map { it.toDomain() }
    }

    override suspend fun recordRecentSearch(query: String) {
        val displayQuery = query.trim()
        if (displayQuery.length < SearchSpec.MIN_QUERY_LENGTH) return

        database.withTransaction {
            searchDao.insertRecentSearch(
                RecentSearchEntity(
                    normalizedQuery = displayQuery.lowercase(Locale.ROOT),
                    displayQuery = displayQuery,
                    searchedAtEpochMillis = clock.nowEpochMillis(),
                ),
            )
            searchDao.trimRecentSearches(RECENT_SEARCH_LIMIT)
        }
    }

    override suspend fun clearRecentSearches() {
        searchDao.clearRecentSearches()
    }

    override fun observeSeries(seriesId: Long): Flow<SeriesDetails?> {
        require(seriesId > 0L) { "A positive series ID is required" }
        return seriesDao.observeDetail(seriesId).map { record -> record?.toDomain() }
    }

    override suspend fun refreshSeries(seriesId: Long) {
        require(seriesId > 0L) { "A positive series ID is required" }
        val mapped =
            remoteDataSource
                .getSeries(seriesId)
                .toMappedDetail(cachedAtEpochMillis = clock.nowEpochMillis())
        if (mapped.summary.seriesId != seriesId) {
            invalidPayload(
                "Series detail ID " + mapped.summary.seriesId +
                    " did not match requested ID " + seriesId,
            )
        }

        database.withTransaction {
            seriesDao.upsertSummary(mapped.summary)
            seriesDao.insertDetail(mapped.detail)
            seriesDao.deleteAlternativeTitles(seriesId)
            seriesDao.deleteCategories(seriesId)
            seriesDao.deleteContributors(seriesId)
            seriesDao.deletePublishers(seriesId)
            if (mapped.alternativeTitles.isNotEmpty()) {
                seriesDao.insertAlternativeTitles(mapped.alternativeTitles)
            }
            if (mapped.categories.isNotEmpty()) {
                seriesDao.insertCategories(mapped.categories)
            }
            if (mapped.contributors.isNotEmpty()) {
                seriesDao.insertContributors(mapped.contributors)
            }
            if (mapped.publishers.isNotEmpty()) {
                seriesDao.insertPublishers(mapped.publishers)
            }
        }
    }

    private companion object {
        const val RECENT_SEARCH_LIMIT = 8
    }
}
