package com.flyonz.ceritaria.studio.feature.series.detail

sealed interface SeriesDetailEffect {
    data object Deleted : SeriesDetailEffect
}
