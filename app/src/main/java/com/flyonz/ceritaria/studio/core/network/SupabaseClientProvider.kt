package com.flyonz.ceritaria.studio.core.network

import com.flyonz.ceritaria.studio.core.config.SupabaseConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseClientProvider @Inject constructor(
    private val config: SupabaseConfig,
) {
    val clientOrNull: SupabaseClient? by lazy {
        if (!config.isConfigured) {
            null
        } else {
            createSupabaseClient(
                supabaseUrl = config.url,
                supabaseKey = config.publishableKey,
            ) {
                install(Auth)
                install(Postgrest)
                install(Storage)
            }
        }
    }
}
