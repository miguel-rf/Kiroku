package com.kiroku.app.test

import androidx.paging.PagingData
import com.kiroku.app.core.common.Clock
import com.kiroku.app.core.model.RecentSearch
import com.kiroku.app.core.model.SearchSpec
import com.kiroku.app.core.model.SeriesDetails
import com.kiroku.app.core.model.SeriesSummary
import com.kiroku.app.core.network.NetworkMonitor
import com.kiroku.app.data.repository.CatalogueRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class MutableTestClock(
    var now: Long,
) : Clock {
    override fun nowEpochMillis(): Long = now
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

class FakeNetworkMonitor(
    initialOnline: Boolean = true,
) : NetworkMonitor {
    val online = MutableStateFlow(initialOnline)
    override val isOnline: Flow<Boolean> = online
}

class FakeCatalogueRepository : CatalogueRepository {
    val searchSpecs = mutableListOf<SearchSpec>()
    val recordedRecentSearches = mutableListOf<String>()
    val requestedSeriesIds = mutableListOf<Long>()
    val recentSearches = MutableStateFlow<List<RecentSearch>>(emptyList())
    val series = MutableStateFlow<SeriesDetails?>(null)

    var searchFlow: (SearchSpec) -> Flow<PagingData<SeriesSummary>> = {
        flowOf(PagingData.empty())
    }
    var refreshBlock: suspend (Long) -> Unit = {}

    override fun search(spec: SearchSpec): Flow<PagingData<SeriesSummary>> {
        searchSpecs += spec
        return searchFlow(spec)
    }

    override fun observeRecentSearches(): Flow<List<RecentSearch>> = recentSearches

    override suspend fun recordRecentSearch(query: String) {
        recordedRecentSearches += query
    }

    override suspend fun clearRecentSearches() {
        recentSearches.value = emptyList()
    }

    override fun observeSeries(seriesId: Long): Flow<SeriesDetails?> = series

    override suspend fun refreshSeries(seriesId: Long) {
        requestedSeriesIds += seriesId
        refreshBlock(seriesId)
    }
}
