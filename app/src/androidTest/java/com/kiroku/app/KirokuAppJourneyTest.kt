package com.kiroku.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.paging.PagingData
import com.kiroku.app.core.designsystem.KirokuTheme
import com.kiroku.app.core.model.Contributor
import com.kiroku.app.core.model.Publisher
import com.kiroku.app.core.model.RecentSearch
import com.kiroku.app.core.model.SearchSpec
import com.kiroku.app.core.model.SeriesDetails
import com.kiroku.app.core.model.SeriesSummary
import com.kiroku.app.core.network.NetworkMonitor
import com.kiroku.app.data.repository.CatalogueRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

class KirokuAppJourneyTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun searchResult_opensCachedSeriesDetails() {
        val repository = JourneyRepository()
        val container =
            object : AppContainer {
                override val catalogueRepository: CatalogueRepository = repository
                override val networkMonitor: NetworkMonitor =
                    object : NetworkMonitor {
                        override val isOnline: Flow<Boolean> = flowOf(true)
                    }
            }
        composeRule.setContent {
            KirokuTheme(dynamicColor = false) {
                KirokuApp(
                    container = container,
                    onExit = {},
                )
            }
        }

        composeRule
            .onNode(hasSetTextAction())
            .performTextInput("One Piece")
        composeRule
            .onNode(hasSetTextAction())
            .performImeAction()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule
                .onAllNodes(hasText("One Piece") and hasClickAction())
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule
            .onNode(hasText("One Piece") and hasClickAction())
            .performClick()

        composeRule.onNodeWithText("Series details").assertIsDisplayed()
        composeRule.onNodeWithText("A pirate adventure").assertIsDisplayed()
    }

    private class JourneyRepository : CatalogueRepository {
        private val summary =
            SeriesSummary(
                id = SERIES_ID,
                title = "One Piece",
                url = null,
                description = "A pirate adventure",
                cover = null,
                type = "Manga",
                year = "1997",
                rating = 8.9,
                ratingVotes = 100,
                genres = listOf("Action"),
                latestChapter = 1120,
                lastUpdatedEpochSeconds = null,
            )
        private val detail =
            MutableStateFlow<SeriesDetails?>(
                SeriesDetails(
                    summary = summary,
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
                ),
            )

        override fun search(spec: SearchSpec): Flow<PagingData<SeriesSummary>> = flowOf(PagingData.from(listOf(summary)))

        override fun observeRecentSearches(): Flow<List<RecentSearch>> = flowOf(emptyList())

        override suspend fun recordRecentSearch(query: String) = Unit

        override suspend fun clearRecentSearches() = Unit

        override fun observeSeries(seriesId: Long): Flow<SeriesDetails?> = detail

        override suspend fun refreshSeries(seriesId: Long) = Unit
    }

    private companion object {
        const val SERIES_ID = 55_099_564_912L
    }
}
