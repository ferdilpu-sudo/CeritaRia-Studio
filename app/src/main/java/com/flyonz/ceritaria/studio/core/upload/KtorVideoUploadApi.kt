package com.flyonz.ceritaria.studio.core.upload

import com.flyonz.ceritaria.studio.core.auth.AccessTokenProvider
import com.flyonz.ceritaria.studio.core.config.CeritariaApiConfig
import com.flyonz.ceritaria.studio.core.network.VideoApiHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.contentType
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KtorVideoUploadApi @Inject constructor(
    @VideoApiHttpClient private val client: HttpClient,
    private val config: CeritariaApiConfig,
    private val tokenProvider: AccessTokenProvider,
) : VideoUploadApi {
    override suspend fun createSession(
        episodeId: String,
        sizeBytes: Long,
    ): VideoUploadSession {
        val token = requireAccessToken()
        val response = client.post(endpoint("/api/video-uploads")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(CreateVideoUploadRequestDto(episodeId = episodeId, sizeBytes = sizeBytes))
        }
        return response.decode<CreateVideoUploadResponseDto>().toDomain()
    }

    override suspend fun authorizePart(
        sessionId: String,
        partNumber: Int,
    ): VideoUploadPartAuthorization {
        val token = requireAccessToken()
        val response = client.post(endpoint("/api/video-uploads/$sessionId/parts")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(AuthorizePartRequestDto(partNumber))
        }
        return response.decode<AuthorizePartResponseDto>().toDomain()
    }

    override suspend fun completeMultipart(
        sessionId: String,
        parts: List<CompletedVideoPart>,
    ) {
        val token = requireAccessToken()
        val response = client.post(endpoint("/api/video-uploads/$sessionId/complete")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(
                CompleteVideoUploadRequestDto(
                    parts.map { CompletedVideoPartDto(it.partNumber, it.etag) },
                ),
            )
        }
        response.requireSuccess()
    }

    override suspend fun finalizeUpload(sessionId: String): FinalizedVideoAsset {
        val token = requireAccessToken()
        return client.post(endpoint("/api/video-uploads/$sessionId/finalize")) {
            bearerAuth(token)
        }.decode<FinalizeVideoUploadResponseDto>().toDomain()
    }

    override suspend fun cancelUpload(sessionId: String) {
        val token = requireAccessToken()
        client.post(endpoint("/api/video-uploads/$sessionId/cancel")) {
            bearerAuth(token)
        }.requireSuccess()
    }

    override suspend fun getStatus(sessionId: String): RemoteVideoUploadStatus {
        val token = requireAccessToken()
        return client.get(endpoint("/api/video-uploads/$sessionId")) {
            bearerAuth(token)
        }.decode<VideoUploadStatusResponseDto>().toDomain()
    }

    private fun endpoint(path: String): String {
        if (!config.isConfigured) {
            throw VideoUploadApiException("API_NOT_CONFIGURED", 0)
        }
        return config.baseUrl + path
    }

    private suspend fun requireAccessToken(): String =
        tokenProvider.accessToken()
            ?: throw VideoUploadApiException("AUTH_REQUIRED", 401)

    private suspend inline fun <reified T> HttpResponse.decode(): T {
        requireSuccess()
        return body()
    }

    private suspend fun HttpResponse.requireSuccess() {
        if (status.isSuccess()) return
        val code = runCatching { body<VideoUploadErrorDto>().error }
            .getOrDefault("HTTP_" + status.value)
        throw VideoUploadApiException(code, status.value)
    }
}
