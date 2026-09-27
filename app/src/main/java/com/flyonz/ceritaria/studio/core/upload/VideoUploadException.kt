package com.flyonz.ceritaria.studio.core.upload

enum class VideoUploadErrorCode {
    JOB_NOT_FOUND,
    EPISODE_NOT_SAVED,
    SOURCE_NOT_READY,
    MULTIPART_STATE_INVALID,
    MULTIPART_ETAG_MISSING,
    REMOTE_SESSION_FAILED,
    TRANSFER_FAILED,
}

class VideoUploadException(
    val code: VideoUploadErrorCode,
    cause: Throwable? = null,
) : IllegalStateException(code.name, cause)
