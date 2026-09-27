package com.flyonz.ceritaria.studio.core.media

import java.security.MessageDigest

object VideoSourceFingerprint {
    fun create(sourceUri: String, metadata: VideoMetadata): String {
        val canonical = listOf(
            sourceUri,
            metadata.sizeBytes,
            metadata.durationMs,
            metadata.width,
            metadata.height,
            metadata.rotationDegrees,
            metadata.frameRate,
            metadata.videoMimeType,
            metadata.audioMimeType,
        ).joinToString(separator = "|")

        return MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
