package com.flyonz.ceritaria.studio.feature.episode.data

import com.flyonz.ceritaria.studio.core.model.resolvePublishStatus
import com.flyonz.ceritaria.studio.feature.episode.domain.Episode
import com.flyonz.ceritaria.studio.feature.episode.domain.videoProviderFrom
import java.time.Instant

fun EpisodeRowDto.toDomain(): Episode = Episode(
    id = id,
    seriesId = seriesId,
    seriesTitle = series?.title,
    episodeNumber = episodeNumber,
    slug = slug,
    title = title,
    shortSynopsis = shortSynopsis,
    recap = recap,
    highlights = highlights,
    videoProvider = videoProviderFrom(videoProvider),
    videoUrl = videoUrl,
    thumbnailUrl = thumbnailUrl,
    durationSeconds = durationSeconds,
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
