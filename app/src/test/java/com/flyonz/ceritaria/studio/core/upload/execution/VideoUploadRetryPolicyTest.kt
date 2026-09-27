package com.flyonz.ceritaria.studio.core.upload.execution

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoUploadRetryPolicyTest {
    @Test
    fun authFailuresDoNotRetry() {
        assertFalse(VideoUploadRetryPolicy.isRetryable("AUTH_REQUIRED"))
        assertFalse(VideoUploadRetryPolicy.isRetryable("UNAUTHENTICATED"))
        assertFalse(VideoUploadRetryPolicy.isRetryable("FORBIDDEN"))
    }

    @Test
    fun configurationAndInvalidSourceDoNotRetry() {
        assertFalse(VideoUploadRetryPolicy.isRetryable("API_NOT_CONFIGURED"))
        assertFalse(VideoUploadRetryPolicy.isRetryable("EPISODE_NOT_FOUND"))
        assertFalse(VideoUploadRetryPolicy.isRetryable("SOURCE_NOT_READY"))
        assertFalse(VideoUploadRetryPolicy.isRetryable("FILE_TOO_LARGE"))
    }

    @Test
    fun transientServerAndTransferFailuresRetry() {
        assertTrue(VideoUploadRetryPolicy.isRetryable("UPLOAD_SESSION_LOOKUP_FAILED"))
        assertTrue(VideoUploadRetryPolicy.isRetryable("VIDEO_UPLOAD_INTERNAL_ERROR"))
        assertTrue(VideoUploadRetryPolicy.isRetryable("R2_HTTP_503"))
        assertTrue(VideoUploadRetryPolicy.isRetryable("TRANSFER_FAILED"))
    }

    @Test
    fun expiredOrStaleRemoteSessionCanRetryThroughRecovery() {
        assertTrue(VideoUploadRetryPolicy.isRetryable("UPLOAD_SESSION_EXPIRED"))
        assertTrue(VideoUploadRetryPolicy.isRetryable("UPLOAD_SESSION_NOT_FOUND"))
        assertTrue(VideoUploadRetryPolicy.isRetryable("R2_HTTP_403"))
    }
}
