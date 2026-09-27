package com.flyonz.ceritaria.studio.feature.episode.editor

import com.flyonz.ceritaria.studio.feature.episode.domain.EditableVideoProvider
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeSaveCommand
import java.time.Instant

fun EpisodeEditorForm.toSaveCommand(
    id: String?,
    existingPublishedAt: Instant?,
): EpisodeSaveCommand = EpisodeSaveCommand(
    id = id,
    seriesId = seriesId.trim(),
    episodeNumber = requireNotNull(episodeNumber.trim().toIntOrNull()),
    slug = slug.trim(),
    title = title.trim(),
    shortSynopsis = shortSynopsis.trim().ifEmpty { null },
    recap = recap.trim().ifEmpty { null },
    highlights = highlights
        .lineSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .take(12)
        .toList(),
    videoProvider = requireNotNull(
        EditableVideoProvider.entries.firstOrNull { it.rawValue == videoProvider },
    ),
    videoUrl = videoUrl.trim(),
    thumbnailUrl = thumbnailUrl.trim().ifEmpty { null },
    durationSeconds = durationSeconds.trim().ifEmpty { null }?.toInt(),
    isPublished = isPublished,
    existingPublishedAt = existingPublishedAt,
    seoTitle = seoTitle.trim().ifEmpty { null },
    seoDescription = seoDescription.trim().ifEmpty { null },
)
