package com.flyonz.ceritaria.studio.core.upload.execution

interface VideoUploadScheduler {
    fun enqueue(
        jobId: String,
        totalBytes: Long,
    ): VideoUploadScheduleResult

    suspend fun cancel(jobId: String)
}

enum class VideoUploadScheduleResult {
    SCHEDULED,
    REJECTED,
}
