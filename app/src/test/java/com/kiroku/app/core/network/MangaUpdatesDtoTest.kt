package com.kiroku.app.core.network

import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class MangaUpdatesDtoTest {
    private val json = createNetworkJson()

    @Test
    fun searchFixture_preservesLongIdsAndConfirmedNulls() {
        val response =
            json.decodeFromString<SeriesSearchResponseDto>(
                fixture("fixtures/series_search.json"),
            )

        assertEquals(10_000, response.totalHits)
        assertEquals(1, response.page)
        assertEquals(25, response.perPage)
        assertEquals(
            55_099_564_912L,
            response.results
                ?.first()
                ?.record
                ?.seriesId,
        )
        assertNull(
            response.results
                ?.first()
                ?.record
                ?.description,
        )
        assertNull(
            response.results
                ?.first()
                ?.record
                ?.bayesianRating,
        )
        assertNull(
            response.results
                ?.first()
                ?.record
                ?.image
                ?.height,
        )
        assertNull(
            response.results
                ?.first()
                ?.record
                ?.lastUpdated
                ?.asRfc3339,
        )
    }

    @Test
    fun detailFixture_ignoresUnknownFieldsWithoutDroppingSupportedData() {
        val detail =
            json.decodeFromString<SeriesDetailDto>(
                fixture("fixtures/series_detail.json"),
            )

        assertEquals(55_099_564_912L, detail.seriesId)
        assertEquals("ワンピース", detail.associated?.last()?.title)
        assertEquals("Artist", detail.authors?.last()?.type)
        assertEquals("Shueisha", detail.publishers?.single()?.publisherName)
    }

    @Test
    fun requiredErrorFields_areNotSilentlyDefaulted() {
        assertThrows(SerializationException::class.java) {
            json.decodeFromString<ApiErrorDto>("""{"status":"error"}""")
        }
    }
}

internal fun fixture(path: String): String {
    val classLoader = checkNotNull(Thread.currentThread().contextClassLoader) { "Missing test class loader" }
    return checkNotNull(classLoader.getResource(path)) { "Missing test fixture: $path" }.readText()
}
