package com.kiroku.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.window.core.layout.WindowSizeClass
import com.kiroku.app.feature.search.SearchScreen
import com.kiroku.app.feature.search.SearchViewModel
import com.kiroku.app.feature.series.SeriesScreen
import com.kiroku.app.feature.series.SeriesSelectionPrompt
import com.kiroku.app.feature.series.SeriesViewModel
import kotlinx.serialization.Serializable

@Serializable
data object SearchRoute : NavKey

@Serializable
data class SeriesRoute(
    val seriesId: Long,
) : NavKey {
    init {
        require(seriesId > 0L) { "A positive series ID is required" }
    }
}

@Composable
fun KirokuApp(
    container: AppContainer,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(SearchRoute)
    val searchFactory =
        remember(container) {
            SearchViewModel.Factory(
                repository = container.catalogueRepository,
                networkMonitor = container.networkMonitor,
            )
        }
    val searchViewModel: SearchViewModel = viewModel(factory = searchFactory)
    val searchState by searchViewModel.uiState.collectAsStateWithLifecycle()
    val searchResults = searchViewModel.searchResults.collectAsLazyPagingItems()
    val isListDetail =
        currentWindowAdaptiveInfo()
            .windowSizeClass
            .isListDetailLayout()
    val selectedSeriesId = (backStack.lastOrNull() as? SeriesRoute)?.seriesId

    fun selectSeries(seriesId: Long) {
        val route = SeriesRoute(seriesId)
        if (backStack.lastOrNull() == route) return
        if (backStack.lastOrNull() is SeriesRoute) {
            backStack.removeLastOrNull()
        }
        backStack.add(route)
    }

    fun navigateBack() {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        } else {
            onExit()
        }
    }

    Row(modifier = modifier.fillMaxSize()) {
        if (isListDetail) {
            Box(modifier = Modifier.weight(LIST_PANE_WEIGHT)) {
                SearchScreen(
                    state = searchState,
                    results = searchResults,
                    selectedSeriesId = selectedSeriesId,
                    onQueryChanged = searchViewModel::updateQuery,
                    onSubmitQuery = searchViewModel::submitQuery,
                    onRecentSearchSelected = searchViewModel::selectRecentSearch,
                    onClearRecentSearches = searchViewModel::clearRecentSearches,
                    onTypeToggled = searchViewModel::toggleType,
                    onFilterToggled = searchViewModel::toggleFilter,
                    onSeriesSelected = ::selectSeries,
                    onDismissError = searchViewModel::dismissError,
                )
            }
            VerticalDivider()
        }

        Box(modifier = Modifier.weight(if (isListDetail) DETAIL_PANE_WEIGHT else 1f)) {
            AppNavDisplay(
                backStack = backStack,
                container = container,
                isListDetail = isListDetail,
                searchContent = {
                    SearchScreen(
                        state = searchState,
                        results = searchResults,
                        selectedSeriesId = selectedSeriesId,
                        onQueryChanged = searchViewModel::updateQuery,
                        onSubmitQuery = searchViewModel::submitQuery,
                        onRecentSearchSelected = searchViewModel::selectRecentSearch,
                        onClearRecentSearches = searchViewModel::clearRecentSearches,
                        onTypeToggled = searchViewModel::toggleType,
                        onFilterToggled = searchViewModel::toggleFilter,
                        onSeriesSelected = ::selectSeries,
                        onDismissError = searchViewModel::dismissError,
                    )
                },
                onBack = ::navigateBack,
            )
        }
    }
}

@Composable
private fun AppNavDisplay(
    backStack: NavBackStack<NavKey>,
    container: AppContainer,
    isListDetail: Boolean,
    searchContent: @Composable () -> Unit,
    onBack: () -> Unit,
) {
    NavDisplay(
        backStack = backStack,
        onBack = onBack,
        entryDecorators =
        listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider =
        entryProvider {
            entry<SearchRoute> {
                if (isListDetail) {
                    SeriesSelectionPrompt(modifier = Modifier.fillMaxSize())
                } else {
                    searchContent()
                }
            }
            entry<SeriesRoute> { route ->
                val factory =
                    remember(route.seriesId, container) {
                        SeriesViewModel.Factory(
                            seriesId = route.seriesId,
                            repository = container.catalogueRepository,
                            networkMonitor = container.networkMonitor,
                        )
                    }
                val seriesViewModel: SeriesViewModel = viewModel(factory = factory)
                val state by seriesViewModel.uiState.collectAsStateWithLifecycle()
                SeriesScreen(
                    state = state,
                    onBack = onBack,
                    onRefresh = seriesViewModel::refresh,
                    showBackButton = !isListDetail,
                )
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

private const val LIST_PANE_WEIGHT = 0.42f
private const val DETAIL_PANE_WEIGHT = 0.58f

internal fun WindowSizeClass.isListDetailLayout(): Boolean = isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
