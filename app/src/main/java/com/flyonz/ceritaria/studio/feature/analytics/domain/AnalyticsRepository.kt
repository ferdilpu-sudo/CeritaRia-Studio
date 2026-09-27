package com.flyonz.ceritaria.studio.feature.analytics.domain

import com.flyonz.ceritaria.studio.core.error.AppResult

interface AnalyticsRepository {
    suspend fun getReport(range: AnalyticsRange): AppResult<AnalyticsReport>
}
