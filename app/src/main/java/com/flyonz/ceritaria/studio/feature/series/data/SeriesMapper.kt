package com.flyonz.ceritaria.studio.feature.series.data

import com.flyonz.ceritaria.studio.core.model.resolvePublishStatus
import com.flyonz.ceritaria.studio.feature.series.domain.Series
import java.time.Instant

fun SeriesRowDto.toDomain(): Series = Series(
    id = id,
    slug = slug,
    title = title,
    shortSynopsis = shortSynopsis,
    synopsis = synopsis,
    genres = genres,
    coverUrl = coverUrl,
    heroUrl = heroUrl,
    isFeatured = isFeatured,
    publishStatus = resolvePublishStatus(isPublished, publishedAt, deletedAt),
    publishedAt = publishedAt.toInstantOrNull(),
    seoTitle = seoTitle,
    seoDescription = seoDescription,
    createdAt = Instant.parse(createdAt),
    updatedAt = Instant.parse(updatedAt),
    deletedAt = deletedAt.toInstantOrNull(),
)

private fun String?.toInstantOrNull(): Instant? =
    this?.let { runCatching { Instant.parse(it) }.getOrNull() }
