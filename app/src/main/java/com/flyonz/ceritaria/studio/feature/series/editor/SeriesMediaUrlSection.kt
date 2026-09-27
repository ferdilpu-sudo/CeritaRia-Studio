package com.flyonz.ceritaria.studio.feature.series.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flyonz.ceritaria.studio.R

@Composable
fun SeriesMediaUrlSection(
    form: SeriesEditorForm,
    errors: Map<SeriesEditorField, SeriesValidationIssue>,
    onChange: (SeriesEditorForm) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.media_urls), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(R.string.media_upload_phase4_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SeriesEditorTextField(
            value = form.coverUrl,
            onValueChange = { onChange(form.copy(coverUrl = it)) },
            label = stringResource(R.string.cover_url),
            issue = errors[SeriesEditorField.COVER_URL],
        )
        SeriesEditorTextField(
            value = form.heroUrl,
            onValueChange = { onChange(form.copy(heroUrl = it)) },
            label = stringResource(R.string.hero_url),
            issue = errors[SeriesEditorField.HERO_URL],
        )
    }
}
