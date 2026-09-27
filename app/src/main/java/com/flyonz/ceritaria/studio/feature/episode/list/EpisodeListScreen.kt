package com.flyonz.ceritaria.studio.feature.episode.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flyonz.ceritaria.studio.R

@Composable
fun EpisodeListScreen(
    contentPadding: PaddingValues,
    onEpisodeClick: (String) -> Unit,
    viewModel: EpisodeListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = contentPadding.calculateTopPadding() + 16.dp,
            end = 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text(stringResource(R.string.episodes)) }
        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.search_episodes)) },
                singleLine = true,
            )
        }
        item {
            EpisodeSeriesFilter(
                options = state.seriesOptions,
                selectedId = state.seriesId,
                onSelected = viewModel::setSeries,
            )
        }
        item { EpisodeStatusFilters(state.status, viewModel::setStatus) }
        item { EpisodeProviderFilters(state.provider, viewModel::setProvider) }

        when {
            state.isLoading -> item { EpisodeLoadingState() }
            state.hasError && state.items.isEmpty() -> item {
                EpisodeErrorState(onRetry = viewModel::refresh)
            }
            state.items.isEmpty() -> item {
                EpisodeEmptyState(hasQuery = state.query.isNotBlank())
            }
            else -> {
                items(state.items, key = { it.id }) { episode ->
                    EpisodeListRow(
                        episode = episode,
                        onClick = { onEpisodeClick(episode.id) },
                    )
                }
                if (state.hasMore) {
                    item {
                        Button(
                            onClick = viewModel::loadMore,
                            enabled = !state.isLoadingMore,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (state.isLoadingMore) {
                                    stringResource(R.string.loading_more)
                                } else {
                                    stringResource(R.string.load_more)
                                },
                            )
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = viewModel::refresh,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isRefreshing,
            ) {
                Text(
                    if (state.isRefreshing) {
                        stringResource(R.string.refreshing)
                    } else {
                        stringResource(R.string.refresh)
                    },
                )
            }
        }
    }
}
