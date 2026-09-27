package com.flyonz.ceritaria.studio.core.upload

import kotlinx.serialization.Serializable

@Serializable
internal data class CreateVideoUploadRequestDto(
    val episodeId: String,
    val mimeType: String = "video/mp4",
    val sizeBytes: Long,
)

@Serializable
internal data class CreateVideoUploadResponseDto(
    val assetId: String,
    val sessionId: String,
    val expiresAt: String,
    val upload: VideoUploadTargetDto,
)

@Serializable
internal data class VideoUploadTargetDto(
    val mode: String,
    val url: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val partSizeBytes: Long? = null,
    val partCount: Int? = null,
)

@Serializable
internal data class AuthorizePartRequestDto(
    val partNumber: Int,
)

@Serializable
internal data class AuthorizePartResponseDto(
    val partNumber: Int,
    val sizeBytes: Long,
    val url: String,
)

@Serializable
internal data class CompletedVideoPartDto(
    val partNumber: Int,
    val etag: String,
)

@Serializable
internal data class CompleteVideoUploadRequestDto(
    val parts: List<CompletedVideoPartDto>,
)

@Serializable
internal data class FinalizeVideoUploadResponseDto(
    val assetId: String,
    val status: String,
    val sizeBytes: Long,
    val etag: String? = null,
)

@Serializable
internal data class VideoUploadStatusResponseDto(
    val sessionId: String,
    val assetId: String,
    val mode: String,
    val sessionStatus: String,
    val assetStatus: String,
    val expiresAt: String,
    val expired: Boolean,
    val expectedSizeBytes: Long,
    val actualSizeBytes: Long? = null,
    val partSizeBytes: Long? = null,
    val partCount: Int? = null,
)

@Serializable
internal data class VideoUploadErrorDto(
    val error: String,
)
