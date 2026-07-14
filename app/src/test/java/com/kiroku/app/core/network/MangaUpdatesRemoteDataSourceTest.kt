package com.kiroku.app.core.network

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.net.ConnectException
import java.util.concurrent.TimeUnit

class MangaUpdatesRemoteDataSourceTest {
    private lateinit var server: MockWebServer
    private val json = createNetworkJson()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun search_sendsConfirmedPathAndPagingBody() = runTest {
        server.enqueue(
            MockResponse
                .Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body(fixture("fixtures/series_search.json"))
                .build(),
        )

        val response =
            dataSource().searchSeries(
                SeriesSearchRequestDto(
                    search = "One Piece",
                    type = listOf("Manga"),
                    filters = listOf("some_releases"),
                    page = 1,
                ),
            )
        val request = server.takeRequest()
        val body =
            json.parseToJsonElement(checkNotNull(request.body).utf8()).jsonObject

        assertEquals(10_000, response.totalHits)
        assertEquals("POST", request.method)
        assertEquals("/series/search", request.url.encodedPath)
        assertEquals("One Piece", body.getValue("search").jsonPrimitive.content)
        assertEquals(
            1,
            body
                .getValue("page")
                .jsonPrimitive.content
                .toInt(),
        )
        assertEquals(
            25,
            body
                .getValue("perpage")
                .jsonPrimitive.content
                .toInt(),
        )
        assertEquals("application/json", request.headers["Accept"])
        assertNotNull(request.headers["User-Agent"])
    }

    @Test
    fun rateLimit_preservesRetryAfterMetadata() = runTest {
        server.enqueue(
            MockResponse
                .Builder()
                .code(429)
                .addHeader("Content-Type", "application/json")
                .addHeader("Retry-After", "12")
                .body(
                    """{"status":"error","reason":"Slow down","context":null}""",
                ).build(),
        )

        val failure =
            captureAppFailure { dataSource().getSeries(42L) }

        assertEquals(FailureKind.RATE_LIMITED, failure.kind)
        assertEquals("Slow down", failure.diagnosticReason)
        assertEquals(12L, failure.retryAfterSeconds)
    }

    @Test
    fun malformedSuccessBody_isASerializationFailure() = runTest {
        server.enqueue(
            MockResponse
                .Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body("{")
                .build(),
        )

        val failure =
            captureAppFailure { dataSource().getSeries(42L) }

        assertEquals(FailureKind.SERIALIZATION, failure.kind)
    }

    @Test
    fun delayedHeaders_areMappedToTimeout() = runTest {
        server.enqueue(
            MockResponse
                .Builder()
                .code(200)
                .headersDelay(250L, TimeUnit.MILLISECONDS)
                .body(fixture("fixtures/series_detail.json"))
                .build(),
        )
        val timeouts =
            NetworkTimeouts(
                connectMillis = 100L,
                readMillis = 50L,
                writeMillis = 100L,
                callMillis = 100L,
            )

        val failure =
            captureAppFailure {
                dataSource(timeouts).getSeries(55_099_564_912L)
            }

        assertEquals(FailureKind.TIMEOUT, failure.kind)
    }

    @Test
    fun refusedConnection_isMappedToOffline() = runTest {
        val failingApi =
            object : MangaUpdatesApi {
                override suspend fun searchSeries(
                    request: SeriesSearchRequestDto,
                ): Response<SeriesSearchResponseDto> = throw ConnectException("Connection refused")

                override suspend fun getSeries(seriesId: Long): Response<SeriesDetailDto> = throw ConnectException("Connection refused")
            }

        val failure =
            captureAppFailure {
                MangaUpdatesRemoteDataSource(failingApi, json).getSeries(42L)
            }

        assertEquals(FailureKind.OFFLINE, failure.kind)
    }

    private fun dataSource(timeouts: NetworkTimeouts = NetworkTimeouts()): MangaUpdatesRemoteDataSource {
        val client = createOkHttpClient(isDebug = false, timeouts = timeouts)
        val api =
            createMangaUpdatesApi(
                client = client,
                json = json,
                baseUrl = server.url("/").toString(),
            )
        return MangaUpdatesRemoteDataSource(api = api, json = json)
    }

    private suspend fun captureAppFailure(block: suspend () -> Unit): AppFailure = try {
        block()
        throw AssertionError("Expected AppFailure")
    } catch (failure: AppFailure) {
        failure
    }
}
