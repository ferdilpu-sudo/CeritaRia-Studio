package com.flyonz.ceritaria.studio.core.upload

import com.flyonz.ceritaria.studio.core.network.R2UploadHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.content.OutgoingContent
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeFully
import java.io.EOFException
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

@Singleton
class KtorR2UploadDataSource @Inject constructor(
    @R2UploadHttpClient private val client: HttpClient,
    private val sourceReader: VideoUploadSourceReader,
) : R2UploadDataSource {
    override suspend fun put(
        url: String,
        headers: Map<String, String>,
        source: VideoTransferSource,
        offsetBytes: Long,
        lengthBytes: Long,
        onProgress: suspend (sentBytes: Long) -> Unit,
    ): R2PutResult {
        require(url.startsWith("https://")) { "R2 upload URL must use HTTPS." }
        require(offsetBytes >= 0L) { "Upload offset cannot be negative." }
        require(lengthBytes > 0L) { "Upload length must be positive." }
        require(offsetBytes + lengthBytes <= source.sizeBytes) {
            "Upload range exceeds the video source."
        }

        val response = client.put(url) {
            headers.forEach { (name, value) -> header(name, value) }
            setBody(
                object : OutgoingContent.WriteChannelContent() {
                    override val contentLength: Long = lengthBytes

                    override suspend fun writeTo(channel: ByteWriteChannel) {
                        streamRange(
                            channel = channel,
                            source = source,
                            offsetBytes = offsetBytes,
                            lengthBytes = lengthBytes,
                            onProgress = onProgress,
                        )
                    }
                },
            )
        }

        response.requireSuccess()
        return R2PutResult(
            etag = response.headers[HttpHeaders.ETag],
        )
    }

    private suspend fun streamRange(
        channel: ByteWriteChannel,
        source: VideoTransferSource,
        offsetBytes: Long,
        lengthBytes: Long,
        onProgress: suspend (Long) -> Unit,
    ) {
        openSource(source.uri, offsetBytes).use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            var remaining = lengthBytes
            var sent = 0L

            while (remaining > 0L) {
                currentCoroutineContext().ensureActive()
                val requested = minOf(buffer.size.toLong(), remaining).toInt()
                val read = input.read(buffer, 0, requested)
                if (read < 0) {
                    throw EOFException("Video source ended before upload range completed.")
                }
                channel.writeFully(buffer, 0, read)
                sent += read
                remaining -= read
                onProgress(sent)
            }
        }
    }

    private fun openSource(sourceUri: String, offsetBytes: Long): java.io.InputStream =
        try {
            sourceReader.open(sourceUri, offsetBytes)
        } catch (error: FileNotFoundException) {
            throw VideoUploadException(VideoUploadErrorCode.SOURCE_NOT_READY, error)
        } catch (error: SecurityException) {
            throw VideoUploadException(VideoUploadErrorCode.SOURCE_NOT_READY, error)
        } catch (error: IllegalArgumentException) {
            throw VideoUploadException(VideoUploadErrorCode.SOURCE_NOT_READY, error)
        }

    private fun HttpResponse.requireSuccess() {
        if (!status.isSuccess()) {
            throw R2UploadException("R2_HTTP_" + status.value)
        }
    }

    private companion object {
        const val BUFFER_SIZE = 128 * 1024
    }
}

class R2UploadException(
    val code: String,
) : IllegalStateException(code)
