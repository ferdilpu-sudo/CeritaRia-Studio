package com.flyonz.ceritaria.studio.feature.analytics.data

import com.flyonz.ceritaria.studio.core.error.AppError
import com.flyonz.ceritaria.studio.core.error.AppResult
import com.flyonz.ceritaria.studio.feature.analytics.domain.AnalyticsRange
import com.flyonz.ceritaria.studio.feature.analytics.domain.AnalyticsReport
import com.flyonz.ceritaria.studio.feature.analytics.domain.AnalyticsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

@Singleton
class AnalyticsRepositoryImpl @Inject constructor(
    private val dataSource: AnalyticsDataSource,
) : AnalyticsRepository {
    override suspend fun getReport(
        range: AnalyticsRange,
    ): AppResult<AnalyticsReport> = try {
        AppResult.Success(dataSource.fetchReport(range.days).toDomain())
    } catch (error: CancellationException) {
        throw error
    } catch (_: IllegalArgumentException) {
        AppResult.Failure(AppError.Configuration)
    } catch (_: Throwable) {
        AppResult.Failure(AppError.Network)
    }
}
