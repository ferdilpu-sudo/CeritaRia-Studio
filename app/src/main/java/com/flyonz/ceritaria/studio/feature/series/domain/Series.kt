package com.flyonz.ceritaria.studio.feature.series.domain

import com.flyonz.ceritaria.studio.core.model.PublishStatus
import java.time.Instant

data class Series(
    val id: String,
    val slug: String,
    val title: String,
    val shortSynopsis: String?,
    val synopsis: String?,
    val genres: List<String>,
    val coverUrl: String?,
    val heroUrl: String?,
    val isFeatured: Boolean,
    val publishStatus: PublishStatus,
    val publishedAt: Instant?,
    val seoTitle: String?,
    val seoDescription: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?,
)
