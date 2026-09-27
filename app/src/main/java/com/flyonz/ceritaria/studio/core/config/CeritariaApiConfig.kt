package com.flyonz.ceritaria.studio.core.config

data class CeritariaApiConfig(
    val baseUrl: String,
) {
    val isConfigured: Boolean
        get() = baseUrl.startsWith("https://")
}
