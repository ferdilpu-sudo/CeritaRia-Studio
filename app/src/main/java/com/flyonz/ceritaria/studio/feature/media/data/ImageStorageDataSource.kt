package com.flyonz.ceritaria.studio.feature.media.data

import java.io.File

interface ImageStorageDataSource {
    suspend fun upload(
        bucket: String,
        path: String,
        file: File,
    ): String

    suspend fun deleteObject(bucket: String, path: String)

    suspend fun deleteOwnedPublicUrl(
        expectedBucket: String,
        publicUrl: String?,
    )
}
