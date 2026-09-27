package com.flyonz.ceritaria.studio.feature.episode.editor.video

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.flyonz.ceritaria.studio.R

@Composable
fun EpisodeVideoAttachmentControls(
    remoteAssetId: String?,
    attachedAssetId: String?,
    state: EpisodeVideoAttachmentUiState,
    onAttach: (String) -> Unit,
    onPreview: (String) -> Unit,
) {
    val assetId = remoteAssetId ?: return
    val isAlreadyAttached = assetId == attachedAssetId ||
        (state.status == EpisodeVideoAttachmentStatus.ATTACHED && state.assetId == assetId)

    if (state.previewStatus == EpisodeVideoPreviewStatus.FAILED) {
        Text(
            text = stringResource(
                R.string.video_asset_preview_failed,
                state.previewErrorCode ?: "UNKNOWN",
            ),
            color = MaterialTheme.colorScheme.error,
        )
    }
    OutlinedButton(
        onClick = { onPreview(assetId) },
        enabled = state.previewStatus != EpisodeVideoPreviewStatus.LOADING,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            stringResource(
                if (state.previewStatus == EpisodeVideoPreviewStatus.LOADING) {
                    R.string.preview_video_loading
                } else {
                    R.string.preview_video
                },
            ),
        )
    }

    when {
        isAlreadyAttached -> Text(
            text = stringResource(R.string.video_asset_attached_active),
            color = MaterialTheme.colorScheme.primary,
        )
        state.status == EpisodeVideoAttachmentStatus.ATTACHING &&
            state.assetId == assetId -> {
            CircularProgressIndicator()
            Text(stringResource(R.string.attaching_video_asset))
        }
        state.status == EpisodeVideoAttachmentStatus.FAILED &&
            state.assetId == assetId -> {
            Text(
                text = stringResource(
                    R.string.video_asset_attach_failed,
                    state.errorCode ?: "UNKNOWN",
                ),
                color = MaterialTheme.colorScheme.error,
            )
            Button(
                onClick = { onAttach(assetId) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.retry_attach_video_asset))
            }
        }
        else -> Button(
            onClick = { onAttach(assetId) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.attach_video_asset))
        }
    }
}
