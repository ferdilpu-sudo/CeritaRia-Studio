package com.flyonz.ceritaria.studio.feature.media.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.flyonz.ceritaria.studio.R

@Composable
fun ImageMediaStatus(
    state: ImageMediaUiState,
    enabled: Boolean,
) {
    if (!enabled) {
        Text(
            text = stringResource(R.string.media_save_first),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    when (state.status) {
        ImageMediaUiStatus.QUEUED,
        ImageMediaUiStatus.RUNNING,
        -> {
            LinearProgressIndicator(
                progress = { state.progress.coerceIn(0, 100) / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.media_upload_progress, state.progress),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        ImageMediaUiStatus.SUCCEEDED -> Text(
            text = stringResource(R.string.media_update_success),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall,
        )
        ImageMediaUiStatus.FAILED -> Text(
            text = stringResource(R.string.media_upload_failed),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
        ImageMediaUiStatus.CANCELLED -> Text(
            text = stringResource(R.string.media_upload_cancelled),
            style = MaterialTheme.typography.bodySmall,
        )
        ImageMediaUiStatus.SAVE_FIRST -> Text(
            text = stringResource(R.string.media_save_first),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ImageMediaUiStatus.IDLE -> Unit
    }
}
