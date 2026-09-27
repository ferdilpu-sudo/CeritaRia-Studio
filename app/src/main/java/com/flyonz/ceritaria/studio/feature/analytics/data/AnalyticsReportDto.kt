package com.flyonz.ceritaria.studio.feature.analytics.data

import kotlinx.serialization.Serializable

@Serializable
data class AnalyticsReportDto(
    val days: Int,
    val summary: AnalyticsSummaryDto,
    val hourly: List<AnalyticsPointDto> = emptyList(),
    val topPages: List<AnalyticsPageDto> = emptyList(),
    val devices: List<AnalyticsPointDto> = emptyList(),
    val referrers: List<AnalyticsPointDto> = emptyList(),
    val events: List<AnalyticsPointDto> = emptyList(),
)

@Serializable
data class AnalyticsSummaryDto(
    val todayPageviews: Long = 0,
    val todayVisitors: Long = 0,
    val periodPageviews: Long = 0,
    val periodVisitors: Long = 0,
    val periodSessions: Long = 0,
    val totalEvents: Long = 0,
)

@Serializable
data class AnalyticsPointDto(
    val label: String,
    val value: Long,
    val visitors: Long? = null,
)

@Serializable
data class AnalyticsPageDto(
    val path: String,
    val views: Long,
    val visitors: Long,
)
