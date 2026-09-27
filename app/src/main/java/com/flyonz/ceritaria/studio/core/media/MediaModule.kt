package com.flyonz.ceritaria.studio.core.media

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaModule {
    @Binds
    @Singleton
    abstract fun bindVideoInspector(
        implementation: Media3VideoInspector,
    ): VideoInspector
}
