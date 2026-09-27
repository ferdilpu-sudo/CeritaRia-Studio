package com.flyonz.ceritaria.studio.feature.episode.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flyonz.ceritaria.studio.R

@Composable
fun EpisodePublishingSection(
    form: EpisodeEditorForm,
    errors: Map<EpisodeEditorField, EpisodeValidationIssue>,
    onChange: (EpisodeEditorForm) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.publishing_and_seo), style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(
                checked = form.isPublished,
                onCheckedChange = { onChange(form.copy(isPublished = it)) },
            )
            Text(stringResource(R.string.status_published))
        }
        EpisodeEditorTextField(
            value = form.seoTitle,
            onValueChange = { onChange(form.copy(seoTitle = it)) },
            label = stringResource(R.string.seo_title),
            issue = errors[EpisodeEditorField.SEO_TITLE],
        )
        EpisodeEditorTextField(
            value = form.seoDescription,
            onValueChange = { onChange(form.copy(seoDescription = it)) },
            label = stringResource(R.string.seo_description),
            issue = errors[EpisodeEditorField.SEO_DESCRIPTION],
            minLines = 2,
            maxLines = 4,
        )
    }
}
