package com.flyonz.ceritaria.studio.feature.media.data

import java.net.URI

fun publicObjectPathOrNull(
    publicUrl: String?,
    expectedBucket: String,
    expectedProjectUrl: String,
): String? {
    if (publicUrl.isNullOrBlank()) return null
    val source = runCatching { URI(publicUrl) }.getOrNull() ?: return null
    val project = runCatching { URI(expectedProjectUrl) }.getOrNull() ?: return null

    if (!source.scheme.equals("https", ignoreCase = true)) return null
    if (!source.host.equals(project.host, ignoreCase = true)) return null

    val path = source.path ?: return null
    val marker = "/storage/v1/object/public/$expectedBucket/"
    if (!path.startsWith(marker)) return null
    return path.removePrefix(marker).takeIf { it.isNotBlank() }
}
