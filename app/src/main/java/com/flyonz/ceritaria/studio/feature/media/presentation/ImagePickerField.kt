package com.flyonz.ceritaria.studio.feature.media.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flyonz.ceritaria.studio.core.designsystem.component.RemoteArtwork

@Composable
fun ImagePickerField(
    label: String,
    imageUrl: String?,
    enabled: Boolean,
    state: ImageMediaUiState,
    onSelected: (String) -> Unit,
    onRemove: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.toString()?.let(onSelected)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        RemoteArtwork(
            imageUrl = imageUrl,
            fallbackText = label,
            modifier = Modifier.fillMaxWidth().height(160.dp),
        )
        ImageMediaStatus(state = state, enabled = enabled)
        ImageMediaActions(
            hasImage = !imageUrl.isNullOrBlank(),
            enabled = enabled,
            state = state,
            onPick = {
                picker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onRemove = onRemove,
            onRetry = onRetry,
            onCancel = onCancel,
        )
    }
}
