package com.flyonz.ceritaria.studio.core.media

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidTemporaryMediaStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : TemporaryMediaStore {
    override fun encodedOutput(jobId: String): File {
        require(jobId.isNotBlank()) { "Video job ID cannot be blank." }
        val directory = encodedDirectory().apply { mkdirs() }
        return File(directory, "$jobId.mp4")
    }

    override fun hasCapacity(estimatedOutputBytes: Long): Boolean {
        val required = estimatedOutputBytes
            .coerceAtLeast(MINIMUM_ESTIMATE_BYTES)
            .plus(RESERVED_HEADROOM_BYTES)
        return encodedDirectory().usableSpace >= required
    }

    override fun deleteEncodedOutput(jobId: String) {
        encodedOutput(jobId).delete()
    }

    override fun cleanupEncodedOutputsOlderThan(cutoff: Instant) {
        val cutoffMs = cutoff.toEpochMilli()
        encodedDirectory().listFiles()
            ?.filter { it.isFile && it.lastModified() < cutoffMs }
            ?.forEach(File::delete)
    }

    private fun encodedDirectory(): File =
        File(context.noBackupFilesDir, DIRECTORY_NAME)

    private companion object {
        const val DIRECTORY_NAME = "encoded-video"
        const val MINIMUM_ESTIMATE_BYTES = 64L * 1024L * 1024L
        const val RESERVED_HEADROOM_BYTES = 128L * 1024L * 1024L
    }
}
