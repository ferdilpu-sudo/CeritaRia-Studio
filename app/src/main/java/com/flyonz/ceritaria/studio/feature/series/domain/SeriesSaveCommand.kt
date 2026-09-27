package com.flyonz.ceritaria.studio.feature.series.domain

import java.time.Instant

data class SeriesSaveCommand(
    val id: String?,
    val slug: String,
    val title: String,
    val shortSynopsis: String?,
    val synopsis: String?,
    val genres: List<String>,
    val coverUrl: String?,
    val heroUrl: String?,
    val isFeatured: Boolean,
    val isPublished: Boolean,
    val existingPublishedAt: Instant?,
    val seoTitle: String?,
    val seoDescription: String?,
)
