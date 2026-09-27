package com.flyonz.ceritaria.studio.feature.media.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flyonz.ceritaria.studio.R

@Composable
fun ImageMediaActions(
    hasImage: Boolean,
    enabled: Boolean,
    state: ImageMediaUiState,
    onPick: () -> Unit,
    onRemove: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    val isRunning = state.status == ImageMediaUiStatus.QUEUED ||
        state.status == ImageMediaUiStatus.RUNNING

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onPick,
            enabled = enabled && !isRunning,
        ) {
            Text(
                stringResource(
                    if (hasImage) R.string.media_replace else R.string.media_choose,
                ),
            )
        }
        if (hasImage) {
            TextButton(
                onClick = onRemove,
                enabled = enabled && !isRunning,
            ) {
                Text(stringResource(R.string.remove))
            }
        }
        if (state.status == ImageMediaUiStatus.FAILED) {
            TextButton(onClick = onRetry, enabled = enabled) {
                Text(stringResource(R.string.retry))
            }
        }
        if (isRunning) {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}
