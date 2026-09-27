package com.flyonz.ceritaria.studio.core.upload

interface R2UploadDataSource {
    suspend fun put(
        url: String,
        headers: Map<String, String>,
        source: VideoTransferSource,
        offsetBytes: Long,
        lengthBytes: Long,
        onProgress: suspend (sentBytes: Long) -> Unit,
    ): R2PutResult
}
