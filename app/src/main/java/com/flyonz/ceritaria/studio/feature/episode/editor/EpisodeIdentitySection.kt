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
fun EpisodeIdentitySection(
    form: EpisodeEditorForm,
    options: List<EpisodeSeriesOption>,
    errors: Map<EpisodeEditorField, EpisodeValidationIssue>,
    onChange: (EpisodeEditorForm) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.episode_information), style = MaterialTheme.typography.titleMedium)
        EpisodeSeriesSelector(
            options = options,
            selectedId = form.seriesId,
            issue = errors[EpisodeEditorField.SERIES],
            onSelected = { onChange(form.copy(seriesId = it)) },
        )
        EpisodeEditorTextField(
            value = form.episodeNumber,
            onValueChange = { onChange(form.copy(episodeNumber = it)) },
            label = stringResource(R.string.episode_number),
            issue = errors[EpisodeEditorField.EPISODE_NUMBER],
        )
        EpisodeEditorTextField(
            value = form.title,
            onValueChange = { onChange(form.copy(title = it)) },
            label = stringResource(R.string.title),
            issue = errors[EpisodeEditorField.TITLE],
        )
        EpisodeEditorTextField(
            value = form.slug,
            onValueChange = { onChange(form.copy(slug = it)) },
            label = stringResource(R.string.slug),
            issue = errors[EpisodeEditorField.SLUG],
        )
    }
}
