package com.flyonz.ceritaria.studio.core.auth

interface AccessTokenProvider {
    suspend fun accessToken(): String?
}
