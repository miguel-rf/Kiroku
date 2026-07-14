package com.kiroku.app.feature.search

import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.kiroku.app.core.designsystem.KirokuTheme
import com.kiroku.app.core.model.SeriesSummary
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SearchScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun initialStateExplainsHowToSearch() {
        composeRule.setContent {
            val flow = remember { flowOf(PagingData.empty<SeriesSummary>()) }
            val results = flow.collectAsLazyPagingItems()
            KirokuTheme(dynamicColor = false) {
                SearchScreen(
                    state = SearchUiState(),
                    results = results,
                    selectedSeriesId = null,
                    onQueryChanged = {},
                    onSubmitQuery = {},
                    onRecentSearchSelected = {},
                    onClearRecentSearches = {},
                    onTypeToggled = {},
                    onFilterToggled = {},
                    onSeriesSelected = {},
                    onDismissError = {},
                )
            }
        }

        composeRule
            .onNodeWithText("Find your next series")
            .assertIsDisplayed()
    }

    @Test
    fun cachedResultCanBeSelectedWhileOffline() {
        var selectedId: Long? = null
        composeRule.setContent {
            val flow = remember { flowOf(PagingData.from(listOf(summary()))) }
            val results = flow.collectAsLazyPagingItems()
            KirokuTheme(dynamicColor = false) {
                SearchScreen(
                    state =
                    SearchUiState(
                        query = "One Piece",
                        isOffline = true,
                    ),
                    results = results,
                    selectedSeriesId = null,
                    onQueryChanged = {},
                    onSubmitQuery = {},
                    onRecentSearchSelected = {},
                    onClearRecentSearches = {},
                    onTypeToggled = {},
                    onFilterToggled = {},
                    onSeriesSelected = { selectedId = it },
                    onDismissError = {},
                )
            }
        }

        composeRule.onNodeWithText("Offline · showing saved content").assertIsDisplayed()
        composeRule
            .onNode(hasText("One Piece") and hasClickAction())
            .performClick()

        composeRule.runOnIdle {
            assertEquals(SERIES_ID, selectedId)
        }
    }

    private fun summary(): SeriesSummary = SeriesSummary(
        id = SERIES_ID,
        title = "One Piece",
        url = null,
        description = null,
        cover = null,
        type = "Manga",
        year = "1997",
        rating = 8.9,
        ratingVotes = 100,
        genres = listOf("Action"),
        latestChapter = 1120,
        lastUpdatedEpochSeconds = null,
    )

    private companion object {
        const val SERIES_ID = 55_099_564_912L
    }
}
