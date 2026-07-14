package com.kiroku.app.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.kiroku.app.core.common.Clock
import com.kiroku.app.core.database.KirokuDatabase
import com.kiroku.app.core.database.SearchQueryEntity
import com.kiroku.app.core.database.SearchRemoteKeyEntity
import com.kiroku.app.core.database.SearchResultEntity
import com.kiroku.app.core.database.SeriesSummaryEntity
import com.kiroku.app.core.model.SearchFilter
import com.kiroku.app.core.model.SearchSpec
import com.kiroku.app.core.model.SeriesTypeFilter
import com.kiroku.app.core.network.AppFailure
import com.kiroku.app.core.network.FailureKind
import com.kiroku.app.core.network.MangaUpdatesRemoteDataSource
import com.kiroku.app.core.network.SeriesSearchRequestDto
import com.kiroku.app.core.network.invalidPayload
import com.kiroku.app.data.mapper.toSearchSummaryEntity

@OptIn(ExperimentalPagingApi::class)
class SeriesSearchRemoteMediator(
    private val spec: SearchSpec,
    private val database: KirokuDatabase,
    private val remoteDataSource: MangaUpdatesRemoteDataSource,
    private val clock: Clock,
) : RemoteMediator<Int, SeriesSummaryEntity>() {
    private val searchDao = database.searchDao()
    private val seriesDao = database.seriesDao()

    override suspend fun initialize(): InitializeAction {
        val now = clock.nowEpochMillis()
        val canReuseCompleteCache =
            database.withTransaction {
                val query = searchDao.getQuery(spec.cacheKey) ?: return@withTransaction false
                val key = searchDao.getRemoteKey(spec.cacheKey) ?: return@withTransaction false
                val ageMillis = now - query.updatedAtEpochMillis
                key.endOfPaginationReached && ageMillis in 0..CACHE_TIMEOUT_MILLIS
            }
        return if (canReuseCompleteCache) {
            InitializeAction.SKIP_INITIAL_REFRESH
        } else {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        }
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, SeriesSummaryEntity>,
    ): MediatorResult {
        val page =
            when (loadType) {
                LoadType.REFRESH -> {
                    FIRST_PAGE
                }

                LoadType.PREPEND -> {
                    return MediatorResult.Success(endOfPaginationReached = true)
                }

                LoadType.APPEND -> {
                    val key =
                        database.withTransaction {
                            searchDao.getRemoteKey(spec.cacheKey)
                        }
                    key?.nextPage
                        ?: return MediatorResult.Success(
                            endOfPaginationReached = key?.endOfPaginationReached ?: true,
                        )
                }
            }

        return try {
            val response =
                remoteDataSource.searchSeries(
                    SeriesSearchRequestDto(
                        search = spec.normalizedQuery,
                        type = spec.types.typeApiValuesOrNull(),
                        filters = spec.filters.filterApiValuesOrNull(),
                        page = page,
                        perpage = NETWORK_PAGE_SIZE,
                    ),
                )
            val responsePage =
                response.page
                    ?: invalidPayload("Search response did not include its page number")
            if (responsePage != page) {
                invalidPayload(
                    "Search response page " + responsePage +
                        " did not match requested page " + page,
                )
            }
            val responsePageSize =
                response.perPage?.takeIf { it > 0 }
                    ?: invalidPayload("Search response did not include a positive page size")
            val totalHits = response.totalHits
            if (totalHits != null && totalHits < 0) {
                invalidPayload("Search response total hit count cannot be negative")
            }
            val responseResults =
                response.results
                    ?: invalidPayload("Search response did not include a results array")
            if (responseResults.size > responsePageSize) {
                invalidPayload("Search response contained more records than its page size")
            }

            val records =
                responseResults.mapIndexed { index, result ->
                    result.record
                        ?: invalidPayload("Search result at index $index did not contain a record")
                }
            val ids =
                records.mapIndexed { index, record ->
                    record.seriesId?.takeIf { it > 0L }
                        ?: invalidPayload("Search result at index $index had an invalid series ID")
                }
            if (ids.distinct().size != ids.size) {
                invalidPayload("Search response contained duplicate series IDs")
            }

            val firstPosition = (responsePage - FIRST_PAGE) * responsePageSize
            val reachedKnownTotal =
                totalHits != null && firstPosition + records.size >= totalHits
            val endReached =
                records.isEmpty() || reachedKnownTotal || records.size < responsePageSize
            val now = clock.nowEpochMillis()

            database.withTransaction {
                val existingById =
                    seriesDao.getSummaries(ids).associateBy(SeriesSummaryEntity::seriesId)
                val summaries =
                    records.map { record ->
                        record.toSearchSummaryEntity(
                            cachedAtEpochMillis = now,
                            existing = record.seriesId?.let(existingById::get),
                        )
                    }

                searchDao.insertQuery(
                    SearchQueryEntity(
                        cacheKey = spec.cacheKey,
                        query = spec.normalizedQuery,
                        types = spec.types.map(SeriesTypeFilter::apiValue).sorted(),
                        filters = spec.filters.map(SearchFilter::apiValue).sorted(),
                        totalHits = totalHits,
                        updatedAtEpochMillis = now,
                    ),
                )
                if (loadType == LoadType.REFRESH) {
                    searchDao.deleteResults(spec.cacheKey)
                }
                seriesDao.upsertSummaries(summaries)
                searchDao.insertResults(
                    summaries.mapIndexed { index, summary ->
                        SearchResultEntity(
                            cacheKey = spec.cacheKey,
                            seriesId = summary.seriesId,
                            position = firstPosition + index,
                        )
                    },
                )
                searchDao.insertRemoteKey(
                    SearchRemoteKeyEntity(
                        cacheKey = spec.cacheKey,
                        nextPage = if (endReached) null else responsePage + 1,
                        endOfPaginationReached = endReached,
                        updatedAtEpochMillis = now,
                    ),
                )
            }

            MediatorResult.Success(endOfPaginationReached = endReached)
        } catch (failure: AppFailure) {
            MediatorResult.Error(failure)
        } catch (failure: Throwable) {
            MediatorResult.Error(
                AppFailure(
                    kind = FailureKind.UNKNOWN,
                    diagnosticReason = "Unexpected search paging failure",
                    cause = failure,
                ),
            )
        }
    }

    private fun Set<SeriesTypeFilter>.typeApiValuesOrNull(): List<String>? = map(SeriesTypeFilter::apiValue).sorted().takeIf(List<String>::isNotEmpty)

    private fun Set<SearchFilter>.filterApiValuesOrNull(): List<String>? = map(SearchFilter::apiValue).sorted().takeIf(List<String>::isNotEmpty)

    companion object {
        const val NETWORK_PAGE_SIZE = 25
        private const val FIRST_PAGE = 1
        private const val CACHE_TIMEOUT_MILLIS = 6L * 60L * 60L * 1_000L
    }
}
