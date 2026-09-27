package com.flyonz.ceritaria.studio.feature.series.detail

import com.flyonz.ceritaria.studio.feature.series.domain.Series

data class SeriesDetailUiState(
    val isLoading: Boolean = true,
    val series: Series? = null,
    val hasError: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteError: Boolean = false,
)
