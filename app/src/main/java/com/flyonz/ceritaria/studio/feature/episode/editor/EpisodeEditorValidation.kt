package com.flyonz.ceritaria.studio.feature.episode.editor

enum class EpisodeEditorField {
    SERIES,
    EPISODE_NUMBER,
    SLUG,
    TITLE,
    SHORT_SYNOPSIS,
    RECAP,
    HIGHLIGHTS,
    VIDEO_PROVIDER,
    VIDEO_URL,
    THUMBNAIL_URL,
    DURATION,
    SEO_TITLE,
    SEO_DESCRIPTION,
}

enum class EpisodeValidationIssue {
    REQUIRED,
    INVALID_UUID,
    INVALID_NUMBER,
    INVALID_SLUG,
    TOO_LONG,
    INVALID_URL,
    UNSUPPORTED_PROVIDER,
    INVALID_VIDEO_URL,
}

data class EpisodeValidationError(
    val field: EpisodeEditorField,
    val issue: EpisodeValidationIssue,
)
