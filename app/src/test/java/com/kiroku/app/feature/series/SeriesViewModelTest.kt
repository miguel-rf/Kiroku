package com.kiroku.app.feature.series

import com.kiroku.app.core.common.UiError
import com.kiroku.app.core.model.Contributor
import com.kiroku.app.core.model.CoverImage
import com.kiroku.app.core.model.Publisher
import com.kiroku.app.core.model.SeriesDetails
import com.kiroku.app.core.model.SeriesSummary
import com.kiroku.app.core.network.AppFailure
import com.kiroku.app.core.network.FailureKind
import com.kiroku.app.test.FakeCatalogueRepository
import com.kiroku.app.test.FakeNetworkMonitor
import com.kiroku.app.test.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SeriesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun cachedContent_remainsVisibleWhenRefreshFailsOffline() = runTest {
        val repository =
            FakeCatalogueRepository().apply {
                series.value = sampleDetails()
                refreshBlock = {
                    throw AppFailure(FailureKind.OFFLINE)
                }
            }
        val networkMonitor = FakeNetworkMonitor(initialOnline = false)
        val viewModel = SeriesViewModel(SERIES_ID, repository, networkMonitor)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        advanceUntilIdle()
        val state = viewModel.uiState.value

        assertNotNull(state.content)
        assertEquals("One Piece", state.content?.title)
        assertEquals(UiError.OFFLINE, state.error)
        assertTrue(state.isOffline)
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertEquals(listOf(SERIES_ID), repository.requestedSeriesIds)
    }

    @Test
    fun successfulRetry_clearsThePreviousRefreshError() = runTest {
        var failRefresh = true
        val repository =
            FakeCatalogueRepository().apply {
                series.value = sampleDetails()
                refreshBlock = {
                    if (failRefresh) throw AppFailure(FailureKind.SERVER)
                }
            }
        val viewModel = SeriesViewModel(SERIES_ID, repository, FakeNetworkMonitor())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()
        assertEquals(UiError.SERVER, viewModel.uiState.value.error)

        failRefresh = false
        viewModel.refresh()
        runCurrent()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isRefreshing)
        assertEquals(2, repository.requestedSeriesIds.size)
    }

    private fun sampleDetails(): SeriesDetails = SeriesDetails(
        summary =
        SeriesSummary(
            id = SERIES_ID,
            title = "One Piece",
            url = null,
            description = "**Pirate** adventure",
            cover = CoverImage(null, null, null, null),
            type = "Manga",
            year = "1997",
            rating = 8.9,
            ratingVotes = 100,
            genres = listOf("Action"),
            latestChapter = 1120,
            lastUpdatedEpochSeconds = null,
        ),
        alternativeTitles = listOf("ONE PIECE"),
        categories = listOf("Pirates"),
        contributors =
        listOf(
            Contributor(
                id = 7L,
                name = "Eiichiro Oda",
                role = "Author",
                url = null,
            ),
        ),
        publishers =
        listOf(
            Publisher(
                id = 10L,
                name = "Shueisha",
                type = "Original",
                notes = null,
                url = null,
            ),
        ),
        status = "Ongoing",
        licensed = true,
        completed = false,
        isFullDetailCached = true,
    )

    private companion object {
        const val SERIES_ID = 55_099_564_912L
    }
}
