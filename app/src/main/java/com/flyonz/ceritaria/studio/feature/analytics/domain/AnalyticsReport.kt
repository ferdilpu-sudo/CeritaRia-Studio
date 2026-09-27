package com.flyonz.ceritaria.studio.feature.analytics.domain

data class AnalyticsReport(
    val days: Int,
    val summary: AnalyticsSummary,
    val hourly: List<AnalyticsPoint>,
    val topPages: List<AnalyticsPage>,
    val devices: List<AnalyticsPoint>,
    val referrers: List<AnalyticsPoint>,
    val events: List<AnalyticsPoint>,
) {
    val isEmpty: Boolean
        get() = summary.totalEvents == 0 &&
            hourly.all { it.value == 0 } &&
            topPages.isEmpty() &&
            devices.isEmpty() &&
            referrers.isEmpty() &&
            events.isEmpty()
}

data class AnalyticsSummary(
    val todayPageviews: Long,
    val todayVisitors: Long,
    val periodPageviews: Long,
    val periodVisitors: Long,
    val periodSessions: Long,
    val totalEvents: Long,
)

data class AnalyticsPoint(
    val label: String,
    val value: Long,
    val visitors: Long? = null,
)

data class AnalyticsPage(
    val path: String,
    val views: Long,
    val visitors: Long,
)
