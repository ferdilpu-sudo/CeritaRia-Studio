package com.flyonz.ceritaria.studio.feature.series.domain

enum class SeriesFilter {
    ALL,
    PUBLISHED,
    DRAFT,
    FEATURED,
}

data class SeriesQuery(
    val page: Int = 0,
    val pageSize: Int = 20,
    val search: String = "",
    val filter: SeriesFilter = SeriesFilter.ALL,
)
