package com.kiroku.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.kiroku.app.core.database.KirokuDatabase
import com.kiroku.app.core.network.AppFailure
import com.kiroku.app.core.network.FailureKind
import com.kiroku.app.core.network.MangaUpdatesRemoteDataSource
import com.kiroku.app.core.network.createMangaUpdatesApi
import com.kiroku.app.core.network.createNetworkJson
import com.kiroku.app.core.network.createOkHttpClient
import com.kiroku.app.core.network.fixture
import com.kiroku.app.test.MutableTestClock
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OfflineFirstCatalogueRepositoryTest {
    private lateinit var database: KirokuDatabase
    private lateinit var server: MockWebServer
    private lateinit var repository: OfflineFirstCatalogueRepository
    private val json = createNetworkJson()
    private val clock = MutableTestClock(now = 1_000L)

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
        repository =
            OfflineFirstCatalogueRepository(
                database = database,
                remoteDataSource = MangaUpdatesRemoteDataSource(api, json),
                clock = clock,
            )
    }

    @After
    fun tearDown() {
        database.close()
        server.close()
    }

    @Test
    fun refreshSeries_commitsBeforeDatabaseFlowEmits() = runTest {
        server.enqueue(jsonResponse(fixture("fixtures/series_detail.json")))

        repository.observeSeries(SERIES_ID).test {
            assertNull(awaitItem())

            repository.refreshSeries(SERIES_ID)
            val cached = awaitItem()

            assertEquals(SERIES_ID, cached?.summary?.id)
            assertEquals(listOf("ONE PIECE", "ワンピース"), cached?.alternativeTitles)
            assertEquals(listOf("Pirates", "Found Family"), cached?.categories)
            assertEquals("**Ongoing** in Japan", cached?.status)
            assertFalse(checkNotNull(cached).completed ?: true)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun failedRefresh_preservesPreviouslyCachedDetail() = runTest {
        server.enqueue(jsonResponse(fixture("fixtures/series_detail.json")))
        repository.refreshSeries(SERIES_ID)
        server.enqueue(
            MockResponse
                .Builder()
                .code(500)
                .addHeader("Content-Type", "application/json")
                .body("""{"status":"error","reason":"Unavailable"}""")
                .build(),
        )

        val failure =
            captureAppFailure { repository.refreshSeries(SERIES_ID) }

        assertEquals(FailureKind.SERVER, failure.kind)
        repository.observeSeries(SERIES_ID).test {
            assertEquals("One Piece", awaitItem()?.summary?.title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun jsonResponse(body: String): MockResponse = MockResponse
        .Builder()
        .code(200)
        .addHeader("Content-Type", "application/json")
        .body(body)
        .build()

    private suspend fun captureAppFailure(block: suspend () -> Unit): AppFailure = try {
        block()
        throw AssertionError("Expected AppFailure")
    } catch (failure: AppFailure) {
        failure
    }

    private companion object {
        const val SERIES_ID = 55_099_564_912L
    }
}
