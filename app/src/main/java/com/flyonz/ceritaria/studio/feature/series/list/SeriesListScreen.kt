package com.flyonz.ceritaria.studio.feature.series.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import com.flyonz.ceritaria.studio.feature.series.domain.SeriesFilter

@Composable
fun SeriesListScreen(
    contentPadding: PaddingValues,
    onSeriesClick: (String) -> Unit,
    viewModel: SeriesListViewModel = hiltViewModel(),
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
        item {
            Text(
                text = stringResource(R.string.series),
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.search_series)) },
                singleLine = true,
            )
        }
        item {
            SeriesFilterRow(selected = state.filter, onSelected = viewModel::setFilter)
        }

        when {
            state.isLoading -> item { SeriesLoadingState() }
            state.hasError && state.items.isEmpty() -> item {
                SeriesErrorState(onRetry = viewModel::refresh)
            }
            state.items.isEmpty() -> item { SeriesEmptyState(hasQuery = state.query.isNotBlank()) }
            else -> {
                items(state.items, key = { it.id }) { series ->
                    SeriesListRow(series = series, onClick = { onSeriesClick(series.id) })
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
                if (state.hasError) {
                    item { SeriesInlineRetry(onRetry = viewModel::refresh) }
                }
            }
        }

        item {
            Button(onClick = viewModel::refresh, modifier = Modifier.fillMaxWidth()) {
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

@Composable
private fun SeriesFilterRow(
    selected: SeriesFilter,
    onSelected: (SeriesFilter) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SeriesFilter.entries.forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelected(filter) },
                label = { Text(filter.label()) },
            )
        }
    }
}

@Composable
private fun SeriesFilter.label(): String = when (this) {
    SeriesFilter.ALL -> stringResource(R.string.filter_all)
    SeriesFilter.PUBLISHED -> stringResource(R.string.status_published)
    SeriesFilter.DRAFT -> stringResource(R.string.status_draft)
    SeriesFilter.FEATURED -> stringResource(R.string.filter_featured)
}
