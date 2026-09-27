package com.flyonz.ceritaria.studio.core.database.videojob

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VideoJobModule {
    @Binds
    @Singleton
    abstract fun bindVideoJobRepository(
        implementation: RoomVideoJobRepository,
    ): VideoJobRepository
}
