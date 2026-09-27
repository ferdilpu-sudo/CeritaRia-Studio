package com.flyonz.ceritaria.studio.feature.media.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageSelectionValidatorTest {
    @Test
    fun acceptsProductionImageTypesAtLimit() {
        val result = ImageSelectionValidator.validate(
            mimeType = "image/webp",
            sizeBytes = ImageSelectionValidator.MAX_SIZE_BYTES,
        )

        assertTrue(result is ImageValidationResult.Valid)
        assertEquals("webp", (result as ImageValidationResult.Valid).extension)
    }

    @Test
    fun rejectsUnsupportedMimeType() {
        val result = ImageSelectionValidator.validate("image/gif", 1024)

        assertEquals(
            ImageValidationIssue.UNSUPPORTED_TYPE,
            (result as ImageValidationResult.Invalid).issue,
        )
    }

    @Test
    fun rejectsFileAboveFiveMiB() {
        val result = ImageSelectionValidator.validate(
            "image/jpeg",
            ImageSelectionValidator.MAX_SIZE_BYTES + 1,
        )

        assertEquals(
            ImageValidationIssue.TOO_LARGE,
            (result as ImageValidationResult.Invalid).issue,
        )
    }

    @Test
    fun unknownSizeCanBeValidatedAgainAfterCopy() {
        val result = ImageSelectionValidator.validate("image/png", null)

        assertTrue(result is ImageValidationResult.Valid)
    }
}
