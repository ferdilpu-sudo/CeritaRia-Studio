package com.flyonz.ceritaria.studio.core.media

import java.io.File

interface VideoEncoder {
    suspend fun encode(
        sourceUri: String,
        metadata: VideoMetadata,
        outputFile: File,
        onProgress: suspend (EncodingProgress) -> Unit,
    ): VideoEncodeResult
}
