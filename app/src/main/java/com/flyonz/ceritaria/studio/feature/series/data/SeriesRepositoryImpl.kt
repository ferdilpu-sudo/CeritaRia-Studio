package com.flyonz.ceritaria.studio.feature.series.data

import com.flyonz.ceritaria.studio.core.error.AppError
import com.flyonz.ceritaria.studio.core.error.AppResult
import com.flyonz.ceritaria.studio.core.model.PagedResult
import com.flyonz.ceritaria.studio.feature.series.domain.Series
import com.flyonz.ceritaria.studio.feature.series.domain.SeriesQuery
import com.flyonz.ceritaria.studio.feature.series.domain.SeriesRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeriesRepositoryImpl @Inject constructor(
    private val dataSource: SeriesDataSource,
) : SeriesRepository {
    override suspend fun getSeries(query: SeriesQuery): AppResult<PagedResult<Series>> =
        runRead {
            val rows = dataSource.fetchSeries(query)
            val visibleRows = rows.take(query.pageSize)
            PagedResult(
                items = visibleRows.map(SeriesRowDto::toDomain),
                page = query.page,
                hasMore = rows.size > query.pageSize,
            )
        }

    override suspend fun getSeriesById(id: String): AppResult<Series?> =
        runRead { dataSource.fetchSeriesById(id)?.toDomain() }

    private suspend fun <T> runRead(block: suspend () -> T): AppResult<T> = try {
        AppResult.Success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: IllegalArgumentException) {
        AppResult.Failure(AppError.Configuration)
    } catch (_: Throwable) {
        AppResult.Failure(AppError.Network)
    }
}
