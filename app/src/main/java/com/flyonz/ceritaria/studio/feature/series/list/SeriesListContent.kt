package com.flyonz.ceritaria.studio.feature.series.list

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
import com.flyonz.ceritaria.studio.feature.series.domain.SeriesFilter

@Composable
fun SeriesListContent(
    contentPadding: PaddingValues,
    state: SeriesListUiState,
    onQueryChange: (String) -> Unit,
    onFilterChange: (SeriesFilter) -> Unit,
    onSeriesClick: (String) -> Unit,
    onCreateSeries: () -> Unit,
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
            SeriesListHeader(
                query = state.query,
                filter = state.filter,
                onQueryChange = onQueryChange,
                onFilterChange = onFilterChange,
                onCreateSeries = onCreateSeries,
            )
        }

        when {
            state.isLoading -> item { SeriesLoadingState() }
            state.hasError && state.items.isEmpty() -> item { SeriesErrorState(onRetry) }
            state.items.isEmpty() -> item {
                SeriesEmptyState(hasQuery = state.query.isNotBlank())
            }
            else -> {
                items(state.items, key = { it.id }) { series ->
                    SeriesListRow(series = series, onClick = { onSeriesClick(series.id) })
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
                if (state.hasError) item { SeriesInlineRetry(onRetry) }
            }
        }
    }
}
