package com.flyonz.ceritaria.studio.core.media

enum class VideoEncoderErrorCode {
    UNSUPPORTED_CODEC,
    EXPORT_FAILED,
}

class VideoEncoderException(
    val code: VideoEncoderErrorCode,
    cause: Throwable? = null,
) : IllegalStateException(code.name, cause)
