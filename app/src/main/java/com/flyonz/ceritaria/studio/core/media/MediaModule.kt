package com.flyonz.ceritaria.studio.core.media

import androidx.media3.common.util.UnstableApi
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@UnstableApi
@Module
@InstallIn(SingletonComponent::class)
abstract class MediaModule {
    @Binds
    @Singleton
    abstract fun bindVideoInspector(
        implementation: Media3VideoInspector,
    ): VideoInspector

    @Binds
    @Singleton
    abstract fun bindVideoSourceAccess(
        implementation: AndroidVideoSourceAccess,
    ): VideoSourceAccess
}
