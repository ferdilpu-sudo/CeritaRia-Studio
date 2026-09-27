package com.flyonz.ceritaria.studio.core.upload

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoUploadApiMapperTest {
    @Test
    fun mapsSingleUploadTarget() {
        val result = CreateVideoUploadResponseDto(
            assetId = "asset",
            sessionId = "session",
            expiresAt = "2026-09-28T00:00:00Z",
            upload = VideoUploadTargetDto(
                mode = "SINGLE",
                url = "https://upload.example",
                headers = mapOf("Content-Type" to "video/mp4"),
            ),
        ).toDomain()

        assertTrue(result.target is VideoUploadTarget.Single)
        assertEquals("asset", result.assetId)
    }

    @Test
    fun mapsMultipartUsingLongPartSize() {
        val result = CreateVideoUploadResponseDto(
            assetId = "asset",
            sessionId = "session",
            expiresAt = "2026-09-28T00:00:00Z",
            upload = VideoUploadTargetDto(
                mode = "MULTIPART",
                partSizeBytes = 3_000_000_000L,
                partCount = 2,
            ),
        ).toDomain()

        val target = result.target as VideoUploadTarget.Multipart
        assertEquals(3_000_000_000L, target.partSizeBytes)
        assertEquals(2, target.partCount)
    }
}
