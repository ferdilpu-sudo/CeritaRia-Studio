package com.flyonz.ceritaria.studio.feature.episode.data

import com.flyonz.ceritaria.studio.core.error.AppError
import com.flyonz.ceritaria.studio.core.error.AppResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeVideoAssetRepositoryImplTest {
    @Test
    fun successfulAttachMapsServerResponse() = runTest {
        val repository = EpisodeVideoAssetRepositoryImpl(
            FakeDataSource(
                EpisodeVideoAttachmentDto(
                    episodeId = "episode-1",
                    assetId = "asset-new",
                    status = "ATTACHED",
                    replacedAssetId = "asset-old",
                ),
            ),
        )

        val result = repository.attachReadyAsset("episode-1", "asset-new")

        assertTrue(result is AppResult.Success)
        val value = (result as AppResult.Success).value
        assertEquals("asset-new", value.assetId)
        assertEquals("asset-old", value.replacedAssetId)
    }

    @Test
    fun conflictMapsToAppConflict() = runTest {
        val repository = EpisodeVideoAssetRepositoryImpl(
            FakeDataSource(
                failure = EpisodeVideoAssetApiException(
                    code = "VIDEO_ASSET_NOT_ATTACHABLE",
                    statusCode = 409,
                ),
            ),
        )

        val result = repository.attachReadyAsset("episode-1", "asset-1")

        assertEquals(AppResult.Failure(AppError.Conflict), result)
    }

    @Test
    fun unexpectedAttachmentStatusIsRejected() = runTest {
        val repository = EpisodeVideoAssetRepositoryImpl(
            FakeDataSource(
                EpisodeVideoAttachmentDto(
                    episodeId = "episode-1",
                    assetId = "asset-1",
                    status = "READY",
                ),
            ),
        )

        val result = repository.attachReadyAsset("episode-1", "asset-1")

        assertTrue(result is AppResult.Failure)
    }

    private class FakeDataSource(
        private val response: EpisodeVideoAttachmentDto? = null,
        private val failure: Throwable? = null,
    ) : EpisodeVideoAssetDataSource {
        override suspend fun attachReadyAsset(
            episodeId: String,
            assetId: String,
        ): EpisodeVideoAttachmentDto {
            failure?.let { throw it }
            return requireNotNull(response)
        }
    }
}
