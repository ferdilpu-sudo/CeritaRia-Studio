package com.flyonz.ceritaria.studio.core.upload

import com.flyonz.ceritaria.studio.core.database.videojob.VideoJob

interface VideoUploadRepository {
    suspend fun upload(
        jobId: String,
        onProgress: suspend (UploadProgress) -> Unit = {},
    ): VideoJob

    suspend fun cancel(jobId: String): VideoJob
}
