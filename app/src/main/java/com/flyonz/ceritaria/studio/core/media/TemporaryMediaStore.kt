package com.flyonz.ceritaria.studio.core.media

import java.io.File
import java.time.Instant

interface TemporaryMediaStore {
    fun encodedOutput(jobId: String): File
    fun hasCapacity(estimatedOutputBytes: Long): Boolean
    fun deleteEncodedOutput(jobId: String)
    fun cleanupEncodedOutputsOlderThan(cutoff: Instant)
}
