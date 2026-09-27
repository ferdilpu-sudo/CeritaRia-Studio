package com.flyonz.ceritaria.studio.feature.episode.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flyonz.ceritaria.studio.R

@Composable
fun EpisodeVideoSection(
    form: EpisodeEditorForm,
    errors: Map<EpisodeEditorField, EpisodeValidationIssue>,
    onChange: (EpisodeEditorForm) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.video), style = MaterialTheme.typography.titleMedium)
        EpisodeProviderSelector(
            selected = form.videoProvider,
            issue = errors[EpisodeEditorField.VIDEO_PROVIDER],
            onSelected = { onChange(form.copy(videoProvider = it)) },
        )
        EpisodeEditorTextField(
            value = form.videoUrl,
            onValueChange = { onChange(form.copy(videoUrl = it)) },
            label = stringResource(R.string.video_url),
            issue = errors[EpisodeEditorField.VIDEO_URL],
        )
        EpisodeEditorTextField(
            value = form.thumbnailUrl,
            onValueChange = { onChange(form.copy(thumbnailUrl = it)) },
            label = stringResource(R.string.thumbnail_url),
            issue = errors[EpisodeEditorField.THUMBNAIL_URL],
        )
        EpisodeEditorTextField(
            value = form.durationSeconds,
            onValueChange = { onChange(form.copy(durationSeconds = it)) },
            label = stringResource(R.string.duration),
            issue = errors[EpisodeEditorField.DURATION],
        )
        Text(
            text = stringResource(R.string.media_upload_phase4_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
