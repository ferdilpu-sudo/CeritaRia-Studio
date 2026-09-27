package com.flyonz.ceritaria.studio.feature.series.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flyonz.ceritaria.studio.R

@Composable
fun SeriesEditorBody(
    state: SeriesEditorUiState,
    viewModel: SeriesEditorViewModel,
) {
    when {
        state.isLoading -> CircularProgressIndicator(modifier = Modifier.padding(24.dp))
        state.loadFailed -> LoadFailed(onRetry = viewModel::retry)
        else -> SeriesEditorFormContent(state = state, viewModel = viewModel)
    }
}

@Composable
private fun LoadFailed(onRetry: () -> Unit) {
    Column(modifier = Modifier.padding(20.dp)) {
        Text(stringResource(R.string.series_editor_load_error))
        Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
            Text(stringResource(R.string.retry))
        }
    }
}

@Composable
private fun SeriesEditorFormContent(
    state: SeriesEditorUiState,
    viewModel: SeriesEditorViewModel,
) {
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        state.saveError?.let { error ->
            item {
                Text(
                    text = stringResource(error.messageRes()),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        item { SeriesIdentitySection(state.form, state.validationErrors, viewModel::setForm) }
        item { SeriesStorySection(state.form, state.validationErrors, viewModel::setForm) }
        item { SeriesMediaUrlSection(state.form, state.validationErrors, viewModel::setForm) }
        item { SeriesPublishingSection(state.form, state.validationErrors, viewModel::setForm) }
        item {
            Button(
                onClick = viewModel::save,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(
                        if (state.isSaving) R.string.saving else R.string.save,
                    ),
                )
            }
        }
    }
}

private fun SeriesEditorSaveError.messageRes(): Int = when (this) {
    SeriesEditorSaveError.DUPLICATE_SLUG -> R.string.series_slug_conflict
    SeriesEditorSaveError.GENERIC -> R.string.series_save_error
}
