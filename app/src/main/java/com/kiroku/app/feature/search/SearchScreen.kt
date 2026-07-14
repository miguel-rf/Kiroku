package com.kiroku.app.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.kiroku.app.R
import com.kiroku.app.core.common.toUiError
import com.kiroku.app.core.designsystem.ErrorPane
import com.kiroku.app.core.designsystem.InlineErrorBanner
import com.kiroku.app.core.designsystem.LoadingPane
import com.kiroku.app.core.designsystem.OfflineBanner
import com.kiroku.app.core.model.SearchFilter
import com.kiroku.app.core.model.SeriesSummary
import com.kiroku.app.core.model.SeriesTypeFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    state: SearchUiState,
    results: LazyPagingItems<SeriesSummary>,
    selectedSeriesId: Long?,
    onQueryChanged: (String) -> Unit,
    onSubmitQuery: () -> Unit,
    onRecentSearchSelected: (String) -> Unit,
    onClearRecentSearches: () -> Unit,
    onTypeToggled: (SeriesTypeFilter) -> Unit,
    onFilterToggled: (SearchFilter) -> Unit,
    onSeriesSelected: (Long) -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.search_title))
                        Text(
                            text = stringResource(R.string.unofficial_attribution),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = results::refresh,
                        enabled =
                        state.canSearch &&
                            results.loadState.refresh !is LoadState.Loading,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = stringResource(R.string.action_refresh),
                        )
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            SearchControls(
                state = state,
                onQueryChanged = onQueryChanged,
                onSubmitQuery = onSubmitQuery,
                onRecentSearchSelected = onRecentSearchSelected,
                onClearRecentSearches = onClearRecentSearches,
                onTypeToggled = onTypeToggled,
                onFilterToggled = onFilterToggled,
            )
            if (state.error != null) {
                InlineErrorBanner(
                    error = state.error,
                    onAction = onDismissError,
                    actionLabelRes = R.string.action_close,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            SearchResults(
                state = state,
                results = results,
                selectedSeriesId = selectedSeriesId,
                onSeriesSelected = onSeriesSelected,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SearchControls(
    state: SearchUiState,
    onQueryChanged: (String) -> Unit,
    onSubmitQuery: () -> Unit,
    onRecentSearchSelected: (String) -> Unit,
    onClearRecentSearches: () -> Unit,
    onTypeToggled: (SeriesTypeFilter) -> Unit,
    onFilterToggled: (SearchFilter) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.search_hint)) },
            supportingText = { Text(stringResource(R.string.search_supporting_text)) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChanged("") }) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.search_clear_query),
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmitQuery() }),
        )

        Text(
            modifier = Modifier.semantics { heading() },
            text = stringResource(R.string.search_filters),
            style = MaterialTheme.typography.titleSmall,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(SeriesTypeFilter.entries, key = SeriesTypeFilter::name) { type ->
                FilterChip(
                    selected = type in state.selectedTypes,
                    onClick = { onTypeToggled(type) },
                    label = { Text(stringResource(type.labelRes)) },
                )
            }
            items(SearchFilter.entries, key = SearchFilter::name) { filter ->
                FilterChip(
                    selected = filter in state.selectedFilters,
                    onClick = { onFilterToggled(filter) },
                    label = { Text(stringResource(filter.labelRes)) },
                )
            }
        }

        if (state.query.isBlank() && state.recentSearches.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    modifier = Modifier.semantics { heading() },
                    text = stringResource(R.string.search_recent),
                    style = MaterialTheme.typography.titleSmall,
                )
                IconButton(onClick = onClearRecentSearches) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.search_clear_recent),
                    )
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(
                    items = state.recentSearches,
                    key = { it.query.lowercase() },
                ) { recent ->
                    AssistChip(
                        onClick = { onRecentSearchSelected(recent.query) },
                        label = { Text(recent.query) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResults(
    state: SearchUiState,
    results: LazyPagingItems<SeriesSummary>,
    selectedSeriesId: Long?,
    onSeriesSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val refreshState = results.loadState.refresh
    when {
        !state.canSearch -> {
            SearchPrompt(
                showMinimumMessage = state.query.isNotBlank(),
                modifier = modifier.fillMaxSize(),
            )
        }

        state.isAwaitingResults -> {
            LoadingPane(
                messageRes = R.string.search_loading,
                modifier = modifier.fillMaxSize(),
            )
        }

        refreshState is LoadState.Loading && results.itemCount == 0 -> {
            LoadingPane(
                messageRes = R.string.search_loading,
                modifier = modifier.fillMaxSize(),
            )
        }

        refreshState is LoadState.Error && results.itemCount == 0 -> {
            ErrorPane(
                error = refreshState.error.toUiError(),
                onRetry = results::retry,
                modifier = modifier.fillMaxSize(),
            )
        }

        refreshState is LoadState.NotLoading && results.itemCount == 0 -> {
            EmptySearch(modifier = modifier.fillMaxSize())
        }

        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.isOffline) {
                    item(key = "offline") { OfflineBanner() }
                }
                if (refreshState is LoadState.Loading) {
                    item(key = "refresh-progress") {
                        LinearProgressIndicator(
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                }
                if (refreshState is LoadState.Error) {
                    item(key = "refresh-error") {
                        InlineErrorBanner(
                            error = refreshState.error.toUiError(),
                            onAction = results::retry,
                        )
                    }
                }
                items(
                    count = results.itemCount,
                    key = results.itemKey(SeriesSummary::id),
                ) { index ->
                    val series = results[index] ?: return@items
                    SeriesSearchCard(
                        series = series,
                        selected = series.id == selectedSeriesId,
                        onClick = { onSeriesSelected(series.id) },
                    )
                }
                when (val appendState = results.loadState.append) {
                    is LoadState.Loading -> {
                        item(key = "append-loading") {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(stringResource(R.string.search_append_loading))
                            }
                        }
                    }

                    is LoadState.Error -> {
                        item(key = "append-error") {
                            InlineErrorBanner(
                                error = appendState.error.toUiError(),
                                onAction = results::retry,
                            )
                        }
                    }

                    is LoadState.NotLoading -> Unit
                }
            }
        }
    }
}

@Composable
private fun SearchPrompt(
    showMinimumMessage: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.MenuBook,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            modifier = Modifier.semantics { heading() },
            text = stringResource(R.string.search_prompt_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text =
            stringResource(
                if (showMinimumMessage) R.string.search_minimum else R.string.search_prompt_message,
            ),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun EmptySearch(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            modifier = Modifier.semantics { heading() },
            text = stringResource(R.string.search_empty_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.search_empty_message),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun SeriesSearchCard(
    series: SeriesSummary,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier =
        Modifier
            .fillMaxWidth()
            .semantics { this.selected = selected },
        colors =
        CardDefaults.cardColors(
            containerColor =
            if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val coverUrl = series.cover?.thumbnailUrl ?: series.cover?.originalUrl
            Box(
                modifier =
                Modifier
                    .size(width = 72.dp, height = 104.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center,
            ) {
                if (coverUrl == null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    val context = LocalContext.current
                    val request =
                        remember(context, coverUrl) {
                            ImageRequest
                                .Builder(context)
                                .data(coverUrl)
                                .crossfade(true)
                                .build()
                        }
                    AsyncImage(
                        model = request,
                        contentDescription =
                        stringResource(R.string.search_result_cover, series.title),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = series.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                )
                val metadata = listOfNotNull(series.type, series.year)
                if (metadata.isNotEmpty()) {
                    Text(
                        text = metadata.joinToString(stringResource(R.string.metadata_separator)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (series.genres.isNotEmpty()) {
                    Text(
                        text =
                        series.genres
                            .take(3)
                            .joinToString(stringResource(R.string.list_separator)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
                series.latestChapter?.let { chapter ->
                    Text(
                        text = stringResource(R.string.search_result_latest_chapter, chapter),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                series.rating?.let { rating ->
                    Text(
                        text = stringResource(R.string.search_result_rating, rating),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = stringResource(R.string.search_selected),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        HorizontalDivider(color = Color.Transparent)
    }
}

private val SeriesTypeFilter.labelRes: Int
    get() =
        when (this) {
            SeriesTypeFilter.MANGA -> R.string.series_type_manga
            SeriesTypeFilter.MANHWA -> R.string.series_type_manhwa
            SeriesTypeFilter.MANHUA -> R.string.series_type_manhua
            SeriesTypeFilter.NOVEL -> R.string.series_type_novel
        }

private val SearchFilter.labelRes: Int
    get() =
        when (this) {
            SearchFilter.COMPLETED -> R.string.filter_completed
            SearchFilter.SOME_RELEASES -> R.string.filter_some_releases
        }
