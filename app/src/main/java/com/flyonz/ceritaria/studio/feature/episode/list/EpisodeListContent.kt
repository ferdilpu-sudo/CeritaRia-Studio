package com.flyonz.ceritaria.studio.feature.episode.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flyonz.ceritaria.studio.R
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeStatusFilter
import com.flyonz.ceritaria.studio.feature.episode.domain.VideoProviderFilter

@Composable
fun EpisodeListContent(
    contentPadding: PaddingValues,
    state: EpisodeListUiState,
    onQueryChange: (String) -> Unit,
    onSeriesChange: (String?) -> Unit,
    onStatusChange: (EpisodeStatusFilter) -> Unit,
    onProviderChange: (VideoProviderFilter) -> Unit,
    onEpisodeClick: (String) -> Unit,
    onCreateEpisode: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
) {
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
        item {
            EpisodeListHeader(
                query = state.query,
                onQueryChange = onQueryChange,
                onCreateEpisode = onCreateEpisode,
            )
        }
        item {
            EpisodeSeriesFilter(
                options = state.seriesOptions,
                selectedId = state.seriesId,
                onSelected = onSeriesChange,
            )
        }
        item { EpisodeStatusFilters(state.status, onStatusChange) }
        item { EpisodeProviderFilters(state.provider, onProviderChange) }

        when {
            state.isLoading -> item { EpisodeLoadingState() }
            state.hasError && state.items.isEmpty() -> item { EpisodeErrorState(onRetry) }
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
                            onClick = onLoadMore,
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
    }
}
