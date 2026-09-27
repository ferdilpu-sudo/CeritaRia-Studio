package com.flyonz.ceritaria.studio.feature.series.domain

import com.flyonz.ceritaria.studio.core.error.AppResult
import com.flyonz.ceritaria.studio.core.model.PagedResult

interface SeriesRepository {
    suspend fun getSeries(query: SeriesQuery): AppResult<PagedResult<Series>>
    suspend fun getSeriesById(id: String): AppResult<Series?>
    suspend fun saveSeries(command: SeriesSaveCommand): AppResult<Series>
    suspend fun softDeleteSeries(id: String): AppResult<Unit>
}
