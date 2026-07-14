package com.example.androidxmlbase.core.storage.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Reusable abstract database containing tables and migration configurations.
 */
@Database(
    entities = [LocalSettingEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun localSettingDao(): LocalSettingDao
}
