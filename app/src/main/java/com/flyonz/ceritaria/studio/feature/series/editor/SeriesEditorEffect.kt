package com.flyonz.ceritaria.studio.feature.series.editor

sealed interface SeriesEditorEffect {
    data class Saved(val seriesId: String) : SeriesEditorEffect
}
