package com.kiroku.app.feature.series

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.kiroku.app.R
import com.kiroku.app.core.designsystem.ErrorPane
import com.kiroku.app.core.designsystem.InlineErrorBanner
import com.kiroku.app.core.designsystem.LoadingPane
import com.kiroku.app.core.designsystem.OfflineBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesScreen(
    state: SeriesUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    showBackButton: Boolean,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.series_details_title)) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onRefresh,
                        enabled = !state.isRefreshing,
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
        when {
            state.isLoading -> {
                LoadingPane(
                    messageRes = R.string.series_loading,
                    modifier = Modifier.fillMaxSize().padding(contentPadding),
                )
            }

            state.content == null && state.error != null -> {
                ErrorPane(
                    error = state.error,
                    onRetry = onRefresh,
                    modifier = Modifier.fillMaxSize().padding(contentPadding),
                )
            }

            state.content != null -> {
                SeriesContent(
                    state = state,
                    onRefresh = onRefresh,
                    contentPadding = contentPadding,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
fun SeriesSelectionPrompt(modifier: Modifier = Modifier) {
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
            text = stringResource(R.string.series_select_prompt),
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun SeriesContent(
    state: SeriesUiState,
    onRefresh: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val content = checkNotNull(state.content)
    var categoriesExpanded by rememberSaveable(content.id) { mutableStateOf(false) }
    LazyColumn(
        modifier = modifier,
        contentPadding =
        PaddingValues(
            start = 20.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding() + 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.isOffline) {
            item(key = "offline") { OfflineBanner() }
        }
        if (state.isRefreshing) {
            item(key = "refresh") { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        }
        if (!content.isFullDetailCached) {
            item(key = "cached-summary") {
                Card(
                    colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.cached_summary_banner),
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
        state.error?.let { error ->
            item(key = "error") {
                InlineErrorBanner(error = error, onAction = onRefresh)
            }
        }
        item(key = "header") { SeriesHeader(content) }

        item(key = "information") {
            SeriesSection(title = stringResource(R.string.series_information)) {
                content.type?.let {
                    LabelValue(stringResource(R.string.series_type), it)
                }
                content.year?.let {
                    LabelValue(stringResource(R.string.series_year), it)
                }
                content.status?.let {
                    LabelValue(stringResource(R.string.series_status), it)
                }
                content.latestChapter?.let {
                    LabelValue(
                        stringResource(R.string.series_latest_chapter),
                        it.toString(),
                    )
                }
                if (content.rating != null) {
                    LabelValue(
                        label = stringResource(R.string.series_rating),
                        value =
                        if (content.ratingVotes != null) {
                            pluralStringResource(
                                R.plurals.series_rating_value,
                                content.ratingVotes,
                                content.rating,
                                content.ratingVotes,
                            )
                        } else {
                            stringResource(R.string.series_rating_only, content.rating)
                        },
                    )
                }
                content.licensed?.let {
                    LabelValue(
                        stringResource(R.string.series_licensed),
                        stringResource(if (it) R.string.series_yes else R.string.series_no),
                    )
                }
                content.completed?.let {
                    LabelValue(
                        stringResource(R.string.series_completed),
                        stringResource(if (it) R.string.series_yes else R.string.series_no),
                    )
                }
            }
        }

        content.description?.let { description ->
            item(key = "description") {
                SeriesSection(title = stringResource(R.string.series_description)) {
                    Text(description, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        if (content.alternativeTitles.isNotEmpty()) {
            item(key = "alternative-titles") {
                TextListSection(
                    title = stringResource(R.string.series_alternative_titles),
                    values = content.alternativeTitles,
                )
            }
        }
        if (content.genres.isNotEmpty()) {
            item(key = "genres") {
                TextListSection(
                    title = stringResource(R.string.series_genres),
                    values = content.genres,
                )
            }
        }
        if (content.categories.isNotEmpty()) {
            item(key = "categories") {
                SeriesSection(title = stringResource(R.string.series_categories)) {
                    val shown =
                        if (categoriesExpanded) content.categories else content.categories.take(12)
                    BulletList(shown)
                    if (content.categories.size > 12) {
                        TextButton(onClick = { categoriesExpanded = !categoriesExpanded }) {
                            Text(
                                if (categoriesExpanded) {
                                    stringResource(R.string.series_categories_expanded)
                                } else {
                                    pluralStringResource(
                                        R.plurals.series_categories_collapsed,
                                        content.categories.size,
                                        content.categories.size,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }

        if (content.authors.isNotEmpty()) {
            item(key = "authors") {
                TextListSection(
                    title = stringResource(R.string.series_authors),
                    values = content.authors,
                )
            }
        }
        if (content.artists.isNotEmpty()) {
            item(key = "artists") {
                TextListSection(
                    title = stringResource(R.string.series_artists),
                    values = content.artists,
                )
            }
        }
        if (content.otherContributors.isNotEmpty()) {
            item(key = "contributors") {
                TextListSection(
                    title = stringResource(R.string.series_contributors),
                    values = content.otherContributors,
                )
            }
        }
        if (content.publishers.isNotEmpty()) {
            item(key = "publishers") {
                SeriesSection(title = stringResource(R.string.series_publishers)) {
                    content.publishers.forEach { publisher ->
                        Text(
                            text =
                            stringResource(
                                R.string.series_publisher_value,
                                publisher.name,
                                publisher.type,
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        publisher.notes?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeriesHeader(content: SeriesDetailsUiModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
            Modifier
                .size(width = 220.dp, height = 320.dp)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            if (content.coverUrl == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.series_no_cover))
                }
            } else {
                val context = LocalContext.current
                val request =
                    remember(context, content.coverUrl) {
                        ImageRequest
                            .Builder(context)
                            .data(content.coverUrl)
                            .crossfade(true)
                            .build()
                    }
                AsyncImage(
                    model = request,
                    contentDescription = stringResource(R.string.series_cover, content.title),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            modifier = Modifier.semantics { heading() },
            text = content.title,
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}

@Composable
private fun SeriesSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            modifier = Modifier.semantics { heading() },
            text = title,
            style = MaterialTheme.typography.titleLarge,
        )
        content()
    }
}

@Composable
private fun TextListSection(
    title: String,
    values: List<String>,
) {
    SeriesSection(title = title) { BulletList(values) }
}

@Composable
private fun BulletList(values: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        values.forEach { value ->
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier =
                    Modifier
                        .padding(top = 8.dp)
                        .size(5.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.primary),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = value,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun LabelValue(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.4f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            modifier = Modifier.weight(0.6f),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
