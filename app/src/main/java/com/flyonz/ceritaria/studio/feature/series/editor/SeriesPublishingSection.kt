package com.flyonz.ceritaria.studio.feature.series.editor

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
fun SeriesPublishingSection(
    form: SeriesEditorForm,
    errors: Map<SeriesEditorField, SeriesValidationIssue>,
    onChange: (SeriesEditorForm) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.publishing_and_seo), style = MaterialTheme.typography.titleMedium)
        ToggleRow(
            label = stringResource(R.string.featured),
            checked = form.isFeatured,
            onCheckedChange = { onChange(form.copy(isFeatured = it)) },
        )
        ToggleRow(
            label = stringResource(R.string.status_published),
            checked = form.isPublished,
            onCheckedChange = { onChange(form.copy(isPublished = it)) },
        )
        SeriesEditorTextField(
            value = form.seoTitle,
            onValueChange = { onChange(form.copy(seoTitle = it)) },
            label = stringResource(R.string.seo_title),
            issue = errors[SeriesEditorField.SEO_TITLE],
        )
        SeriesEditorTextField(
            value = form.seoDescription,
            onValueChange = { onChange(form.copy(seoDescription = it)) },
            label = stringResource(R.string.seo_description),
            issue = errors[SeriesEditorField.SEO_DESCRIPTION],
            minLines = 2,
            maxLines = 4,
        )
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label)
    }
}
