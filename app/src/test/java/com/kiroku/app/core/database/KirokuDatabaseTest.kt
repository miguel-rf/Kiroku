package com.kiroku.app.core.database

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class KirokuDatabaseTest {
    private lateinit var database: KirokuDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, KirokuDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun searchResults_areReturnedInApiPositionOrder() = runTest {
        val query = searchQuery()
        val later = summary(seriesId = 2L, title = "Second")
        val earlier = summary(seriesId = 1L, title = "First")

        database.searchDao().insertQuery(query)
        database.seriesDao().upsertSummaries(listOf(later, earlier))
        database.searchDao().insertResults(
            listOf(
                SearchResultEntity(query.cacheKey, later.seriesId, position = 1),
                SearchResultEntity(query.cacheKey, earlier.seriesId, position = 0),
            ),
        )

        val result =
            database.searchDao().pagingSource(query.cacheKey).load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 25,
                    placeholdersEnabled = false,
                ),
            )
        val page =
            result as PagingSource.LoadResult.Page<Int, SeriesSummaryEntity>

        assertEquals(listOf(1L, 2L), page.data.map { it.seriesId })
    }

    @Test
    fun detailRelation_returnsOrderedChildrenAndFullCacheMarker() = runTest {
        val seriesId = 55_099_564_912L
        database.seriesDao().upsertSummary(summary(seriesId, "One Piece"))
        database.seriesDao().insertDetail(
            SeriesDetailEntity(
                seriesId = seriesId,
                status = "Ongoing",
                licensed = true,
                completed = false,
                cachedAtEpochMillis = 2L,
            ),
        )
        database.seriesDao().insertAlternativeTitles(
            listOf(
                AlternativeTitleEntity(seriesId, position = 1, title = "ワンピース"),
                AlternativeTitleEntity(seriesId, position = 0, title = "ONE PIECE"),
            ),
        )
        database.seriesDao().insertCategories(
            listOf(CategoryEntity(seriesId, position = 0, name = "Pirates")),
        )
        database.seriesDao().insertContributors(
            listOf(
                ContributorEntity(
                    seriesId = seriesId,
                    position = 0,
                    contributorId = 7L,
                    name = "Eiichiro Oda",
                    role = "Author",
                    url = null,
                ),
            ),
        )
        database.seriesDao().insertPublishers(
            listOf(
                PublisherEntity(
                    seriesId = seriesId,
                    position = 0,
                    publisherId = 10L,
                    name = "Shueisha",
                    type = "Original",
                    notes = null,
                    url = null,
                ),
            ),
        )

        val detail =
            database
                .seriesDao()
                .observeDetail(seriesId)
                .first { it?.detail != null }

        assertNotNull(detail)
        assertEquals(
            listOf("ONE PIECE", "ワンピース"),
            detail?.alternativeTitles?.sortedBy { it.position }?.map { it.title },
        )
        assertEquals("Pirates", detail?.categories?.single()?.name)
        assertEquals("Author", detail?.contributors?.single()?.role)
        assertFalse(checkNotNull(detail?.detail).completed ?: true)
    }

    @Test
    fun recentSearches_upsertSortAndTrimDeterministically() = runTest {
        val dao = database.searchDao()
        dao.insertRecentSearch(RecentSearchEntity("older", "Older", 1L))
        dao.insertRecentSearch(RecentSearchEntity("newer", "Newer", 2L))
        dao.insertRecentSearch(RecentSearchEntity("newest", "Newest", 3L))
        dao.insertRecentSearch(RecentSearchEntity("older", "OLDER", 4L))
        dao.trimRecentSearches(limit = 2)

        val searches = dao.observeRecentSearches(limit = 8).first()

        assertEquals(listOf("OLDER", "Newest"), searches.map { it.displayQuery })
        assertTrue(searches.all { it.normalizedQuery != "newer" })
    }

    private fun searchQuery(): SearchQueryEntity = SearchQueryEntity(
        cacheKey = "query-key",
        query = "one piece",
        types = listOf("Manga"),
        filters = emptyList(),
        totalHits = 2,
        updatedAtEpochMillis = 1L,
    )

    private fun summary(
        seriesId: Long,
        title: String,
    ): SeriesSummaryEntity = SeriesSummaryEntity(
        seriesId = seriesId,
        title = title,
        url = null,
        description = null,
        imageOriginalUrl = null,
        imageThumbnailUrl = null,
        imageWidth = null,
        imageHeight = null,
        type = "Manga",
        year = null,
        rating = null,
        ratingVotes = null,
        genres = emptyList(),
        latestChapter = null,
        apiLastUpdatedEpochSeconds = null,
        cachedAtEpochMillis = 1L,
    )
}
