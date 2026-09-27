package com.flyonz.ceritaria.studio.feature.series.data

import com.flyonz.ceritaria.studio.feature.series.domain.SeriesQuery

interface SeriesDataSource {
    suspend fun fetchSeries(query: SeriesQuery): List<SeriesRowDto>
    suspend fun fetchSeriesById(id: String): SeriesRowDto?
}
