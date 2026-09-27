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
fun SeriesStorySection(
    form: SeriesEditorForm,
    errors: Map<SeriesEditorField, SeriesValidationIssue>,
    onChange: (SeriesEditorForm) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.story), style = MaterialTheme.typography.titleMedium)
        SeriesEditorTextField(
            value = form.shortSynopsis,
            onValueChange = { onChange(form.copy(shortSynopsis = it)) },
            label = stringResource(R.string.short_synopsis),
            issue = errors[SeriesEditorField.SHORT_SYNOPSIS],
            minLines = 2,
            maxLines = 3,
        )
        SeriesEditorTextField(
            value = form.synopsis,
            onValueChange = { onChange(form.copy(synopsis = it)) },
            label = stringResource(R.string.synopsis),
            issue = errors[SeriesEditorField.SYNOPSIS],
            minLines = 5,
            maxLines = 10,
        )
    }
}
