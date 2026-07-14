package com.kiroku.app.core.designsystem

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kiroku.app.R
import com.kiroku.app.core.common.UiError

@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    Card(
        modifier =
        modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        colors =
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = Icons.Outlined.CloudOff, contentDescription = null)
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.offline_banner),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun LoadingPane(
    @StringRes messageRes: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
        modifier.semantics {
            liveRegion = LiveRegionMode.Polite
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(
            text = androidx.compose.ui.res.stringResource(messageRes),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
fun ErrorPane(
    error: UiError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
        modifier
            .padding(24.dp)
            .semantics { liveRegion = LiveRegionMode.Assertive },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            modifier = Modifier.semantics { heading() },
            text = androidx.compose.ui.res.stringResource(error.titleRes),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = androidx.compose.ui.res.stringResource(error.messageRes),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRetry) {
            Text(androidx.compose.ui.res.stringResource(R.string.action_retry))
        }
    }
}

@Composable
fun InlineErrorBanner(
    error: UiError,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    @StringRes actionLabelRes: Int = R.string.action_retry,
) {
    Card(
        modifier =
        modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Assertive },
        colors =
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = Icons.Outlined.ErrorOutline, contentDescription = null)
                Text(
                    text = androidx.compose.ui.res.stringResource(error.titleRes),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Text(
                text = androidx.compose.ui.res.stringResource(error.messageRes),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onAction) {
                Text(androidx.compose.ui.res.stringResource(actionLabelRes))
            }
        }
    }
}

@get:StringRes
private val UiError.titleRes: Int
    get() =
        when (this) {
            UiError.OFFLINE -> R.string.error_offline_title
            UiError.TIMEOUT -> R.string.error_timeout_title
            UiError.AUTHENTICATION -> R.string.error_authentication_title
            UiError.PERMISSION -> R.string.error_permission_title
            UiError.RATE_LIMITED -> R.string.error_rate_limit_title
            UiError.VALIDATION -> R.string.error_validation_title
            UiError.NOT_FOUND -> R.string.error_not_found_title
            UiError.SERVER -> R.string.error_server_title
            UiError.DATA -> R.string.error_data_title
            UiError.UNKNOWN -> R.string.error_unknown_title
        }

@get:StringRes
private val UiError.messageRes: Int
    get() =
        when (this) {
            UiError.OFFLINE -> R.string.error_offline_message
            UiError.TIMEOUT -> R.string.error_timeout_message
            UiError.AUTHENTICATION -> R.string.error_authentication_message
            UiError.PERMISSION -> R.string.error_permission_message
            UiError.RATE_LIMITED -> R.string.error_rate_limit_message
            UiError.VALIDATION -> R.string.error_validation_message
            UiError.NOT_FOUND -> R.string.error_not_found_message
            UiError.SERVER -> R.string.error_server_message
            UiError.DATA -> R.string.error_data_message
            UiError.UNKNOWN -> R.string.error_unknown_message
        }
