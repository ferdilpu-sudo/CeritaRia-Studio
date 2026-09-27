package com.flyonz.ceritaria.studio.feature.media.data

import com.flyonz.ceritaria.studio.feature.media.domain.ImageMediaSlot
import com.flyonz.ceritaria.studio.feature.media.domain.ImageSelection
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageMediaRepositoryImplTest {
    @Test
    fun replaceUploadsThenUpdatesReferenceThenCleansOldObject() = runTest {
        val events = mutableListOf<String>()
        val storage = FakeStorage(events)
        val references = FakeReferences(events)
        val repository = ImageMediaRepositoryImpl(storage, references)

        val result = repository.replace(
            ownerId = "owner-1",
            slot = ImageMediaSlot.SERIES_COVER,
            operationId = "op-1",
            file = File("cover.webp"),
            selection = selection(),
            oldPublicUrl = "https://project.supabase.co/storage/v1/object/public/series-media/owner-1/old.webp",
            onUploadProgress = {},
        )

        assertEquals("https://cdn/series-media/owner-1/op-1.webp", result)
        assertEquals(
            listOf("upload", "reference:https://cdn/series-media/owner-1/op-1.webp", "cleanup"),
            events,
        )
    }

    @Test
    fun failedReferenceUpdateRollsBackNewObjectAndDoesNotCleanOld() = runTest {
        val events = mutableListOf<String>()
        val storage = FakeStorage(events)
        val references = FakeReferences(events, fail = true)
        val repository = ImageMediaRepositoryImpl(storage, references)

        runCatching {
            repository.replace(
                ownerId = "owner-1",
                slot = ImageMediaSlot.SERIES_COVER,
                operationId = "op-1",
                file = File("cover.webp"),
                selection = selection(),
                oldPublicUrl = "https://project.supabase.co/storage/v1/object/public/series-media/owner-1/old.webp",
                onUploadProgress = {},
            )
        }

        assertEquals(listOf("upload", "reference:failed", "rollback"), events)
    }

    @Test
    fun removeClearsReferenceBeforeCleanup() = runTest {
        val events = mutableListOf<String>()
        val repository = ImageMediaRepositoryImpl(
            FakeStorage(events),
            FakeReferences(events),
        )

        repository.remove(
            ownerId = "owner-1",
            slot = ImageMediaSlot.EPISODE_THUMBNAIL,
            oldPublicUrl = "https://project.supabase.co/storage/v1/object/public/episode-media/owner-1/old.webp",
        )

        assertEquals(listOf("reference:null", "cleanup"), events)
    }

    private class FakeStorage(
        private val events: MutableList<String>,
    ) : ImageStorageDataSource {
        override suspend fun upload(
            bucket: String,
            path: String,
            file: File,
            onProgress: suspend (Int) -> Unit,
        ): String {
            events += "upload"
            onProgress(100)
            return "https://cdn/$bucket/$path"
        }

        override suspend fun deleteObject(bucket: String, path: String) {
            events += "rollback"
        }

        override suspend fun deleteOwnedPublicUrl(
            expectedBucket: String,
            publicUrl: String?,
        ) {
            events += "cleanup"
        }
    }

    private class FakeReferences(
        private val events: MutableList<String>,
        private val fail: Boolean = false,
    ) : ImageReferenceDataSource {
        override suspend fun updateReference(
            ownerId: String,
            slot: ImageMediaSlot,
            publicUrl: String?,
        ) {
            if (fail) {
                events += "reference:failed"
                error("write failed")
            }
            events += "reference:$publicUrl"
        }
    }

    private fun selection() = ImageSelection(
        uri = "content://cover",
        mimeType = "image/webp",
        sizeBytes = 1024,
        extension = "webp",
    )
}
