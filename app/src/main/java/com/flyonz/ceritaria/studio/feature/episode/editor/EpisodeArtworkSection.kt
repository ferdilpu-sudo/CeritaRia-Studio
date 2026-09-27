package com.flyonz.ceritaria.studio.feature.episode.editor

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.flyonz.ceritaria.studio.R
import com.flyonz.ceritaria.studio.feature.media.domain.ImageMediaSlot
import com.flyonz.ceritaria.studio.feature.media.presentation.ImageMediaUiState
import com.flyonz.ceritaria.studio.feature.media.presentation.ImageMediaViewModel
import com.flyonz.ceritaria.studio.feature.media.presentation.ImagePickerField

@Composable
fun EpisodeArtworkSection(
    recordId: String?,
    form: EpisodeEditorForm,
    mediaState: ImageMediaUiState,
    mediaViewModel: ImageMediaViewModel,
) {
    Text(stringResource(R.string.artwork), style = MaterialTheme.typography.titleMedium)
    val slot = ImageMediaSlot.EPISODE_THUMBNAIL
    val oldUrl = form.thumbnailUrl.ifBlank { null }
    ImagePickerField(
        label = stringResource(R.string.thumbnail_image),
        imageUrl = oldUrl,
        enabled = recordId != null,
        state = mediaState,
        onSelected = { mediaViewModel.replace(recordId, slot, it, oldUrl) },
        onRemove = { mediaViewModel.remove(recordId, slot, oldUrl) },
        onRetry = { mediaViewModel.retry(recordId, slot) },
        onCancel = { mediaViewModel.cancel(slot) },
    )
}
