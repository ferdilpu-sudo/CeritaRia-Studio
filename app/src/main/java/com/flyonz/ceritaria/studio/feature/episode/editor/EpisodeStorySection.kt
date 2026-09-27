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
fun EpisodeStorySection(
    form: EpisodeEditorForm,
    errors: Map<EpisodeEditorField, EpisodeValidationIssue>,
    onChange: (EpisodeEditorForm) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.story), style = MaterialTheme.typography.titleMedium)
        EpisodeEditorTextField(
            value = form.shortSynopsis,
            onValueChange = { onChange(form.copy(shortSynopsis = it)) },
            label = stringResource(R.string.short_synopsis),
            issue = errors[EpisodeEditorField.SHORT_SYNOPSIS],
            minLines = 2,
            maxLines = 3,
        )
        EpisodeEditorTextField(
            value = form.recap,
            onValueChange = { onChange(form.copy(recap = it)) },
            label = stringResource(R.string.recap),
            issue = errors[EpisodeEditorField.RECAP],
            minLines = 5,
            maxLines = 10,
        )
        EpisodeEditorTextField(
            value = form.highlights,
            onValueChange = { onChange(form.copy(highlights = it)) },
            label = stringResource(R.string.highlights_one_per_line),
            issue = errors[EpisodeEditorField.HIGHLIGHTS],
            minLines = 4,
            maxLines = 8,
        )
    }
}
