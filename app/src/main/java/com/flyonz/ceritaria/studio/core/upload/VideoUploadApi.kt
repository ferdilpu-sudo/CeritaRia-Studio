package com.flyonz.ceritaria.studio.core.upload

interface VideoUploadApi {
    suspend fun createSession(
        episodeId: String,
        sizeBytes: Long,
    ): VideoUploadSession

    suspend fun authorizePart(
        sessionId: String,
        partNumber: Int,
    ): VideoUploadPartAuthorization

    suspend fun completeMultipart(
        sessionId: String,
        parts: List<CompletedVideoPart>,
    )

    suspend fun finalizeUpload(sessionId: String): FinalizedVideoAsset
    suspend fun cancelUpload(sessionId: String)
    suspend fun getStatus(sessionId: String): RemoteVideoUploadStatus
}
