package com.flyonz.ceritaria.studio.feature.series.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flyonz.ceritaria.studio.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesDetailScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onEpisodesClick: (String) -> Unit,
    onEditClick: (String) -> Unit,
    onDeleted: () -> Unit,
    viewModel: SeriesDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            if (effect is SeriesDetailEffect.Deleted) onDeleted()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding()),
    ) {
        TopAppBar(
            title = { Text(state.series?.title ?: stringResource(R.string.series_detail)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            },
        )
        when {
            state.isLoading -> CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            state.hasError || state.series == null -> LoadFailed(onRetry = viewModel::retry)
            else -> SeriesDetailContent(
                series = requireNotNull(state.series),
                isDeleting = state.isDeleting,
                deleteError = state.deleteError,
                onEpisodesClick = onEpisodesClick,
                onEditClick = onEditClick,
                onDeleteClick = { showDeleteDialog = true },
            )
        }
    }

    if (showDeleteDialog) {
        DeleteSeriesDialog(
            onConfirm = {
                showDeleteDialog = false
                viewModel.delete()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

@Composable
private fun LoadFailed(onRetry: () -> Unit) {
    Column(modifier = Modifier.padding(20.dp)) {
        Text(stringResource(R.string.series_detail_error))
        Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
            Text(stringResource(R.string.retry))
        }
    }
}

@Composable
private fun DeleteSeriesDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_series_title)) },
        text = { Text(stringResource(R.string.delete_series_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
