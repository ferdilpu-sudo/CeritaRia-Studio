package com.flyonz.ceritaria.studio.core.upload

import java.io.InputStream

interface VideoUploadSourceReader {
    fun open(sourceUri: String, offsetBytes: Long): InputStream
}
