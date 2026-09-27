package com.flyonz.ceritaria.studio.core.media

interface VideoInspector {
    suspend fun inspect(sourceUri: String): VideoMetadata
}
