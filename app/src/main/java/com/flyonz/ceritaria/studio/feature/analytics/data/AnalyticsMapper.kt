package com.flyonz.ceritaria.studio.feature.analytics.data

import com.flyonz.ceritaria.studio.feature.analytics.domain.AnalyticsPage
import com.flyonz.ceritaria.studio.feature.analytics.domain.AnalyticsPoint
import com.flyonz.ceritaria.studio.feature.analytics.domain.AnalyticsReport
import com.flyonz.ceritaria.studio.feature.analytics.domain.AnalyticsSummary

fun AnalyticsReportDto.toDomain(): AnalyticsReport = AnalyticsReport(
    days = days,
    summary = AnalyticsSummary(
        todayPageviews = summary.todayPageviews,
        todayVisitors = summary.todayVisitors,
        periodPageviews = summary.periodPageviews,
        periodVisitors = summary.periodVisitors,
        periodSessions = summary.periodSessions,
        totalEvents = summary.totalEvents,
    ),
    hourly = hourly.map(AnalyticsPointDto::toDomain),
    topPages = topPages.map {
        AnalyticsPage(path = it.path, views = it.views, visitors = it.visitors)
    },
    devices = devices.map(AnalyticsPointDto::toDomain),
    referrers = referrers.map(AnalyticsPointDto::toDomain),
    events = events.map(AnalyticsPointDto::toDomain),
)

private fun AnalyticsPointDto.toDomain() = AnalyticsPoint(
    label = label,
    value = value,
    visitors = visitors,
)
