package com.kiroku.app.feature.search

import androidx.paging.PagingData
import com.kiroku.app.test.FakeCatalogueRepository
import com.kiroku.app.test.FakeNetworkMonitor
import com.kiroku.app.test.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun query_isDebouncedWithoutArbitraryWaiting() = runTest {
        val repository = FakeCatalogueRepository()
        val networkMonitor = FakeNetworkMonitor()
        val viewModel = SearchViewModel(repository, networkMonitor)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.searchResults.collect()
        }

        viewModel.updateQuery("One Piece")
        runCurrent()
        assertTrue(viewModel.uiState.value.isAwaitingResults)

        advanceTimeBy(349L)
        runCurrent()
        assertTrue(repository.searchSpecs.isEmpty())

        advanceTimeBy(1L)
        runCurrent()

        assertEquals("One Piece", repository.searchSpecs.single().normalizedQuery)
        assertEquals(listOf("One Piece"), repository.recordedRecentSearches)
        assertFalse(viewModel.uiState.value.isAwaitingResults)
    }

    @Test
    fun newerQuery_cancelsTheStalePagingStream() = runTest {
        val repository = FakeCatalogueRepository()
        val canceledQueries = mutableListOf<String>()
        repository.searchFlow = { spec ->
            flow {
                try {
                    emit(PagingData.empty())
                    awaitCancellation()
                } finally {
                    canceledQueries += spec.normalizedQuery
                }
            }
        }
        val viewModel = SearchViewModel(repository, FakeNetworkMonitor())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.searchResults.collect()
        }

        viewModel.updateQuery("One Piece")
        advanceTimeBy(350L)
        runCurrent()
        viewModel.updateQuery("Naruto")
        advanceTimeBy(350L)
        runCurrent()

        assertTrue("One Piece" in canceledQueries)
        assertEquals("Naruto", repository.searchSpecs.last().normalizedQuery)
    }

    @Test
    fun networkState_isExposedWithoutChangingCachedResults() = runTest {
        val networkMonitor = FakeNetworkMonitor(initialOnline = true)
        val viewModel = SearchViewModel(FakeCatalogueRepository(), networkMonitor)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        runCurrent()

        networkMonitor.online.value = false
        runCurrent()

        assertTrue(viewModel.uiState.value.isOffline)
    }
}
