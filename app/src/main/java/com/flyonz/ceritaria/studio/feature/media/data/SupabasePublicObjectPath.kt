package com.flyonz.ceritaria.studio.feature.media.data

import java.net.URI

fun publicObjectPathOrNull(
    publicUrl: String?,
    expectedBucket: String,
): String? {
    if (publicUrl.isNullOrBlank()) return null
    val path = runCatching { URI(publicUrl).path }.getOrNull() ?: return null
    val marker = "/storage/v1/object/public/$expectedBucket/"
    if (!path.startsWith(marker)) return null
    return path.removePrefix(marker).takeIf { it.isNotBlank() }
}
