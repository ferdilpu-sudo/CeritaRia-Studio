package com.flyonz.ceritaria.studio.feature.episode.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeEditorValidatorTest {
    @Test
    fun validYouTubeFormHasNoErrors() {
        val errors = EpisodeEditorValidator.validate(validForm())

        assertTrue(errors.isEmpty())
    }

    @Test
    fun invalidEpisodeNumberMatchesProductionBounds() {
        val errors = EpisodeEditorValidator.validate(
            validForm().copy(episodeNumber = "10001"),
        )

        assertEquals(
            EpisodeValidationIssue.INVALID_NUMBER,
            errors.first { it.field == EpisodeEditorField.EPISODE_NUMBER }.issue,
        )
    }

    @Test
    fun unsupportedProviderCannotBeWrittenBack() {
        val errors = EpisodeEditorValidator.validate(
            validForm().copy(videoProvider = "r2"),
        )

        assertEquals(
            EpisodeValidationIssue.UNSUPPORTED_PROVIDER,
            errors.first { it.field == EpisodeEditorField.VIDEO_PROVIDER }.issue,
        )
    }

    @Test
    fun mapperKeepsOnlyFirstTwelveHighlights() {
        val form = validForm().copy(
            highlights = (1..15).joinToString("\n") { " Moment $it " },
        )

        val command = form.toSaveCommand(id = null, existingPublishedAt = null)

        assertEquals(12, command.highlights.size)
        assertEquals("Moment 1", command.highlights.first())
    }

    private fun validForm() = EpisodeEditorForm(
        seriesId = "11111111-1111-1111-1111-111111111111",
        episodeNumber = "1",
        slug = "episode-1",
        title = "Episode 1",
        videoProvider = "youtube",
        videoUrl = "https://youtu.be/dQw4w9WgXcQ",
    )
}
