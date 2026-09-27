package com.flyonz.ceritaria.studio.core.upload.execution

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VideoUploadExecutionModule {
    @Binds
    @Singleton
    abstract fun bindVideoUploadScheduler(
        implementation: AndroidVideoUploadScheduler,
    ): VideoUploadScheduler
}
