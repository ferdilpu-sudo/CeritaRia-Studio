package com.flyonz.ceritaria.studio.core.media

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.inspector.MetadataRetriever
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.guava.await

@UnstableApi
@Singleton
class Media3VideoInspector @Inject constructor(
    @ApplicationContext private val context: Context,
) : VideoInspector {
    override suspend fun inspect(sourceUri: String): VideoMetadata {
        val uri = Uri.parse(sourceUri)
        val mediaItem = MediaItem.fromUri(uri)

        return MetadataRetriever.Builder(context, mediaItem).build().use { retriever ->
            val trackGroups = retriever.retrieveTrackGroups().await()
            val durationUs = retriever.retrieveDurationUs().await()
            val formats = buildList {
                for (groupIndex in 0 until trackGroups.length) {
                    val group = trackGroups[groupIndex]
                    for (formatIndex in 0 until group.length) {
                        add(group.getFormat(formatIndex))
                    }
                }
            }
            val video = formats.firstOrNull { MimeTypes.isVideo(it.sampleMimeType) }
            val audio = formats.firstOrNull { MimeTypes.isAudio(it.sampleMimeType) }

            VideoMetadata(
                containerMimeType = resolveContainerMime(uri, video),
                sizeBytes = querySize(uri) ?: 0L,
                durationMs = durationUs
                    .takeUnless { it == C.TIME_UNSET }
                    ?.div(MICROS_PER_MILLISECOND),
                width = video?.width.knownInt(),
                height = video?.height.knownInt(),
                rotationDegrees = video?.rotationDegrees,
                frameRate = video?.frameRate.knownFloat(),
                videoMimeType = video?.sampleMimeType,
                audioMimeType = audio?.sampleMimeType,
                audioBitrate = audio?.averageBitrate.knownInt(),
                audioSampleRate = audio?.sampleRate.knownInt(),
                hasVideo = video != null,
                hasAudio = audio != null,
            )
        }
    }

    private fun resolveContainerMime(uri: Uri, videoFormat: Format?): String? =
        context.contentResolver.getType(uri) ?: videoFormat?.containerMimeType

    private fun querySize(uri: Uri): Long? =
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val index = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (index < 0 || cursor.isNull(index)) null else cursor.getLong(index)
        }

    private fun Int?.knownInt(): Int? =
        this?.takeIf { it != Format.NO_VALUE && it > 0 }

    private fun Float?.knownFloat(): Float? =
        this?.takeIf { it > 0f }

    private companion object {
        const val MICROS_PER_MILLISECOND = 1_000L
    }
}
