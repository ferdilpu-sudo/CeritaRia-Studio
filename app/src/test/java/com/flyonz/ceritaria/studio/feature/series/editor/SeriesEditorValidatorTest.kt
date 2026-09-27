package com.flyonz.ceritaria.studio.feature.series.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeriesEditorValidatorTest {
    @Test
    fun validFormHasNoErrors() {
        val errors = SeriesEditorValidator.validate(
            validForm().copy(coverUrl = "https://example.com/cover.webp"),
        )

        assertTrue(errors.isEmpty())
    }

    @Test
    fun slugMustMatchProductionKebabCaseContract() {
        val errors = SeriesEditorValidator.validate(
            validForm().copy(slug = "Wajah Kedua"),
        )

        assertEquals(
            SeriesValidationIssue.INVALID_SLUG,
            errors.first { it.field == SeriesEditorField.SLUG }.issue,
        )
    }

    @Test
    fun productionLengthLimitsAreEnforced() {
        val errors = SeriesEditorValidator.validate(
            validForm().copy(shortSynopsis = "x".repeat(321)),
        )

        assertEquals(
            SeriesValidationIssue.TOO_LONG,
            errors.first { it.field == SeriesEditorField.SHORT_SYNOPSIS }.issue,
        )
    }

    @Test
    fun mapperTrimsValuesAndKeepsAtMostTwelveGenres() {
        val form = validForm().copy(
            title = "  Wajah Kedua  ",
            genres = (1..15).joinToString(",") { " Genre $it " },
            seoTitle = "  ",
        )

        val command = form.toSaveCommand(id = null, existingPublishedAt = null)

        assertEquals("Wajah Kedua", command.title)
        assertEquals(12, command.genres.size)
        assertEquals("Genre 1", command.genres.first())
        assertEquals(null, command.seoTitle)
    }

    private fun validForm() = SeriesEditorForm(
        slug = "wajah-kedua",
        title = "Wajah Kedua",
    )
}
