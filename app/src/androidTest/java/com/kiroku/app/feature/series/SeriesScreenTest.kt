package com.kiroku.app.feature.series

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import com.kiroku.app.core.common.UiError
import com.kiroku.app.core.designsystem.KirokuTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SeriesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun compactDetailShowsMetadataAndBackAction() {
        var backClicked = false
        composeRule.setContent {
            KirokuTheme(dynamicColor = false) {
                SeriesScreen(
                    state =
                    SeriesUiState(
                        seriesId = SERIES_ID,
                        content = detail(),
                        isLoading = false,
                    ),
                    onBack = { backClicked = true },
                    onRefresh = {},
                    showBackButton = true,
                )
            }
        }

        composeRule.onNodeWithText("One Piece").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToIndex(2)
        composeRule.onNodeWithText("A pirate adventure").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.runOnIdle { assertTrue(backClicked) }
    }

    @Test
    fun listDetailPaneHidesBackButton() {
        composeRule.setContent {
            KirokuTheme(dynamicColor = false) {
                SeriesScreen(
                    state =
                    SeriesUiState(
                        seriesId = SERIES_ID,
                        content = detail(),
                        isLoading = false,
                    ),
                    onBack = {},
                    onRefresh = {},
                    showBackButton = false,
                )
            }
        }

        composeRule.onNodeWithContentDescription("Back").assertDoesNotExist()
        composeRule.onNodeWithText("One Piece").assertIsDisplayed()
    }

    @Test
    fun fatalOfflineStateOffersRetry() {
        var retried = false
        composeRule.setContent {
            KirokuTheme(dynamicColor = false) {
                SeriesScreen(
                    state =
                    SeriesUiState(
                        seriesId = SERIES_ID,
                        isLoading = false,
                        error = UiError.OFFLINE,
                    ),
                    onBack = {},
                    onRefresh = { retried = true },
                    showBackButton = true,
                )
            }
        }

        composeRule.onNodeWithText("You are offline").assertIsDisplayed()
        composeRule.onNodeWithText("Try again").performClick()
        composeRule.runOnIdle { assertTrue(retried) }
    }

    private fun detail(): SeriesDetailsUiModel = SeriesDetailsUiModel(
        id = SERIES_ID,
        title = "One Piece",
        coverUrl = null,
        description = "A pirate adventure",
        type = "Manga",
        year = "1997",
        rating = 8.9,
        ratingVotes = 100,
        genres = listOf("Action", "Adventure"),
        latestChapter = 1120,
        alternativeTitles = listOf("ONE PIECE"),
        categories = listOf("Pirates"),
        authors = listOf("Eiichiro Oda"),
        artists = listOf("Eiichiro Oda"),
        otherContributors = emptyList(),
        publishers =
        listOf(
            SeriesPublisherUiModel(
                name = "Shueisha",
                type = "Original",
                notes = null,
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
