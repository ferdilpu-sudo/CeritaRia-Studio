package com.flyonz.ceritaria.studio.core.upload

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.FilterInputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidVideoUploadSourceReader @Inject constructor(
    @ApplicationContext private val context: Context,
) : VideoUploadSourceReader {
    override fun open(sourceUri: String, offsetBytes: Long): InputStream {
        require(offsetBytes >= 0L) { "Offset cannot be negative." }
        val uri = Uri.parse(sourceUri)
        return when (uri.scheme?.lowercase()) {
            "file" -> openFileUri(uri, offsetBytes)
            "content" -> openContentUri(uri, offsetBytes)
            else -> throw IllegalArgumentException("Unsupported video source URI.")
        }
    }

    private fun openFileUri(uri: Uri, offsetBytes: Long): InputStream {
        val path = requireNotNull(uri.path) { "File URI path is missing." }
        val input = FileInputStream(File(path))
        positionOrSkip(input, offsetBytes)
        return input
    }

    private fun openContentUri(uri: Uri, offsetBytes: Long): InputStream {
        val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalArgumentException("Video source cannot be opened.")
        return try {
            val input = FileInputStream(descriptor.fileDescriptor)
            positionOrSkip(input, offsetBytes)
            DescriptorInputStream(input, descriptor)
        } catch (error: Throwable) {
            descriptor.close()
            throw error
        }
    }

    private fun positionOrSkip(input: FileInputStream, offsetBytes: Long) {
        if (offsetBytes == 0L) return
        val positioned = runCatching {
            input.channel.position(offsetBytes)
        }.isSuccess
        if (!positioned) {
            skipFully(input, offsetBytes)
        }
    }

    private fun skipFully(input: InputStream, bytes: Long) {
        var remaining = bytes
        val scratch = ByteArray(SKIP_BUFFER_SIZE)
        while (remaining > 0L) {
            val skipped = input.skip(remaining)
            if (skipped > 0L) {
                remaining -= skipped
                continue
            }
            val read = input.read(
                scratch,
                0,
                minOf(scratch.size.toLong(), remaining).toInt(),
            )
            if (read < 0) {
                throw IllegalArgumentException("Video source ended before requested offset.")
            }
            remaining -= read
        }
    }

    private class DescriptorInputStream(
        input: InputStream,
        private val descriptor: ParcelFileDescriptor,
    ) : FilterInputStream(input) {
        override fun close() {
            try {
                super.close()
            } finally {
                descriptor.close()
            }
        }
    }

    private companion object {
        const val SKIP_BUFFER_SIZE = 64 * 1024
    }
}
