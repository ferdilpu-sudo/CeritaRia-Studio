package com.flyonz.ceritaria.studio.feature.series.editor

import com.flyonz.ceritaria.studio.core.model.PublishStatus
import com.flyonz.ceritaria.studio.feature.series.domain.Series

data class SeriesEditorForm(
    val slug: String = "",
    val title: String = "",
    val shortSynopsis: String = "",
    val synopsis: String = "",
    val genres: String = "",
    val coverUrl: String = "",
    val heroUrl: String = "",
    val isFeatured: Boolean = false,
    val isPublished: Boolean = false,
    val seoTitle: String = "",
    val seoDescription: String = "",
) {
    companion object {
        fun from(series: Series): SeriesEditorForm = SeriesEditorForm(
            slug = series.slug,
            title = series.title,
            shortSynopsis = series.shortSynopsis.orEmpty(),
            synopsis = series.synopsis.orEmpty(),
            genres = series.genres.joinToString(", "),
            coverUrl = series.coverUrl.orEmpty(),
            heroUrl = series.heroUrl.orEmpty(),
            isFeatured = series.isFeatured,
            isPublished = series.publishStatus == PublishStatus.PUBLISHED,
            seoTitle = series.seoTitle.orEmpty(),
            seoDescription = series.seoDescription.orEmpty(),
        )
    }
}
