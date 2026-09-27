package com.flyonz.ceritaria.studio.feature.episode.data

import com.flyonz.ceritaria.studio.core.error.AppError
import com.flyonz.ceritaria.studio.core.error.AppResult
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeVideoAssetRepository
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeVideoAttachment
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeVideoPreview
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

@Singleton
class EpisodeVideoAssetRepositoryImpl @Inject constructor(
    private val dataSource: EpisodeVideoAssetDataSource,
) : EpisodeVideoAssetRepository {
    override suspend fun attachReadyAsset(
        episodeId: String,
        assetId: String,
    ): AppResult<EpisodeVideoAttachment> = try {
        val response = dataSource.attachReadyAsset(episodeId, assetId)
        require(response.status == "ATTACHED")
        AppResult.Success(
            EpisodeVideoAttachment(
                episodeId = response.episodeId,
                assetId = response.assetId,
                replacedAssetId = response.replacedAssetId,
            ),
        )
    } catch (error: CancellationException) {
        throw error
    } catch (error: EpisodeVideoAssetApiException) {
        AppResult.Failure(error.toAppError())
    } catch (_: IllegalArgumentException) {
        AppResult.Failure(AppError.Validation("Invalid attachment response"))
    } catch (_: Throwable) {
        AppResult.Failure(AppError.Network)
    }

    override suspend fun getPreview(assetId: String): AppResult<EpisodeVideoPreview> = try {
        val response = dataSource.getPreview(assetId)
        require(response.status == "READY")
        AppResult.Success(
            EpisodeVideoPreview(
                assetId = response.assetId,
                url = response.url,
                expiresInSeconds = response.expiresInSeconds,
            ),
        )
    } catch (error: CancellationException) {
        throw error
    } catch (error: EpisodeVideoAssetApiException) {
        AppResult.Failure(error.toAppError())
    } catch (_: IllegalArgumentException) {
        AppResult.Failure(AppError.Validation("Invalid preview response"))
    } catch (_: Throwable) {
        AppResult.Failure(AppError.Network)
    }

    private fun EpisodeVideoAssetApiException.toAppError(): AppError = when (statusCode) {
        0 -> AppError.Configuration
        401 -> AppError.Authentication
        403 -> AppError.Authorization
        409 -> AppError.Conflict
        else -> AppError.Network
    }
}
