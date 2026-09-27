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
fun SeriesIdentitySection(
    form: SeriesEditorForm,
    errors: Map<SeriesEditorField, SeriesValidationIssue>,
    onChange: (SeriesEditorForm) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.series_information), style = MaterialTheme.typography.titleMedium)
        SeriesEditorTextField(
            value = form.title,
            onValueChange = { onChange(form.copy(title = it)) },
            label = stringResource(R.string.title),
            issue = errors[SeriesEditorField.TITLE],
        )
        SeriesEditorTextField(
            value = form.slug,
            onValueChange = { onChange(form.copy(slug = it)) },
            label = stringResource(R.string.slug),
            issue = errors[SeriesEditorField.SLUG],
        )
        SeriesEditorTextField(
            value = form.genres,
            onValueChange = { onChange(form.copy(genres = it)) },
            label = stringResource(R.string.genres_comma_separated),
            issue = errors[SeriesEditorField.GENRES],
        )
    }
}
