package com.flyonz.ceritaria.studio.feature.series.editor

import java.net.URI

object SeriesEditorValidator {
    fun validate(form: SeriesEditorForm): List<SeriesValidationError> = buildList {
        validateSlug(form.slug)?.let(::add)
        validateTitle(form.title)?.let(::add)
        checkMax(form.shortSynopsis, 320, SeriesEditorField.SHORT_SYNOPSIS)?.let(::add)
        checkMax(form.synopsis, 8000, SeriesEditorField.SYNOPSIS)?.let(::add)
        checkMax(form.genres, 400, SeriesEditorField.GENRES)?.let(::add)
        validateUrl(form.coverUrl, SeriesEditorField.COVER_URL)?.let(::add)
        validateUrl(form.heroUrl, SeriesEditorField.HERO_URL)?.let(::add)
        checkMax(form.seoTitle, 200, SeriesEditorField.SEO_TITLE)?.let(::add)
        checkMax(form.seoDescription, 320, SeriesEditorField.SEO_DESCRIPTION)?.let(::add)
    }

    private fun validateSlug(value: String): SeriesValidationError? {
        val slug = value.trim()
        return when {
            slug.isEmpty() -> error(SeriesEditorField.SLUG, SeriesValidationIssue.REQUIRED)
            slug.length !in 2..160 -> error(SeriesEditorField.SLUG, SeriesValidationIssue.INVALID_SLUG)
            !SLUG_PATTERN.matches(slug) -> error(SeriesEditorField.SLUG, SeriesValidationIssue.INVALID_SLUG)
            else -> null
        }
    }

    private fun validateTitle(value: String): SeriesValidationError? {
        val title = value.trim()
        return when {
            title.isEmpty() -> error(SeriesEditorField.TITLE, SeriesValidationIssue.REQUIRED)
            title.length !in 2..200 -> error(SeriesEditorField.TITLE, SeriesValidationIssue.TOO_LONG)
            else -> null
        }
    }

    private fun checkMax(
        value: String,
        max: Int,
        field: SeriesEditorField,
    ): SeriesValidationError? =
        if (value.trim().length > max) error(field, SeriesValidationIssue.TOO_LONG) else null

    private fun validateUrl(
        value: String,
        field: SeriesEditorField,
    ): SeriesValidationError? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        val valid = runCatching { URI(trimmed).isAbsolute }.getOrDefault(false)
        return if (valid) null else error(field, SeriesValidationIssue.INVALID_URL)
    }

    private fun error(
        field: SeriesEditorField,
        issue: SeriesValidationIssue,
    ) = SeriesValidationError(field, issue)

    private val SLUG_PATTERN = Regex("^[a-z0-9]+(?:-[a-z0-9]+)*$")
}
