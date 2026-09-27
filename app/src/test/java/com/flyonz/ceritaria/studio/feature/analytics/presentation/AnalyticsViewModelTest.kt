package com.flyonz.ceritaria.studio.feature.analytics.presentation

import com.flyonz.ceritaria.studio.core.error.AppError
import com.flyonz.ceritaria.studio.core.error.AppResult
import com.flyonz.ceritaria.studio.feature.analytics.domain.AnalyticsRange
import com.flyonz.ceritaria.studio.feature.analytics.domain.AnalyticsReport
import com.flyonz.ceritaria.studio.feature.analytics.domain.AnalyticsRepository
import com.flyonz.ceritaria.studio.feature.analytics.domain.AnalyticsSummary
import com.flyonz.ceritaria.studio.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialLoadPublishesReport() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeRepository()
        val viewModel = AnalyticsViewModel(repository)

        advanceUntilIdle()

        assertEquals(AnalyticsRange.DAYS_7, viewModel.state.value.range)
        assertEquals(7, viewModel.state.value.report?.days)
        assertFalse(viewModel.state.value.hasError)
    }

    @Test
    fun changingRangeCancelsOlderRequest() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeRepository(delayDays7 = true)
        val viewModel = AnalyticsViewModel(repository)

        viewModel.setRange(AnalyticsRange.DAYS_30)
        advanceUntilIdle()

        assertEquals(AnalyticsRange.DAYS_30, viewModel.state.value.range)
        assertEquals(30, viewModel.state.value.report?.days)
    }

    @Test
    fun failedRefreshKeepsLastSuccessfulReport() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeRepository()
            val viewModel = AnalyticsViewModel(repository)
            advanceUntilIdle()
            repository.fail = true

            viewModel.refresh()
            advanceUntilIdle()

            assertEquals(7, viewModel.state.value.report?.days)
            assertTrue(viewModel.state.value.hasError)
            assertFalse(viewModel.state.value.isRefreshing)
        }

    private class FakeRepository(
        private val delayDays7: Boolean = false,
    ) : AnalyticsRepository {
        var fail = false

        override suspend fun getReport(
            range: AnalyticsRange,
        ): AppResult<AnalyticsReport> {
            if (delayDays7 && range == AnalyticsRange.DAYS_7) delay(1_000)
            if (fail) return AppResult.Failure(AppError.Network)
            return AppResult.Success(report(range.days))
        }
    }

    private companion object {
        fun report(days: Int) = AnalyticsReport(
            days = days,
            summary = AnalyticsSummary(
                todayPageviews = 10,
                todayVisitors = 7,
                periodPageviews = 100,
                periodVisitors = 50,
                periodSessions = 60,
                totalEvents = 130,
            ),
            hourly = emptyList(),
            topPages = emptyList(),
            devices = emptyList(),
            referrers = emptyList(),
            events = emptyList(),
        )
    }
}
