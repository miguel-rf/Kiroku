package com.kiroku.app.data.mapper

import com.kiroku.app.core.database.SeriesDetailRecord
import com.kiroku.app.core.database.SeriesSummaryEntity
import com.kiroku.app.core.network.AppFailure
import com.kiroku.app.core.network.FailureKind
import com.kiroku.app.core.network.SeriesDetailDto
import com.kiroku.app.core.network.SeriesRecordDto
import com.kiroku.app.core.network.createNetworkJson
import com.kiroku.app.core.network.fixture
import com.kiroku.app.feature.series.toUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SeriesMappersTest {
    private val json = createNetworkJson()

    @Test
    fun detailMapping_preservesSupportedMetadataAndSeparatesContributorRoles() {
        val dto =
            json.decodeFromString<SeriesDetailDto>(
                fixture("fixtures/series_detail.json"),
            )
        val mapped = dto.toMappedDetail(cachedAtEpochMillis = 123L)
        val domain =
            SeriesDetailRecord(
                summary = mapped.summary,
                detail = mapped.detail,
                alternativeTitles = mapped.alternativeTitles,
                categories = mapped.categories,
                contributors = mapped.contributors,
                publishers = mapped.publishers,
            ).toDomain()
        val ui = domain.toUiModel()

        assertEquals(55_099_564_912L, ui.id)
        assertEquals(listOf("ONE PIECE", "ワンピース"), ui.alternativeTitles)
        assertEquals(listOf("Eiichiro Oda"), ui.authors)
        assertEquals(listOf("Eiichiro Oda"), ui.artists)
        assertEquals("A pirate adventure\n\nFollow Luffy and his crew.", ui.description)
        assertEquals("Ongoing in Japan", ui.status)
        assertNull(mapped.publishers.single().url)
    }

    @Test
    fun searchMapping_keepsRicherCachedFieldsWhenSearchOmitsThem() {
        val existing = summaryEntity(description = "Cached full description")
        val mapped =
            SeriesRecordDto(
                seriesId = existing.seriesId,
                title = "Updated title",
                description = null,
            ).toSearchSummaryEntity(
                cachedAtEpochMillis = 456L,
                existing = existing,
            )

        assertEquals("Updated title", mapped.title)
        assertEquals("Cached full description", mapped.description)
        assertEquals(existing.imageOriginalUrl, mapped.imageOriginalUrl)
        assertEquals(456L, mapped.cachedAtEpochMillis)
    }

    @Test
    fun invalidRequiredIdentity_failsInsteadOfInventingAValue() {
        val failure =
            assertThrows(AppFailure::class.java) {
                SeriesRecordDto(seriesId = 0L, title = "Invalid")
                    .toSearchSummaryEntity(cachedAtEpochMillis = 1L, existing = null)
            }

        assertEquals(FailureKind.SERIALIZATION, failure.kind)
        assertTrue(failure.message.orEmpty().contains("positive integer"))
    }

    private fun summaryEntity(description: String): SeriesSummaryEntity = SeriesSummaryEntity(
        seriesId = 55_099_564_912L,
        title = "One Piece",
        url = "https://www.mangaupdates.com/series/example",
        description = description,
        imageOriginalUrl = "https://cdn.mangaupdates.com/original.jpg",
        imageThumbnailUrl = "https://cdn.mangaupdates.com/thumb.jpg",
        imageWidth = 600,
        imageHeight = 900,
        type = "Manga",
        year = "1997",
        rating = 8.9,
        ratingVotes = 12_345,
        genres = listOf("Action"),
        latestChapter = 1120,
        apiLastUpdatedEpochSeconds = 1_735_689_600L,
        cachedAtEpochMillis = 100L,
    )
}
