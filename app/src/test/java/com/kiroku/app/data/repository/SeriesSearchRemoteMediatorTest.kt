package com.kiroku.app.data.repository

import android.content.Context
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kiroku.app.core.database.KirokuDatabase
import com.kiroku.app.core.database.SeriesSummaryEntity
import com.kiroku.app.core.model.SearchSpec
import com.kiroku.app.core.network.MangaUpdatesRemoteDataSource
import com.kiroku.app.core.network.SeriesRecordDto
import com.kiroku.app.core.network.SeriesSearchResponseDto
import com.kiroku.app.core.network.SeriesSearchResultDto
import com.kiroku.app.core.network.createMangaUpdatesApi
import com.kiroku.app.core.network.createNetworkJson
import com.kiroku.app.core.network.createOkHttpClient
import com.kiroku.app.test.MutableTestClock
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalPagingApi::class)
class SeriesSearchRemoteMediatorTest {
    private lateinit var database: KirokuDatabase
    private lateinit var server: MockWebServer
    private lateinit var remoteDataSource: MangaUpdatesRemoteDataSource
    private val json = createNetworkJson()
    private val clock = MutableTestClock(now = 10_000L)
    private val spec = SearchSpec(query = "One Piece")

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, KirokuDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        server = MockWebServer()
        server.start()
        val api =
            createMangaUpdatesApi(
                client = createOkHttpClient(isDebug = false),
                json = json,
                baseUrl = server.url("/").toString(),
            )
        remoteDataSource = MangaUpdatesRemoteDataSource(api, json)
    }

    @After
    fun tearDown() {
        database.close()
        server.close()
    }

    @Test
    fun refreshThenAppend_storeStableOrderAndCompleteRemoteKey() = runTest {
        server.enqueue(searchResponse(page = 1, ids = (1L..25L).toList(), totalHits = 26))
        server.enqueue(searchResponse(page = 2, ids = listOf(26L), totalHits = 26))
        val mediator = mediator()

        val refresh = mediator.load(LoadType.REFRESH, emptyPagingState())
        val append = mediator.load(LoadType.APPEND, emptyPagingState())

        assertFalse((refresh as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        assertTrue((append as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        val page = loadCachedPage()
        assertEquals((1L..26L).toList(), page.data.map { it.seriesId })
        val key = database.searchDao().getRemoteKey(spec.cacheKey)
        assertTrue(checkNotNull(key).endOfPaginationReached)
        assertNull(key.nextPage)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun completeFreshCache_skipsInitialRefresh() = runTest {
        server.enqueue(searchResponse(page = 1, ids = listOf(1L, 2L), totalHits = 2))
        val initialMediator = mediator()
        initialMediator.load(LoadType.REFRESH, emptyPagingState())

        val action = mediator().initialize()

        assertEquals(
            RemoteMediator.InitializeAction.SKIP_INITIAL_REFRESH,
            action,
        )
    }

    @Test
    fun failedRefresh_keepsExistingRowsAndReturnsError() = runTest {
        server.enqueue(searchResponse(page = 1, ids = listOf(1L, 2L), totalHits = 2))
        val mediator = mediator()
        mediator.load(LoadType.REFRESH, emptyPagingState())
        server.enqueue(
            MockResponse
                .Builder()
                .code(500)
                .addHeader("Content-Type", "application/json")
                .body("""{"status":"error","reason":"Unavailable"}""")
                .build(),
        )

        val failed = mediator.load(LoadType.REFRESH, emptyPagingState())

        assertTrue(failed is RemoteMediator.MediatorResult.Error)
        assertEquals(listOf(1L, 2L), loadCachedPage().data.map { it.seriesId })
    }

    private fun mediator(): SeriesSearchRemoteMediator = SeriesSearchRemoteMediator(
        spec = spec,
        database = database,
        remoteDataSource = remoteDataSource,
        clock = clock,
    )

    private fun emptyPagingState(): PagingState<Int, SeriesSummaryEntity> = PagingState(
        pages = emptyList(),
        anchorPosition = null,
        config =
        PagingConfig(
            pageSize = SeriesSearchRemoteMediator.NETWORK_PAGE_SIZE,
            enablePlaceholders = false,
        ),
        leadingPlaceholderCount = 0,
    )

    private suspend fun loadCachedPage(): PagingSource.LoadResult.Page<Int, SeriesSummaryEntity> {
        val result =
            database.searchDao().pagingSource(spec.cacheKey).load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 50,
                    placeholdersEnabled = false,
                ),
            )
        return result as PagingSource.LoadResult.Page<Int, SeriesSummaryEntity>
    }

    private fun searchResponse(
        page: Int,
        ids: List<Long>,
        totalHits: Int,
    ): MockResponse {
        val response =
            SeriesSearchResponseDto(
                totalHits = totalHits,
                page = page,
                perPage = SeriesSearchRemoteMediator.NETWORK_PAGE_SIZE,
                results =
                ids.map { id ->
                    SeriesSearchResultDto(
                        record =
                        SeriesRecordDto(
                            seriesId = id,
                            title = "Series $id",
                            type = "Manga",
                        ),
                    )
                },
            )
        return MockResponse
            .Builder()
            .code(200)
            .addHeader("Content-Type", "application/json")
            .body(json.encodeToString(response))
            .build()
    }
}
