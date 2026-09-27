package com.flyonz.ceritaria.studio.feature.analytics.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AnalyticsMapperTest {
    @Test
    fun mapsDashboardRpcShapeWithoutDroppingMetrics() {
        val dto = AnalyticsReportDto(
            days = 30,
            summary = AnalyticsSummaryDto(
                todayPageviews = 12,
                todayVisitors = 8,
                periodPageviews = 300,
                periodVisitors = 140,
                periodSessions = 180,
                totalEvents = 420,
            ),
            hourly = listOf(
                AnalyticsPointDto(label = "21:00", value = 14, visitors = 9),
            ),
            topPages = listOf(
                AnalyticsPageDto(path = "/series/test", views = 90, visitors = 55),
            ),
            devices = listOf(AnalyticsPointDto("mobile", 200)),
            referrers = listOf(AnalyticsPointDto("direct", 150)),
            events = listOf(AnalyticsPointDto("play_intent", 60)),
        )

        val report = dto.toDomain()

        assertEquals(30, report.days)
        assertEquals(300L, report.summary.periodPageviews)
        assertEquals(9L, report.hourly.single().visitors)
        assertEquals("/series/test", report.topPages.single().path)
        assertEquals("play_intent", report.events.single().label)
        assertFalse(report.isEmpty)
    }
}
