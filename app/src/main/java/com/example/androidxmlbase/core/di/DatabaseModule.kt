package com.example.androidxmlbase.core.di

import android.content.Context
import androidx.room.Room
import com.example.androidxmlbase.core.storage.database.AppDatabase
import com.example.androidxmlbase.core.storage.database.LocalSettingDao
import com.example.androidxmlbase.core.storage.secure.SecureStore
import com.example.androidxmlbase.core.storage.secure.SecureStoreKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.util.UUID
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private val DB_PASSPHRASE_KEY = SecureStoreKey("db_passphrase")

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        secureStore: SecureStore,
    ): AppDatabase {
        System.loadLibrary("sqlcipher")
        val passphrase =
            runBlocking {
                var key = secureStore.getString(DB_PASSPHRASE_KEY)
                if (key.isNullOrEmpty()) {
                    key = UUID.randomUUID().toString()
                    secureStore.putString(DB_PASSPHRASE_KEY, key)
                }
                key
            }

        val factory = SupportOpenHelperFactory(passphrase.toByteArray())

        return Room
            .databaseBuilder(
                context,
                AppDatabase::class.java,
                "app_database.db",
            ).openHelperFactory(factory)
            .fallbackToDestructiveMigration(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideLocalSettingDao(database: AppDatabase): LocalSettingDao = database.localSettingDao()
}
