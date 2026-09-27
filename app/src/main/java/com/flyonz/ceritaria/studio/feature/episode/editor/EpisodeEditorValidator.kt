package com.flyonz.ceritaria.studio.feature.episode.editor

import java.net.URI
import java.util.UUID

object EpisodeEditorValidator {
    fun validate(form: EpisodeEditorForm): List<EpisodeValidationError> = buildList {
        validateSeries(form.seriesId)?.let(::add)
        validateNumber(form.episodeNumber, 10_000, EpisodeEditorField.EPISODE_NUMBER)?.let(::add)
        validateSlug(form.slug)?.let(::add)
        validateTitle(form.title)?.let(::add)
        checkMax(form.shortSynopsis, 320, EpisodeEditorField.SHORT_SYNOPSIS)?.let(::add)
        checkMax(form.recap, 20_000, EpisodeEditorField.RECAP)?.let(::add)
        checkMax(form.highlights, 4_000, EpisodeEditorField.HIGHLIGHTS)?.let(::add)
        validateProvider(form.videoProvider)?.let(::add)
        validateVideoUrl(form.videoProvider, form.videoUrl)?.let(::add)
        validateR2Asset(form)?.let(::add)
        validateUrl(form.thumbnailUrl, EpisodeEditorField.THUMBNAIL_URL)?.let(::add)
        validateOptionalNumber(form.durationSeconds, 86_400, EpisodeEditorField.DURATION)?.let(::add)
        checkMax(form.seoTitle, 200, EpisodeEditorField.SEO_TITLE)?.let(::add)
        checkMax(form.seoDescription, 320, EpisodeEditorField.SEO_DESCRIPTION)?.let(::add)
    }

    private fun validateSeries(value: String): EpisodeValidationError? {
        val valid = runCatching { UUID.fromString(value.trim()) }.isSuccess
        return if (valid) null else error(EpisodeEditorField.SERIES, EpisodeValidationIssue.INVALID_UUID)
    }

    private fun validateNumber(
        value: String,
        max: Int,
        field: EpisodeEditorField,
    ): EpisodeValidationError? {
        val number = value.toIntOrNull()
        return if (number != null && number in 1..max) {
            null
        } else {
            error(field, EpisodeValidationIssue.INVALID_NUMBER)
        }
    }

    private fun validateOptionalNumber(
        value: String,
        max: Int,
        field: EpisodeEditorField,
    ): EpisodeValidationError? =
        if (value.trim().isEmpty()) null else validateNumber(value.trim(), max, field)

    private fun validateSlug(value: String): EpisodeValidationError? {
        val slug = value.trim()
        return when {
            slug.isEmpty() -> error(EpisodeEditorField.SLUG, EpisodeValidationIssue.REQUIRED)
            slug.length !in 2..160 -> error(EpisodeEditorField.SLUG, EpisodeValidationIssue.INVALID_SLUG)
            !SLUG_PATTERN.matches(slug) -> error(EpisodeEditorField.SLUG, EpisodeValidationIssue.INVALID_SLUG)
            else -> null
        }
    }

    private fun validateTitle(value: String): EpisodeValidationError? {
        val title = value.trim()
        return when {
            title.isEmpty() -> error(EpisodeEditorField.TITLE, EpisodeValidationIssue.REQUIRED)
            title.length !in 2..200 -> error(EpisodeEditorField.TITLE, EpisodeValidationIssue.TOO_LONG)
            else -> null
        }
    }

    private fun validateProvider(value: String): EpisodeValidationError? =
        if (value in SUPPORTED_PROVIDERS) null
        else error(EpisodeEditorField.VIDEO_PROVIDER, EpisodeValidationIssue.UNSUPPORTED_PROVIDER)

    private fun validateVideoUrl(
        provider: String,
        value: String,
    ): EpisodeValidationError? = when {
        provider == "r2" -> null
        value.trim().isEmpty() -> error(EpisodeEditorField.VIDEO_URL, EpisodeValidationIssue.REQUIRED)
        value.trim().length > 2048 -> error(EpisodeEditorField.VIDEO_URL, EpisodeValidationIssue.TOO_LONG)
        !EpisodeVideoUrlValidator.isValid(provider, value) ->
            error(EpisodeEditorField.VIDEO_URL, EpisodeValidationIssue.INVALID_VIDEO_URL)
        else -> null
    }

    private fun validateUrl(
        value: String,
        field: EpisodeEditorField,
    ): EpisodeValidationError? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        val valid = runCatching { URI(trimmed).isAbsolute }.getOrDefault(false)
        return if (valid) null else error(field, EpisodeValidationIssue.INVALID_URL)
    }

    private fun checkMax(
        value: String,
        max: Int,
        field: EpisodeEditorField,
    ): EpisodeValidationError? =
        if (value.trim().length > max) error(field, EpisodeValidationIssue.TOO_LONG) else null

    private fun error(
        field: EpisodeEditorField,
        issue: EpisodeValidationIssue,
    ) = EpisodeValidationError(field, issue)

    private val SLUG_PATTERN = Regex("^[a-z0-9]+(?:-[a-z0-9]+)*$")
    private fun validateR2Asset(form: EpisodeEditorForm): EpisodeValidationError? {
        if (form.videoProvider != "r2") return null
        val valid = form.videoAssetId
            ?.let { runCatching { UUID.fromString(it) }.isSuccess }
            ?: false
        return if (valid) null else {
            error(EpisodeEditorField.VIDEO_PROVIDER, EpisodeValidationIssue.UNSUPPORTED_PROVIDER)
        }
    }

    private val SUPPORTED_PROVIDERS = setOf("youtube", "facebook", "r2")
}
