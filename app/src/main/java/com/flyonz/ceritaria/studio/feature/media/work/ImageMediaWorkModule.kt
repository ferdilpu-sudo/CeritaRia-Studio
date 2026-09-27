package com.flyonz.ceritaria.studio.feature.media.work

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ImageMediaWorkModule {
    @Binds
    @Singleton
    abstract fun bindImageMediaScheduler(
        implementation: WorkManagerImageMediaScheduler,
    ): ImageMediaScheduler
}
