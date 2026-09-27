package com.flyonz.ceritaria.studio.feature.series.editor

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.flyonz.ceritaria.studio.R

@Composable
fun SeriesEditorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    issue: SeriesValidationIssue?,
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

private fun SeriesValidationIssue.messageRes(): Int = when (this) {
    SeriesValidationIssue.REQUIRED -> R.string.field_required
    SeriesValidationIssue.INVALID_SLUG -> R.string.invalid_slug
    SeriesValidationIssue.TOO_LONG -> R.string.field_too_long
    SeriesValidationIssue.INVALID_URL -> R.string.invalid_url
}
