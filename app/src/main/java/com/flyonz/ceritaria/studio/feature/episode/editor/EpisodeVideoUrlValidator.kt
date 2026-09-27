package com.flyonz.ceritaria.studio.feature.episode.editor

import java.net.URI
import java.net.URLDecoder

object EpisodeVideoUrlValidator {
    fun isValid(provider: String, value: String): Boolean = when (provider) {
        "youtube" -> isYouTube(value)
        "facebook" -> isFacebook(value)
        else -> false
    }

    private fun isYouTube(value: String): Boolean {
        val uri = parseHttps(value) ?: return false
        val host = normalizeHost(uri.host ?: return false)
        val pathSegments = uri.path.orEmpty().split("/").filter(String::isNotBlank)

        if (host == "youtu.be") {
            return pathSegments.firstOrNull()?.matches(VIDEO_ID_PATTERN) == true
        }
        if (!isYouTubeHost(host)) return false

        val queryId = queryParameter(uri, "v")
        val pathId = if (pathSegments.firstOrNull() in YOUTUBE_PATH_PREFIXES) {
            pathSegments.getOrNull(1)
        } else {
            null
        }
        return (queryId ?: pathId)?.matches(VIDEO_ID_PATTERN) == true
    }

    private fun isFacebook(value: String): Boolean {
        val uri = parseHttps(value) ?: return false
        val host = uri.host?.lowercase() ?: return false
        if (host !in FACEBOOK_HOSTS) return false
        val path = uri.path.orEmpty().replace(Regex("/{2,}"), "/")

        return FACEBOOK_REEL.matches(path) ||
            FACEBOOK_VIDEO.matches(path) ||
            (FACEBOOK_WATCH.matches(path) && queryParameter(uri, "v") != null) ||
            (FACEBOOK_VIDEO_PHP.matches(path) && queryParameter(uri, "v") != null)
    }

    private fun parseHttps(value: String): URI? = runCatching {
        URI(value.trim()).takeIf { it.scheme.equals("https", ignoreCase = true) }
    }.getOrNull()

    private fun queryParameter(uri: URI, name: String): String? =
        uri.rawQuery.orEmpty()
            .split("&")
            .mapNotNull { pair ->
                val parts = pair.split("=", limit = 2)
                if (parts.firstOrNull() == name) {
                    parts.getOrNull(1)?.let {
                        URLDecoder.decode(it, "UTF-8")
                    }
                } else {
                    null
                }
            }
            .firstOrNull()

    private fun normalizeHost(host: String): String =
        host.lowercase().removePrefix("www.")

    private fun isYouTubeHost(host: String): Boolean =
        host == "youtube.com" ||
            host.endsWith(".youtube.com") ||
            host == "youtube-nocookie.com" ||
            host.endsWith(".youtube-nocookie.com")

    private val VIDEO_ID_PATTERN = Regex("^[A-Za-z0-9_-]{11}$")
    private val YOUTUBE_PATH_PREFIXES = setOf("shorts", "embed", "live", "v")
    private val FACEBOOK_HOSTS = setOf("facebook.com", "www.facebook.com", "m.facebook.com")
    private val FACEBOOK_REEL = Regex("^/reel/[^/]+/?$", RegexOption.IGNORE_CASE)
    private val FACEBOOK_VIDEO = Regex("^/[^/]+/videos/[^/]+/?$", RegexOption.IGNORE_CASE)
    private val FACEBOOK_WATCH = Regex("^/watch/?$", RegexOption.IGNORE_CASE)
    private val FACEBOOK_VIDEO_PHP = Regex("^/video\\.php$", RegexOption.IGNORE_CASE)
}
