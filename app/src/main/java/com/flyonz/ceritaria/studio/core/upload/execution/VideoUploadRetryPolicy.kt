package com.flyonz.ceritaria.studio.core.upload.execution

object VideoUploadRetryPolicy {
    fun isRetryable(errorCode: String): Boolean =
        errorCode !in NON_RETRYABLE_ERRORS

    private val NON_RETRYABLE_ERRORS = setOf(
        "API_NOT_CONFIGURED",
        "AUTH_REQUIRED",
        "UNAUTHENTICATED",
        "FORBIDDEN",
        "EPISODE_NOT_FOUND",
        "FILE_TOO_LARGE",
        "MULTIPART_LIMIT_EXCEEDED",
        "INVALID_REQUEST",
        "JOB_NOT_FOUND",
        "EPISODE_NOT_SAVED",
        "SOURCE_NOT_READY",
        "MULTIPART_STATE_INVALID",
    )
}
