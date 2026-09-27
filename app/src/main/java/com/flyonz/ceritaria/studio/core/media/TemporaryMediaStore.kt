package com.flyonz.ceritaria.studio.core.media

import java.io.File

interface TemporaryMediaStore {
    fun encodedOutput(jobId: String): File
    fun hasEncodedOutput(jobId: String): Boolean
    fun hasCapacity(estimatedOutputBytes: Long): Boolean
    fun deleteEncodedOutput(jobId: String)
}
