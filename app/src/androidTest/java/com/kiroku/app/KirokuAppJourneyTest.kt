package com.kiroku.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.paging.PagingData
import androidx.test.platform.app.InstrumentationRegistry
import com.kiroku.app.core.designsystem.KirokuTheme
import com.kiroku.app.core.model.Contributor
import com.kiroku.app.core.model.Publisher
import com.kiroku.app.core.model.RecentSearch
import com.kiroku.app.core.model.SearchSpec
import com.kiroku.app.core.model.SeriesDetails
import com.kiroku.app.core.model.SeriesSummary
import com.kiroku.app.core.network.AppFailure
import com.kiroku.app.core.network.FailureKind
import com.kiroku.app.core.network.NetworkMonitor
import com.kiroku.app.data.repository.CatalogueRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class KirokuAppJourneyTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun searchResult_opensCachedDetailsAndUsesExpectedPaneNavigation() {
        val repository = JourneyRepository()
        setAppContent(container(repository))

        openCachedResult()

        assertCachedDetailsDisplayed()
        if (isListDetailWindow()) {
            composeRule.onNode(hasContentDescription("Back")).assertDoesNotExist()
            composeRule.onNode(hasSetTextAction()).assertIsDisplayed()
        } else {
            composeRule.onNode(hasContentDescription("Back")).performClick()
            composeRule.onNode(hasSetTextAction()).assertIsDisplayed()
        }
    }

    @Test
    fun selectedSeries_survivesSavedInstanceStateRestore() {
        val repository = JourneyRepository()
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            AppUnderTest(container(repository))
        }

        openCachedResult()
        assertCachedDetailsDisplayed()

        restorationTester.emulateSavedInstanceStateRestore()

        assertCachedDetailsDisplayed()
        val backButton = composeRule.onNode(hasContentDescription("Back"))
        if (isListDetailWindow()) {
            backButton.assertDoesNotExist()
        } else {
            backButton.assertIsDisplayed()
        }
    }

    @Test
    fun cachedDetailsRemainVisibleOfflineAndRetryRecovers() {
        val repository = JourneyRepository(refreshShouldFail = true)
        val isOnline = MutableStateFlow(false)
        setAppContent(container(repository, isOnline))

        openCachedResult()
        assertCachedDetailsDisplayed()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule
                .onAllNodes(hasText("Offline · showing saved content"))
                .fetchSemanticsNodes()
                .isNotEmpty() &&
                composeRule
                    .onAllNodes(hasText("You are offline"))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
        }

        repository.refreshShouldFail = false
        isOnline.value = true
        composeRule.onNode(hasText("Try again") and hasClickAction()).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            repository.refreshAttempts.get() >= 2 &&
                composeRule
                    .onAllNodes(hasText("You are offline"))
                    .fetchSemanticsNodes()
                    .isEmpty() &&
                composeRule
                    .onAllNodes(hasText("Offline · showing saved content"))
                    .fetchSemanticsNodes()
                    .isEmpty()
        }
        assertCachedDetailsDisplayed()
    }

    private fun setAppContent(container: AppContainer) {
        composeRule.setContent { AppUnderTest(container) }
    }

    @androidx.compose.runtime.Composable
    private fun AppUnderTest(container: AppContainer) {
        KirokuTheme(dynamicColor = false) {
            KirokuApp(
                container = container,
                onExit = {},
            )
        }
    }

    private fun openCachedResult() {
        composeRule.onNode(hasSetTextAction()).performTextInput("One Piece")
        composeRule.onNode(hasSetTextAction()).performImeAction()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule
                .onAllNodes(
                    hasText("One Piece") and
                        hasClickAction() and
                        hasSetTextAction().not(),
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule
            .onNode(
                hasText("One Piece") and
                    hasClickAction() and
                    hasSetTextAction().not(),
            )
            .performClick()
    }

    private fun assertCachedDetailsDisplayed() {
        composeRule.onNode(hasText("Series details")).assertIsDisplayed()
        composeRule.onNode(hasText("No cover available")).assertIsDisplayed()
    }

    private fun container(
        repository: JourneyRepository,
        isOnline: Flow<Boolean> = flowOf(true),
    ): AppContainer = object : AppContainer {
        override val catalogueRepository: CatalogueRepository = repository
        override val networkMonitor: NetworkMonitor =
            object : NetworkMonitor {
                override val isOnline: Flow<Boolean> = isOnline
            }
    }

    private fun isListDetailWindow(): Boolean = InstrumentationRegistry
        .getInstrumentation()
        .targetContext
        .resources
        .configuration
        .screenWidthDp >= 600

    private class JourneyRepository(
        @Volatile var refreshShouldFail: Boolean = false,
    ) : CatalogueRepository {
        val refreshAttempts = AtomicInteger()

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

        override fun search(spec: SearchSpec): Flow<PagingData<SeriesSummary>> {
            check(spec.normalizedQuery == "One Piece") { "Unexpected search query: ${spec.query}" }
            check(spec.types.isEmpty()) { "Unexpected search types: ${spec.types}" }
            check(spec.filters.isEmpty()) { "Unexpected search filters: ${spec.filters}" }
            return flowOf(PagingData.from(listOf(summary)))
        }

        override fun observeRecentSearches(): Flow<List<RecentSearch>> = flowOf(emptyList())

        override suspend fun recordRecentSearch(query: String) = Unit

        override suspend fun clearRecentSearches() = Unit

        override fun observeSeries(seriesId: Long): Flow<SeriesDetails?> {
            check(seriesId == SERIES_ID) { "Unexpected selected series ID: $seriesId" }
            return detail
        }

        override suspend fun refreshSeries(seriesId: Long) {
            check(seriesId == SERIES_ID) { "Unexpected refreshed series ID: $seriesId" }
            refreshAttempts.incrementAndGet()
            if (refreshShouldFail) {
                throw AppFailure(FailureKind.OFFLINE, "Device validation offline fixture")
            }
        }
    }

    private companion object {
        const val SERIES_ID = 55_099_564_912L
    }
}
