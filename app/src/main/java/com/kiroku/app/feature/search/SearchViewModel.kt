package com.kiroku.app.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kiroku.app.core.common.UiError
import com.kiroku.app.core.common.toUiError
import com.kiroku.app.core.model.RecentSearch
import com.kiroku.app.core.model.SearchFilter
import com.kiroku.app.core.model.SearchSpec
import com.kiroku.app.core.model.SeriesSummary
import com.kiroku.app.core.model.SeriesTypeFilter
import com.kiroku.app.core.network.NetworkMonitor
import com.kiroku.app.data.repository.CatalogueRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val selectedTypes: Set<SeriesTypeFilter> = emptySet(),
    val selectedFilters: Set<SearchFilter> = emptySet(),
    val recentSearches: List<RecentSearch> = emptyList(),
    val isOffline: Boolean = false,
    val isAwaitingResults: Boolean = false,
    val error: UiError? = null,
) {
    val canSearch: Boolean
        get() = query.trim().length >= SearchSpec.MIN_QUERY_LENGTH
}

class SearchViewModel(
    private val repository: CatalogueRepository,
    networkMonitor: NetworkMonitor,
) : ViewModel() {
    private data class SearchInputs(
        val query: String,
        val settledQuery: String,
        val selectedTypes: Set<SeriesTypeFilter>,
        val selectedFilters: Set<SearchFilter>,
    )

    private val query = MutableStateFlow("")
    private val settledQuery = MutableStateFlow("")
    private val selectedTypes = MutableStateFlow<Set<SeriesTypeFilter>>(emptySet())
    private val selectedFilters = MutableStateFlow<Set<SearchFilter>>(emptySet())
    private val error = MutableStateFlow<UiError?>(null)
    private var querySettlementJob: Job? = null

    private val inputs =
        combine(query, settledQuery, selectedTypes, selectedFilters) { query, settled, types, filters ->
            SearchInputs(
                query = query,
                settledQuery = settled,
                selectedTypes = types,
                selectedFilters = filters,
            )
        }

    private val recentSearches =
        repository
            .observeRecentSearches()
            .catch { failure ->
                error.value = failure.toUiError()
                emit(emptyList())
            }

    private val isOnline =
        networkMonitor.isOnline
            .onStart { emit(true) }
            .catch { emit(true) }

    val uiState =
        combine(inputs, recentSearches, isOnline, error) { inputs, recent, online, error ->
            SearchUiState(
                query = inputs.query,
                selectedTypes = inputs.selectedTypes,
                selectedFilters = inputs.selectedFilters,
                recentSearches = recent,
                isOffline = !online,
                isAwaitingResults =
                inputs.query.trim().length >= SearchSpec.MIN_QUERY_LENGTH &&
                    inputs.query.trim() != inputs.settledQuery,
                error = error,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = SearchUiState(),
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: Flow<PagingData<SeriesSummary>> =
        combine(settledQuery, selectedTypes, selectedFilters) { query, types, filters ->
            SearchSpec(
                query = query,
                types = types,
                filters = filters,
            )
        }.distinctUntilChanged()
            .onEach { spec ->
                if (spec.isSearchable) {
                    try {
                        repository.recordRecentSearch(spec.normalizedQuery)
                    } catch (failure: Throwable) {
                        error.value = failure.toUiError()
                    }
                }
            }.flatMapLatest { spec ->
                if (spec.isSearchable) repository.search(spec) else flowOf(PagingData.empty())
            }.cachedIn(viewModelScope)

    fun updateQuery(value: String) {
        query.value = value
        querySettlementJob?.cancel()
        val normalized = value.trim()
        if (normalized.length < SearchSpec.MIN_QUERY_LENGTH) {
            settledQuery.value = normalized
            return
        }
        querySettlementJob =
            viewModelScope.launch {
                delay(SEARCH_DEBOUNCE_MILLIS)
                settledQuery.value = normalized
            }
    }

    fun submitQuery() {
        querySettlementJob?.cancel()
        settledQuery.value = query.value.trim()
    }

    fun selectRecentSearch(value: String) {
        query.value = value
        submitQuery()
    }

    fun toggleType(type: SeriesTypeFilter) {
        selectedTypes.update { current -> current.toggle(type) }
    }

    fun toggleFilter(filter: SearchFilter) {
        selectedFilters.update { current -> current.toggle(filter) }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            try {
                repository.clearRecentSearches()
            } catch (failure: Throwable) {
                error.value = failure.toUiError()
            }
        }
    }

    fun dismissError() {
        error.value = null
    }

    private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value

    class Factory(
        private val repository: CatalogueRepository,
        private val networkMonitor: NetworkMonitor,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SearchViewModel::class.java)) {
                "Unsupported ViewModel type: " + modelClass.name
            }
            return SearchViewModel(repository, networkMonitor) as T
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 350L
    }
}
