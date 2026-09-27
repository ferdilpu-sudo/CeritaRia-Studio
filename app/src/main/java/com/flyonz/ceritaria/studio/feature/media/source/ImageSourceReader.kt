package com.flyonz.ceritaria.studio.feature.media.source

import com.flyonz.ceritaria.studio.feature.media.domain.ImageSelection
import java.io.File

interface ImageSourceReader {
    suspend fun prepare(uri: String, operationId: String): PreparedImage
}

data class PreparedImage(
    val file: File,
    val selection: ImageSelection,
)
