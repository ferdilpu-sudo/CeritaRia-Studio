package com.flyonz.ceritaria.studio.feature.episode.reorder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flyonz.ceritaria.studio.R

@Composable
fun EpisodeReorderContent(
    state: EpisodeReorderUiState,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
) {
    when {
        state.isLoading -> CircularProgressIndicator(modifier = Modifier.padding(24.dp))
        state.loadFailed -> LoadFailed(onRetry)
        else -> ReorderBody(state, onMoveUp, onMoveDown, onSave)
    }
}

@Composable
private fun ReorderBody(
    state: EpisodeReorderUiState,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.reorder_help),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.saveError?.let {
            Text(
                text = stringResource(it.messageRes()),
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (state.saveSucceeded) {
            Text(
                text = stringResource(R.string.reorder_saved),
                color = MaterialTheme.colorScheme.primary,
            )
        }

        if (state.episodes.isEmpty()) {
            Text(stringResource(R.string.no_episodes_reorder))
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(state.episodes, key = { _, episode -> episode.id }) { index, episode ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(
                                    R.string.reorder_episode_position,
                                    index + 1,
                                    episode.title,
                                ),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = stringResource(
                                    R.string.original_episode_number,
                                    episode.episodeNumber,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(
                            onClick = { onMoveUp(index) },
                            enabled = index > 0 && !state.isSaving,
                        ) {
                            Icon(
                                Icons.Outlined.ArrowUpward,
                                contentDescription = stringResource(
                                    R.string.move_episode_up,
                                    episode.title,
                                ),
                            )
                        }
                        IconButton(
                            onClick = { onMoveDown(index) },
                            enabled = index < state.episodes.lastIndex && !state.isSaving,
                        ) {
                            Icon(
                                Icons.Outlined.ArrowDownward,
                                contentDescription = stringResource(
                                    R.string.move_episode_down,
                                    episode.title,
                                ),
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }

        Button(
            onClick = onSave,
            enabled = state.isDirty && !state.isSaving,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        ) {
            Text(
                stringResource(
                    if (state.isSaving) R.string.reorder_saving else R.string.reorder_save_order,
                ),
            )
        }
    }
}

@Composable
private fun LoadFailed(onRetry: () -> Unit) {
    Column(modifier = Modifier.padding(20.dp)) {
        Text(stringResource(R.string.reorder_load_error))
        Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
            Text(stringResource(R.string.retry))
        }
    }
}

private fun EpisodeReorderSaveError.messageRes(): Int = when (this) {
    EpisodeReorderSaveError.CONFLICT -> R.string.reorder_conflict
    EpisodeReorderSaveError.GENERIC -> R.string.reorder_save_error
}
