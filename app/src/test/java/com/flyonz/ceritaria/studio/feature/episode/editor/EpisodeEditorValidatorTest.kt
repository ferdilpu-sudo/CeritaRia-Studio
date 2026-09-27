package com.flyonz.ceritaria.studio.feature.episode.editor

import com.flyonz.ceritaria.studio.feature.episode.domain.EditableVideoProvider

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
    fun attachedR2FormIsValidWithoutLegacyUrl() {
        val form = validForm().copy(
            videoProvider = "r2",
            videoUrl = "",
            videoAssetId = "22222222-2222-2222-2222-222222222222",
        )

        val errors = EpisodeEditorValidator.validate(form)

        assertTrue(errors.isEmpty())
        val command = form.toSaveCommand(id = "episode-1", existingPublishedAt = null)
        assertEquals(EditableVideoProvider.R2, command.videoProvider)
        assertEquals(null, command.videoUrl)
        assertEquals("22222222-2222-2222-2222-222222222222", command.videoAssetId)
    }

    @Test
    fun r2WithoutAttachedAssetIsRejected() {
        val errors = EpisodeEditorValidator.validate(
            validForm().copy(videoProvider = "r2", videoUrl = "", videoAssetId = null),
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
