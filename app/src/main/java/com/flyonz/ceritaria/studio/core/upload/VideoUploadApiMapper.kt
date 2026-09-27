package com.flyonz.ceritaria.studio.core.upload

internal fun CreateVideoUploadResponseDto.toDomain(): VideoUploadSession {
    val target = when (upload.mode) {
        "SINGLE" -> VideoUploadTarget.Single(
            url = requireNotNull(upload.url),
            headers = upload.headers,
        )
        "MULTIPART" -> VideoUploadTarget.Multipart(
            partSizeBytes = requireNotNull(upload.partSizeBytes),
            partCount = requireNotNull(upload.partCount),
        )
        else -> error("Unsupported upload mode: " + upload.mode)
    }
    return VideoUploadSession(
        assetId = assetId,
        sessionId = sessionId,
        expiresAt = expiresAt,
        target = target,
    )
}

internal fun AuthorizePartResponseDto.toDomain() = VideoUploadPartAuthorization(
    partNumber = partNumber,
    sizeBytes = sizeBytes,
    url = url,
)

internal fun FinalizeVideoUploadResponseDto.toDomain() = FinalizedVideoAsset(
    assetId = assetId,
    sizeBytes = sizeBytes,
    etag = etag,
)

internal fun VideoUploadStatusResponseDto.toDomain() = RemoteVideoUploadStatus(
    sessionId = sessionId,
    assetId = assetId,
    mode = mode,
    sessionStatus = sessionStatus,
    assetStatus = assetStatus,
    expiresAt = expiresAt,
    expired = expired,
    expectedSizeBytes = expectedSizeBytes,
    actualSizeBytes = actualSizeBytes,
    partSizeBytes = partSizeBytes,
    partCount = partCount,
)
