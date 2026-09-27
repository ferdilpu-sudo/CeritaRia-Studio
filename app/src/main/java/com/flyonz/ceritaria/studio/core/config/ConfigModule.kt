package com.flyonz.ceritaria.studio.core.config

import com.flyonz.ceritaria.studio.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ConfigModule {
    @Provides
    @Singleton
    fun provideSupabaseConfig(): SupabaseConfig = SupabaseConfig(
        url = BuildConfig.SUPABASE_URL.trim(),
        publishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY.trim(),
    )
}
