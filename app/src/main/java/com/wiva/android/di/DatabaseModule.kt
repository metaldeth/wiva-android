package com.wiva.android.di

import android.content.Context
import androidx.room.Room
import com.wiva.android.data.local.db.WivaDatabase
import com.wiva.android.data.local.db.JsonStoreDao
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
    fun provideDatabase(@ApplicationContext context: Context): WivaDatabase =
        Room
            .databaseBuilder(context, WivaDatabase::class.java, "wiva.db")
            .build()

    @Provides
    fun provideJsonStoreDao(db: WivaDatabase): JsonStoreDao = db.jsonStoreDao()
}
