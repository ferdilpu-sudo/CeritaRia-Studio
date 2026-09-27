package com.flyonz.ceritaria.studio.feature.series.list

import com.flyonz.ceritaria.studio.feature.series.domain.Series
import com.flyonz.ceritaria.studio.feature.series.domain.SeriesFilter

data class SeriesListUiState(
    val items: List<Series> = emptyList(),
    val query: String = "",
    val filter: SeriesFilter = SeriesFilter.ALL,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val nextPage: Int = 0,
    val hasError: Boolean = false,
)
