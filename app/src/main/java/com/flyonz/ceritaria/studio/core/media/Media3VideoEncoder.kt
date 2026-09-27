package com.flyonz.ceritaria.studio.core.media

import android.content.Context
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.AudioEncoderSettings
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.flyonz.ceritaria.studio.core.coroutines.MainDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@UnstableApi
@Singleton
class Media3VideoEncoder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val outputPlanner: VideoOutputPlanner,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
) : VideoEncoder {
    override suspend fun encode(
        sourceUri: String,
        metadata: VideoMetadata,
        outputFile: File,
        onProgress: suspend (EncodingProgress) -> Unit,
    ): VideoEncodeResult = withContext(mainDispatcher) {
        coroutineScope {
            outputFile.parentFile?.mkdirs()
            outputFile.delete()

            val preset = StreamingPreset()
            val editedMediaItem = buildEditedMediaItem(sourceUri, metadata, preset)
            val completion = CompletableDeferred<Unit>()
            val listener = completionListener(completion)
            val transformer = buildTransformer(preset, listener)
            val progressJob = launch {
                reportProgress(transformer, completion, onProgress)
            }

            try {
                transformer.start(editedMediaItem, outputFile.absolutePath)
                completion.await()
                onProgress(EncodingProgress(100))
                VideoEncodeResult(
                    outputUri = outputFile.toURI().toString(),
                    sizeBytes = outputFile.length(),
                )
            } catch (error: CancellationException) {
                transformer.cancel()
                outputFile.delete()
                throw error
            } catch (error: Throwable) {
                outputFile.delete()
                throw error
            } finally {
                progressJob.cancel()
                transformer.removeListener(listener)
            }
        }
    }

    private fun buildEditedMediaItem(
        sourceUri: String,
        metadata: VideoMetadata,
        preset: StreamingPreset,
    ): EditedMediaItem {
        val videoEffects = mutableListOf<Effect>()
        outputPlanner.plan(metadata)?.targetShortSide?.let { shortSide ->
            videoEffects += Presentation.createForShortSide(shortSide)
        }
        return EditedMediaItem.Builder(MediaItem.fromUri(sourceUri))
            .setFrameRate(preset.maxFrameRate.toInt())
            .setEffects(Effects(emptyList(), videoEffects))
            .build()
    }

    private fun buildTransformer(
        preset: StreamingPreset,
        listener: Transformer.Listener,
    ): Transformer {
        val audioSettings = AudioEncoderSettings.Builder()
            .setBitrate(preset.targetAudioBitrate)
            .build()
        val encoderFactory = DefaultEncoderFactory.Builder(context)
            .setRequestedAudioEncoderSettings(audioSettings)
            .build()

        return Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .setEncoderFactory(encoderFactory)
            .addListener(listener)
            .build()
    }

    private fun completionListener(
        completion: CompletableDeferred<Unit>,
    ): Transformer.Listener = object : Transformer.Listener {
        override fun onCompleted(
            composition: Composition,
            exportResult: ExportResult,
        ) {
            completion.complete(Unit)
        }

        override fun onError(
            composition: Composition,
            exportResult: ExportResult,
            exportException: ExportException,
        ) {
            completion.completeExceptionally(
                VideoEncoderException(
                    code = exportException.toEncoderErrorCode(),
                    cause = exportException,
                ),
            )
        }
    }

    private fun ExportException.toEncoderErrorCode(): VideoEncoderErrorCode = when (errorCode) {
        ExportException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        ExportException.ERROR_CODE_ENCODING_FORMAT_UNSUPPORTED,
        -> VideoEncoderErrorCode.UNSUPPORTED_CODEC
        else -> VideoEncoderErrorCode.EXPORT_FAILED
    }

    private suspend fun reportProgress(
        transformer: Transformer,
        completion: CompletableDeferred<Unit>,
        onProgress: suspend (EncodingProgress) -> Unit,
    ) {
        val holder = ProgressHolder()
        while (!completion.isCompleted) {
            if (transformer.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                onProgress(EncodingProgress(holder.progress.coerceIn(0, 99)))
            }
            delay(PROGRESS_INTERVAL_MS)
        }
    }

    private companion object {
        const val PROGRESS_INTERVAL_MS = 400L
    }
}
