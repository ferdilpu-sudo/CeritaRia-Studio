package com.flyonz.ceritaria.studio.feature.media.domain

object ImageSelectionValidator {
    const val MAX_SIZE_BYTES = 5L * 1024L * 1024L

    fun validate(
        mimeType: String?,
        sizeBytes: Long?,
    ): ImageValidationResult {
        val normalizedMime = mimeType?.lowercase()?.trim()
        val extension = when (normalizedMime) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> return ImageValidationResult.Invalid(ImageValidationIssue.UNSUPPORTED_TYPE)
        }

        if (sizeBytes != null && sizeBytes > MAX_SIZE_BYTES) {
            return ImageValidationResult.Invalid(ImageValidationIssue.TOO_LARGE)
        }
        if (sizeBytes != null && sizeBytes <= 0L) {
            return ImageValidationResult.Invalid(ImageValidationIssue.EMPTY_FILE)
        }

        return ImageValidationResult.Valid(
            mimeType = requireNotNull(normalizedMime),
            extension = extension,
        )
    }
}

sealed interface ImageValidationResult {
    data class Valid(
        val mimeType: String,
        val extension: String,
    ) : ImageValidationResult

    data class Invalid(val issue: ImageValidationIssue) : ImageValidationResult
}

enum class ImageValidationIssue {
    UNSUPPORTED_TYPE,
    TOO_LARGE,
    EMPTY_FILE,
}
