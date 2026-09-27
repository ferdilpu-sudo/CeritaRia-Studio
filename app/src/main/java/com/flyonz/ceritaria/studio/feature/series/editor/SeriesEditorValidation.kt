package com.flyonz.ceritaria.studio.feature.series.editor

enum class SeriesEditorField {
    SLUG,
    TITLE,
    SHORT_SYNOPSIS,
    SYNOPSIS,
    GENRES,
    COVER_URL,
    HERO_URL,
    SEO_TITLE,
    SEO_DESCRIPTION,
}

enum class SeriesValidationIssue {
    REQUIRED,
    INVALID_SLUG,
    TOO_LONG,
    INVALID_URL,
}

data class SeriesValidationError(
    val field: SeriesEditorField,
    val issue: SeriesValidationIssue,
)
