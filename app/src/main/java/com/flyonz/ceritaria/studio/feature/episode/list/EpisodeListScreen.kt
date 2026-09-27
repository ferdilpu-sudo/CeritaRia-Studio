package com.flyonz.ceritaria.studio.feature.episode.list

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeListScreen(
    contentPadding: PaddingValues,
    onEpisodeClick: (String) -> Unit,
    onCreateEpisode: (String?) -> Unit,
    viewModel: EpisodeListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        EpisodeListContent(
            contentPadding = contentPadding,
            state = state,
            onQueryChange = viewModel::setQuery,
            onSeriesChange = viewModel::setSeries,
            onStatusChange = viewModel::setStatus,
            onProviderChange = viewModel::setProvider,
            onEpisodeClick = onEpisodeClick,
            onCreateEpisode = { onCreateEpisode(state.seriesId) },
            onLoadMore = viewModel::loadMore,
            onRetry = viewModel::refresh,
        )
    }
}
