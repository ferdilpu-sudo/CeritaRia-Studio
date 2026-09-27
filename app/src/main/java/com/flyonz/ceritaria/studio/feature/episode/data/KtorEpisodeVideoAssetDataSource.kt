package com.flyonz.ceritaria.studio.feature.episode.data

import com.flyonz.ceritaria.studio.core.auth.AccessTokenProvider
import com.flyonz.ceritaria.studio.core.config.CeritariaApiConfig
import com.flyonz.ceritaria.studio.core.network.VideoApiHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable

@Singleton
class KtorEpisodeVideoAssetDataSource @Inject constructor(
    @VideoApiHttpClient private val client: HttpClient,
    private val config: CeritariaApiConfig,
    private val tokenProvider: AccessTokenProvider,
) : EpisodeVideoAssetDataSource {
    override suspend fun attachReadyAsset(
        episodeId: String,
        assetId: String,
    ): EpisodeVideoAttachmentDto {
        if (!config.isConfigured) {
            throw EpisodeVideoAssetApiException("API_NOT_CONFIGURED", 0)
        }
        val token = tokenProvider.accessToken()
            ?: throw EpisodeVideoAssetApiException("AUTH_REQUIRED", 401)
        val response = client.post(
            config.baseUrl + "/api/episodes/" + episodeId +
                "/video-assets/" + assetId + "/attach",
        ) {
            bearerAuth(token)
        }
        response.requireSuccess()
        return response.body()
    }

    override suspend fun getPreview(assetId: String): EpisodeVideoPreviewDto {
        if (!config.isConfigured) {
            throw EpisodeVideoAssetApiException("API_NOT_CONFIGURED", 0)
        }
        val token = tokenProvider.accessToken()
            ?: throw EpisodeVideoAssetApiException("AUTH_REQUIRED", 401)
        val response = client.get(
            config.baseUrl + "/api/video-assets/" + assetId + "/preview",
        ) {
            bearerAuth(token)
        }
        response.requireSuccess()
        return response.body()
    }

    private suspend fun HttpResponse.requireSuccess() {
        if (status.isSuccess()) return
        val code = runCatching { body<EpisodeVideoAssetErrorDto>().error }
            .getOrDefault("HTTP_" + status.value)
        throw EpisodeVideoAssetApiException(code, status.value)
    }
}

@Serializable
private data class EpisodeVideoAssetErrorDto(
    val error: String,
)
