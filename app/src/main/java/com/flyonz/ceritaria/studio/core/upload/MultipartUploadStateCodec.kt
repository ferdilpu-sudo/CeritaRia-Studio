package com.flyonz.ceritaria.studio.core.upload

import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MultipartUploadStateCodec @Inject constructor() {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(state: MultipartUploadState): String = json.encodeToString(
        PersistedMultipartState(
            partSizeBytes = state.partSizeBytes,
            partCount = state.partCount,
            completedParts = state.completedParts.map {
                PersistedCompletedPart(it.partNumber, it.etag)
            },
        ),
    )

    fun decode(value: String?): MultipartUploadState? {
        if (value.isNullOrBlank()) return null
        val persisted = runCatching {
            json.decodeFromString<PersistedMultipartState>(value)
        }.getOrNull() ?: return null

        if (persisted.partSizeBytes <= 0L || persisted.partCount <= 0) return null
        return MultipartUploadState(
            partSizeBytes = persisted.partSizeBytes,
            partCount = persisted.partCount,
            completedParts = persisted.completedParts.map {
                CompletedVideoPart(it.partNumber, it.etag)
            },
        )
    }
}

@Serializable
private data class PersistedMultipartState(
    val partSizeBytes: Long,
    val partCount: Int,
    val completedParts: List<PersistedCompletedPart> = emptyList(),
)

@Serializable
private data class PersistedCompletedPart(
    val partNumber: Int,
    val etag: String,
)
