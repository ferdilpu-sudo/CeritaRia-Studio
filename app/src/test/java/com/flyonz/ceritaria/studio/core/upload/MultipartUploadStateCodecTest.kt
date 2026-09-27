package com.flyonz.ceritaria.studio.core.upload

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MultipartUploadStateCodecTest {
    private val codec = MultipartUploadStateCodec()

    @Test
    fun roundTripPreservesCompletedParts() {
        val original = MultipartUploadState(
            partSizeBytes = 16_777_216,
            partCount = 3,
            completedParts = listOf(
                CompletedVideoPart(1, "etag-one"),
                CompletedVideoPart(2, "etag-two"),
            ),
        )

        assertEquals(original, codec.decode(codec.encode(original)))
    }

    @Test
    fun invalidPersistedStateReturnsNull() {
        assertNull(codec.decode("""{"partSizeBytes":0,"partCount":2}"""))
    }
}
