package com.flyonz.ceritaria.studio.feature.series.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flyonz.ceritaria.studio.R
import com.flyonz.ceritaria.studio.feature.media.domain.ImageMediaSlot
import com.flyonz.ceritaria.studio.feature.media.presentation.ImageMediaUiState
import com.flyonz.ceritaria.studio.feature.media.presentation.ImageMediaViewModel
import com.flyonz.ceritaria.studio.feature.media.presentation.ImagePickerField

@Composable
fun SeriesArtworkSection(
    recordId: String?,
    form: SeriesEditorForm,
    mediaStates: Map<ImageMediaSlot, ImageMediaUiState>,
    mediaViewModel: ImageMediaViewModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.artwork), style = MaterialTheme.typography.titleMedium)
        ArtworkField(
            label = stringResource(R.string.cover_image),
            recordId = recordId,
            slot = ImageMediaSlot.SERIES_COVER,
            currentUrl = form.coverUrl,
            state = mediaStates[ImageMediaSlot.SERIES_COVER] ?: ImageMediaUiState(),
            mediaViewModel = mediaViewModel,
        )
        ArtworkField(
            label = stringResource(R.string.hero_image),
            recordId = recordId,
            slot = ImageMediaSlot.SERIES_HERO,
            currentUrl = form.heroUrl,
            state = mediaStates[ImageMediaSlot.SERIES_HERO] ?: ImageMediaUiState(),
            mediaViewModel = mediaViewModel,
        )
    }
}

@Composable
private fun ArtworkField(
    label: String,
    recordId: String?,
    slot: ImageMediaSlot,
    currentUrl: String,
    state: ImageMediaUiState,
    mediaViewModel: ImageMediaViewModel,
) {
    val oldUrl = currentUrl.ifBlank { null }
    ImagePickerField(
        label = label,
        imageUrl = oldUrl,
        enabled = recordId != null,
        state = state,
        onSelected = { mediaViewModel.replace(recordId, slot, it, oldUrl) },
        onRemove = { mediaViewModel.remove(recordId, slot, oldUrl) },
        onRetry = { mediaViewModel.retry(recordId, slot) },
        onCancel = { mediaViewModel.cancel(slot) },
    )
}
