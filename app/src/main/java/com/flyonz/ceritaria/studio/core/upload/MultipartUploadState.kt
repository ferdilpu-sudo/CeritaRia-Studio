package com.flyonz.ceritaria.studio.core.upload

data class MultipartUploadState(
    val partSizeBytes: Long,
    val partCount: Int,
    val completedParts: List<CompletedVideoPart>,
) {
    fun completedPart(partNumber: Int): CompletedVideoPart? =
        completedParts.firstOrNull { it.partNumber == partNumber }

    fun withCompleted(part: CompletedVideoPart): MultipartUploadState = copy(
        completedParts = (completedParts.filterNot { it.partNumber == part.partNumber } + part)
            .sortedBy { it.partNumber },
    )
}
