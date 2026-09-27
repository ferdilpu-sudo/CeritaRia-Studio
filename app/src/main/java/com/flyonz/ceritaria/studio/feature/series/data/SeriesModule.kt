package com.flyonz.ceritaria.studio.feature.series.data

import com.flyonz.ceritaria.studio.feature.series.domain.SeriesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SeriesModule {
    @Binds
    @Singleton
    abstract fun bindSeriesRepository(implementation: SeriesRepositoryImpl): SeriesRepository
}
