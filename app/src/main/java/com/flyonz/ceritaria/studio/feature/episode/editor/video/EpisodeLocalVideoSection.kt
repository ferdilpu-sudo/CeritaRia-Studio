package com.flyonz.ceritaria.studio.feature.episode.editor.video

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flyonz.ceritaria.studio.R
import com.flyonz.ceritaria.studio.core.media.VideoEncodingStatus
import com.flyonz.ceritaria.studio.core.media.VideoMetadata
import java.util.Locale

@Composable
fun EpisodeLocalVideoSection(
    state: EpisodeLocalVideoUiState,
    onSelected: (String) -> Unit,
    onPrepare: () -> Unit,
    onCancel: () -> Unit,
    onUpload: () -> Unit,
    onCancelUpload: () -> Unit,
    attachedAssetId: String?,
    attachmentState: EpisodeVideoAttachmentUiState,
    onAttach: (String) -> Unit,
) {
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.toString()?.let(onSelected)
    }
    val transferLocked = state.status in TRANSFER_LOCKED_STATUSES
    val isBusy = state.status == EpisodeLocalVideoStatus.INSPECTING ||
        state.status == EpisodeLocalVideoStatus.ENCODING ||
        transferLocked

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.local_video), style = MaterialTheme.typography.titleMedium)

        state.job?.let { job ->
            Text(
                text = stringResource(R.string.source_video) + ": " +
                    metadataSummary(job.sourceMetadata),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.studio_video_output),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LocalVideoStatus(state)

        OutlinedButton(
            onClick = {
                picker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
                )
            },
            enabled = state.status != EpisodeLocalVideoStatus.SAVE_FIRST && !isBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(
                    if (state.job == null) {
                        R.string.select_local_video
                    } else {
                        R.string.replace_local_video
                    },
                ),
            )
        }

        if (state.status == EpisodeLocalVideoStatus.READY_TO_ENCODE ||
            state.status == EpisodeLocalVideoStatus.FAILED ||
            state.status == EpisodeLocalVideoStatus.CANCELLED
        ) {
            Button(
                onClick = onPrepare,
                enabled = state.job?.needsEncoding == true,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.prepare_video))
            }
        }

        if (state.status == EpisodeLocalVideoStatus.ENCODING) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.cancel_encoding))
            }
        }

        if (state.canUpload()) {
            Button(onClick = onUpload, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(
                        if (state.status == EpisodeLocalVideoStatus.UPLOAD_FAILED ||
                            state.status == EpisodeLocalVideoStatus.UPLOAD_CANCELLED
                        ) {
                            R.string.retry_video_upload
                        } else {
                            R.string.upload_video
                        },
                    ),
                )
            }
        }

        if (state.status == EpisodeLocalVideoStatus.UPLOAD_READY) {
            EpisodeVideoAttachmentControls(
                remoteAssetId = state.job?.remoteAssetId,
                attachedAssetId = attachedAssetId,
                state = attachmentState,
                onAttach = onAttach,
            )
        }

        if (state.status == EpisodeLocalVideoStatus.UPLOAD_QUEUED ||
            state.status == EpisodeLocalVideoStatus.UPLOADING ||
            state.status == EpisodeLocalVideoStatus.UPLOAD_FAILED
        ) {
            OutlinedButton(
                onClick = onCancelUpload,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.cancel_video_upload))
            }
        }
    }
}

@Composable
private fun LocalVideoStatus(state: EpisodeLocalVideoUiState) {
    when (state.status) {
        EpisodeLocalVideoStatus.IDLE -> Unit
        EpisodeLocalVideoStatus.SAVE_FIRST -> Text(stringResource(R.string.local_video_save_first))
        EpisodeLocalVideoStatus.INSPECTING -> Text(stringResource(R.string.local_video_inspecting))
        EpisodeLocalVideoStatus.READY_WITHOUT_ENCODING ->
            Text(stringResource(R.string.local_video_ready_direct))
        EpisodeLocalVideoStatus.READY_TO_ENCODE ->
            Text(stringResource(R.string.local_video_ready_encode))
        EpisodeLocalVideoStatus.ENCODING -> ProgressStatus(
            progress = state.job?.encodingProgress ?: 0,
            label = R.string.local_video_encoding,
        )
        EpisodeLocalVideoStatus.ENCODED_READY ->
            Text(stringResource(R.string.local_video_encoded_ready))
        EpisodeLocalVideoStatus.UPLOAD_QUEUED ->
            Text(stringResource(R.string.video_upload_queued))
        EpisodeLocalVideoStatus.UPLOADING -> {
            val job = state.job
            val percent = if (job != null && job.totalBytes > 0L) {
                ((job.uploadedBytes * 100L) / job.totalBytes).toInt().coerceIn(0, 100)
            } else {
                0
            }
            ProgressStatus(percent, R.string.video_upload_progress_local)
        }
        EpisodeLocalVideoStatus.VERIFYING ->
            Text(stringResource(R.string.video_upload_verifying))
        EpisodeLocalVideoStatus.UPLOAD_READY -> Text(
            stringResource(
                R.string.video_upload_ready,
                state.job?.remoteAssetId ?: "?",
            ),
            color = MaterialTheme.colorScheme.primary,
        )
        EpisodeLocalVideoStatus.UPLOAD_FAILED -> Text(
            stringResource(
                R.string.video_upload_failed_local,
                state.errorCode ?: "UNKNOWN",
            ),
            color = MaterialTheme.colorScheme.error,
        )
        EpisodeLocalVideoStatus.UPLOAD_CANCELLED ->
            Text(stringResource(R.string.video_upload_cancelled_local))
        EpisodeLocalVideoStatus.FAILED -> Text(
            text = stringResource(
                R.string.local_video_failed,
                state.errorCode ?: "UNKNOWN",
            ),
            color = MaterialTheme.colorScheme.error,
        )
        EpisodeLocalVideoStatus.CANCELLED ->
            Text(stringResource(R.string.local_video_cancelled))
    }
}

@Composable
private fun ProgressStatus(
    progress: Int,
    label: Int,
) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0, 100) / 100f },
        modifier = Modifier.fillMaxWidth(),
    )
    Text(stringResource(label, progress))
}

private fun EpisodeLocalVideoUiState.canUpload(): Boolean {
    val current = job ?: return false
    val sourceReady = !current.needsEncoding ||
        current.encodingStatus == VideoEncodingStatus.READY
    return sourceReady && status in UPLOAD_ACTION_STATUSES
}

private fun metadataSummary(metadata: VideoMetadata): String {
    val durationSeconds = (metadata.durationMs ?: 0L) / 1000L
    val duration = "%02d:%02d".format(
        Locale.US,
        durationSeconds / 60,
        durationSeconds % 60,
    )
    val resolution = if (metadata.width != null && metadata.height != null) {
        metadata.width.toString() + "×" + metadata.height.toString()
    } else {
        "?×?"
    }
    val fps = metadata.frameRate?.let {
        "%.1f fps".format(Locale.US, it)
    } ?: "? fps"
    return duration + " · " + resolution + " · " + fps + " · " +
        formatBytes(metadata.sizeBytes)
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L * 1024L ->
        "%.1f GB".format(Locale.US, bytes / (1024.0 * 1024.0 * 1024.0))
    bytes >= 1024L * 1024L ->
        "%.1f MB".format(Locale.US, bytes / (1024.0 * 1024.0))
    else -> "%.1f KB".format(Locale.US, bytes / 1024.0)
}

private val TRANSFER_LOCKED_STATUSES = setOf(
    EpisodeLocalVideoStatus.UPLOAD_QUEUED,
    EpisodeLocalVideoStatus.UPLOADING,
    EpisodeLocalVideoStatus.VERIFYING,
    EpisodeLocalVideoStatus.UPLOAD_READY,
    EpisodeLocalVideoStatus.UPLOAD_FAILED,
)

private val UPLOAD_ACTION_STATUSES = setOf(
    EpisodeLocalVideoStatus.READY_WITHOUT_ENCODING,
    EpisodeLocalVideoStatus.ENCODED_READY,
    EpisodeLocalVideoStatus.UPLOAD_FAILED,
    EpisodeLocalVideoStatus.UPLOAD_CANCELLED,
)
