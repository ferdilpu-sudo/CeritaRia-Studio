package com.flyonz.ceritaria.studio.feature.media.source

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.flyonz.ceritaria.studio.core.coroutines.IoDispatcher
import com.flyonz.ceritaria.studio.feature.media.domain.ImageSelection
import com.flyonz.ceritaria.studio.feature.media.domain.ImageSelectionValidator
import com.flyonz.ceritaria.studio.feature.media.domain.ImageValidationIssue
import com.flyonz.ceritaria.studio.feature.media.domain.ImageValidationResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

@Singleton
class AndroidImageSourceReader @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ImageSourceReader {
    override suspend fun prepare(uri: String, operationId: String): PreparedImage =
        withContext(ioDispatcher) {
            val parsedUri = Uri.parse(uri)
            val mimeType = context.contentResolver.getType(parsedUri)
            val declaredSize = querySize(parsedUri)
            val validation = ImageSelectionValidator.validate(mimeType, declaredSize)
            val valid = validation as? ImageValidationResult.Valid
                ?: throw invalidSource(validation)

            val directory = File(context.cacheDir, TEMP_DIRECTORY).apply { mkdirs() }
            val target = File(directory, "$operationId.${valid.extension}")
            try {
                copyWithLimit(parsedUri, target)
                val actualSize = target.length()
                val actualValidation = ImageSelectionValidator.validate(valid.mimeType, actualSize)
                if (actualValidation !is ImageValidationResult.Valid) {
                    throw invalidSource(actualValidation)
                }
                PreparedImage(
                    file = target,
                    selection = ImageSelection(
                        uri = uri,
                        mimeType = valid.mimeType,
                        sizeBytes = actualSize,
                        extension = valid.extension,
                    ),
                )
            } catch (error: Throwable) {
                target.delete()
                throw error
            }
        }

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

    private suspend fun copyWithLimit(uri: Uri, target: File) {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw ImageSourceException(null, "Selected image cannot be opened.")
        input.use { source ->
            target.outputStream().buffered().use { sink ->
                val buffer = ByteArray(BUFFER_SIZE)
                var total = 0L
                while (true) {
                    coroutineContext.ensureActive()
                    val read = source.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > ImageSelectionValidator.MAX_SIZE_BYTES) {
                        throw ImageSourceException(
                            issue = ImageValidationIssue.TOO_LARGE,
                            message = "Selected image exceeds the 5 MiB limit.",
                        )
                    }
                    sink.write(buffer, 0, read)
                }
            }
        }
    }

    private fun invalidSource(result: ImageValidationResult): ImageSourceException {
        val invalid = result as ImageValidationResult.Invalid
        return ImageSourceException(invalid.issue, "Selected image is not valid.")
    }

    private companion object {
        const val TEMP_DIRECTORY = "image-media"
        const val BUFFER_SIZE = 64 * 1024
    }
}
