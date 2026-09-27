package com.flyonz.ceritaria.studio.feature.episode.data

import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeRepository
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeVideoAssetRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EpisodeModule {
    @Binds
    @Singleton
    abstract fun bindEpisodeDataSource(
        implementation: SupabaseEpisodeDataSource,
    ): EpisodeDataSource

    @Binds
    @Singleton
    abstract fun bindEpisodeRepository(implementation: EpisodeRepositoryImpl): EpisodeRepository

    @Binds
    @Singleton
    abstract fun bindEpisodeVideoAssetDataSource(
        implementation: KtorEpisodeVideoAssetDataSource,
    ): EpisodeVideoAssetDataSource

    @Binds
    @Singleton
    abstract fun bindEpisodeVideoAssetRepository(
        implementation: EpisodeVideoAssetRepositoryImpl,
    ): EpisodeVideoAssetRepository
}
