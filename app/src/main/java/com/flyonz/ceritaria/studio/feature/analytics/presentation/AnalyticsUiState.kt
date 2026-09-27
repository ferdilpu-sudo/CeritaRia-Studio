package com.flyonz.ceritaria.studio.feature.analytics.presentation

import com.flyonz.ceritaria.studio.feature.analytics.domain.AnalyticsRange
import com.flyonz.ceritaria.studio.feature.analytics.domain.AnalyticsReport

data class AnalyticsUiState(
    val range: AnalyticsRange = AnalyticsRange.DAYS_7,
    val report: AnalyticsReport? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val hasError: Boolean = false,
) {
    val isEmpty: Boolean
        get() = report?.isEmpty == true
}
