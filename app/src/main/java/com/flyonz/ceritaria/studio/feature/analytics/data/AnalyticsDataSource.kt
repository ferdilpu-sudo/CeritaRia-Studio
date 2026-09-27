package com.flyonz.ceritaria.studio.feature.analytics.data

interface AnalyticsDataSource {
    suspend fun fetchReport(days: Int): AnalyticsReportDto
}
