package com.flyonz.ceritaria.studio.feature.series.list

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
fun SeriesListScreen(
    contentPadding: PaddingValues,
    onSeriesClick: (String) -> Unit,
    viewModel: SeriesListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        SeriesListContent(
            contentPadding = contentPadding,
            state = state,
            onQueryChange = viewModel::setQuery,
            onFilterChange = viewModel::setFilter,
            onSeriesClick = onSeriesClick,
            onLoadMore = viewModel::loadMore,
            onRetry = viewModel::refresh,
        )
    }
}
