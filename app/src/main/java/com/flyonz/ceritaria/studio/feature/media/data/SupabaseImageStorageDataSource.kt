package com.flyonz.ceritaria.studio.feature.media.data

import com.flyonz.ceritaria.studio.core.config.SupabaseConfig
import com.flyonz.ceritaria.studio.core.network.SupabaseClientProvider
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseImageStorageDataSource @Inject constructor(
    private val clientProvider: SupabaseClientProvider,
    private val config: SupabaseConfig,
) : ImageStorageDataSource {
    override suspend fun upload(
        bucket: String,
        path: String,
        file: File,
    ): String {
        val bucketApi = requireClient().storage.from(bucket)
        bucketApi.upload(path, file) {
            upsert = true
        }
        return bucketApi.publicUrl(path)
    }

    override suspend fun deleteObject(bucket: String, path: String) {
        requireClient().storage.from(bucket).delete(path)
    }

    override suspend fun deleteOwnedPublicUrl(
        expectedBucket: String,
        publicUrl: String?,
    ) {
        val path = publicObjectPathOrNull(
            publicUrl = publicUrl,
            expectedBucket = expectedBucket,
            expectedProjectUrl = config.url,
        ) ?: return
        deleteObject(expectedBucket, path)
    }

    private fun requireClient() = requireNotNull(clientProvider.clientOrNull) {
        "Supabase is not configured."
    }
}
