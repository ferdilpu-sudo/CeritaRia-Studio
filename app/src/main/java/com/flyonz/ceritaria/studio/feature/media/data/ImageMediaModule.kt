package com.flyonz.ceritaria.studio.feature.media.data

import com.flyonz.ceritaria.studio.feature.media.domain.ImageMediaRepository
import com.flyonz.ceritaria.studio.feature.media.source.AndroidImageSourceReader
import com.flyonz.ceritaria.studio.feature.media.source.ImageSourceReader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ImageMediaModule {
    @Binds
    @Singleton
    abstract fun bindImageMediaRepository(
        implementation: ImageMediaRepositoryImpl,
    ): ImageMediaRepository

    @Binds
    @Singleton
    abstract fun bindImageStorageDataSource(
        implementation: SupabaseImageStorageDataSource,
    ): ImageStorageDataSource

    @Binds
    @Singleton
    abstract fun bindImageReferenceDataSource(
        implementation: SupabaseImageReferenceDataSource,
    ): ImageReferenceDataSource

    @Binds
    @Singleton
    abstract fun bindImageSourceReader(
        implementation: AndroidImageSourceReader,
    ): ImageSourceReader
}
