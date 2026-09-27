package com.flyonz.ceritaria.studio.feature.series.editor

import com.flyonz.ceritaria.studio.feature.series.domain.SeriesSaveCommand
import java.time.Instant

fun SeriesEditorForm.toSaveCommand(
    id: String?,
    existingPublishedAt: Instant?,
): SeriesSaveCommand = SeriesSaveCommand(
    id = id,
    slug = slug.trim(),
    title = title.trim(),
    shortSynopsis = shortSynopsis.trim().ifEmpty { null },
    synopsis = synopsis.trim().ifEmpty { null },
    genres = genres.split(",")
        .map(String::trim)
        .filter(String::isNotEmpty)
        .take(12),
    coverUrl = coverUrl.trim().ifEmpty { null },
    heroUrl = heroUrl.trim().ifEmpty { null },
    isFeatured = isFeatured,
    isPublished = isPublished,
    existingPublishedAt = existingPublishedAt,
    seoTitle = seoTitle.trim().ifEmpty { null },
    seoDescription = seoDescription.trim().ifEmpty { null },
)
