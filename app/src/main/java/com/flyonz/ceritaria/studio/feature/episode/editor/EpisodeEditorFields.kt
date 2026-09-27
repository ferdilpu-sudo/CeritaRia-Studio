package com.flyonz.ceritaria.studio.feature.episode.editor

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.flyonz.ceritaria.studio.R

@Composable
fun EpisodeEditorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    issue: EpisodeValidationIssue?,
    modifier: Modifier = Modifier.fillMaxWidth(),
    minLines: Int = 1,
    maxLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        minLines = minLines,
        maxLines = maxLines,
        isError = issue != null,
        supportingText = issue?.let {
            { Text(stringResource(it.messageRes())) }
        },
    )
}

private fun EpisodeValidationIssue.messageRes(): Int = when (this) {
    EpisodeValidationIssue.REQUIRED -> R.string.field_required
    EpisodeValidationIssue.INVALID_UUID -> R.string.invalid_series
    EpisodeValidationIssue.INVALID_NUMBER -> R.string.invalid_number
    EpisodeValidationIssue.INVALID_SLUG -> R.string.invalid_slug
    EpisodeValidationIssue.TOO_LONG -> R.string.field_too_long
    EpisodeValidationIssue.INVALID_URL -> R.string.invalid_url
    EpisodeValidationIssue.UNSUPPORTED_PROVIDER -> R.string.unsupported_provider
    EpisodeValidationIssue.INVALID_VIDEO_URL -> R.string.invalid_video_url
}
