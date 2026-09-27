package com.flyonz.ceritaria.studio.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.flyonz.ceritaria.studio.core.database.videojob.VideoJobDao
import com.flyonz.ceritaria.studio.core.database.videojob.VideoJobEntity

@Database(
    entities = [VideoJobEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class StudioDatabase : RoomDatabase() {
    abstract fun videoJobDao(): VideoJobDao
}
