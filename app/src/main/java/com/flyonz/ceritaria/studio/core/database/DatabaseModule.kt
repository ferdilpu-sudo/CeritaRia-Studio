package com.flyonz.ceritaria.studio.core.database

import android.content.Context
import androidx.room.Room
import com.flyonz.ceritaria.studio.core.database.videojob.VideoJobDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): StudioDatabase = Room.databaseBuilder(
        context,
        StudioDatabase::class.java,
        DATABASE_NAME,
    ).build()

    @Provides
    fun provideVideoJobDao(database: StudioDatabase): VideoJobDao =
        database.videoJobDao()

    private const val DATABASE_NAME = "ceritaria-studio.db"
}
