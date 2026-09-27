package com.flyonz.ceritaria.studio.core.upload

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UploadModule {
    @Binds
    @Singleton
    abstract fun bindVideoUploadApi(
        implementation: KtorVideoUploadApi,
    ): VideoUploadApi

    @Binds
    @Singleton
    abstract fun bindR2UploadDataSource(
        implementation: KtorR2UploadDataSource,
    ): R2UploadDataSource

    @Binds
    @Singleton
    abstract fun bindVideoUploadSourceReader(
        implementation: AndroidVideoUploadSourceReader,
    ): VideoUploadSourceReader
}
