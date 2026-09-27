package com.flyonz.ceritaria.studio.feature.media.data

import com.flyonz.ceritaria.studio.core.config.SupabaseConfig
import com.flyonz.ceritaria.studio.core.network.SupabaseClientProvider
import io.github.jan.supabase.storage.UploadStatus
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.uploadAsFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.collect

@Singleton
class SupabaseImageStorageDataSource @Inject constructor(
    private val clientProvider: SupabaseClientProvider,
    private val config: SupabaseConfig,
) : ImageStorageDataSource {
    override suspend fun upload(
        bucket: String,
        path: String,
        file: File,
        onProgress: suspend (Int) -> Unit,
    ): String {
        val bucketApi = requireClient().storage.from(bucket)
        bucketApi.uploadAsFlow(path, file) {
            upsert = true
        }.collect { status ->
            when (status) {
                is UploadStatus.Progress -> {
                    val percent = if (status.contentLength > 0L) {
                        ((status.totalBytesSend * 100L) / status.contentLength)
                            .toInt()
                            .coerceIn(0, 100)
                    } else {
                        0
                    }
                    onProgress(percent)
                }
                is UploadStatus.Success -> onProgress(100)
            }
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
