package com.flyonz.ceritaria.studio.core.auth

import com.flyonz.ceritaria.studio.core.network.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseAccessTokenProvider @Inject constructor(
    private val clientProvider: SupabaseClientProvider,
) : AccessTokenProvider {
    override suspend fun accessToken(): String? =
        clientProvider.clientOrNull
            ?.auth
            ?.currentSessionOrNull()
            ?.accessToken
}
