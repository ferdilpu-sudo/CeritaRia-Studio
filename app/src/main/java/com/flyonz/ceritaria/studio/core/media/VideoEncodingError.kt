package com.flyonz.ceritaria.studio.core.media

enum class VideoEncodingErrorCode {
    JOB_NOT_FOUND,
    ENCODING_NOT_REQUIRED,
    INSUFFICIENT_STORAGE,
    UNSUPPORTED_CODEC,
    ENCODER_FAILED,
}

class VideoEncodingException(
    val code: VideoEncodingErrorCode,
    cause: Throwable? = null,
) : IllegalStateException(code.name, cause)
