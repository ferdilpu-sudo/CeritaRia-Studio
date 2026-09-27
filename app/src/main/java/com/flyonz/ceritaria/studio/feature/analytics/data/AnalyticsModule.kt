package com.flyonz.ceritaria.studio.feature.analytics.data

import com.flyonz.ceritaria.studio.feature.analytics.domain.AnalyticsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {
    @Binds
    @Singleton
    abstract fun bindAnalyticsDataSource(
        implementation: SupabaseAnalyticsDataSource,
    ): AnalyticsDataSource

    @Binds
    @Singleton
    abstract fun bindAnalyticsRepository(
        implementation: AnalyticsRepositoryImpl,
    ): AnalyticsRepository
}
