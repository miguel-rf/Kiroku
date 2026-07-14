package com.kiroku.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchSpecTest {
    @Test
    fun cacheKey_isStableAcrossCaseAndSetOrder() {
        val first =
            SearchSpec(
                query = " One Piece ",
                types = linkedSetOf(SeriesTypeFilter.MANGA, SeriesTypeFilter.MANHWA),
                filters = linkedSetOf(SearchFilter.SOME_RELEASES, SearchFilter.COMPLETED),
            )
        val second =
            SearchSpec(
                query = "one piece",
                types = linkedSetOf(SeriesTypeFilter.MANHWA, SeriesTypeFilter.MANGA),
                filters = linkedSetOf(SearchFilter.COMPLETED, SearchFilter.SOME_RELEASES),
            )

        assertEquals(first.cacheKey, second.cacheKey)
        assertTrue(first.isSearchable)
    }

    @Test
    fun cacheKey_changesWhenAFilterChanges() {
        val unfiltered = SearchSpec(query = "One Piece")
        val filtered =
            SearchSpec(
                query = "One Piece",
                filters = setOf(SearchFilter.COMPLETED),
            )

        assertNotEquals(unfiltered.cacheKey, filtered.cacheKey)
    }
}
