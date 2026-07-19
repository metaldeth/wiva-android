package com.viwa.android.di

import android.content.Context
import androidx.room.Room
import com.viwa.android.data.local.db.ViwaDatabase
import com.viwa.android.data.local.db.JsonStoreDao
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
    fun provideDatabase(@ApplicationContext context: Context): ViwaDatabase =
        Room
            .databaseBuilder(context, ViwaDatabase::class.java, "wiva.db")
            .build()

    @Provides
    fun provideJsonStoreDao(db: ViwaDatabase): JsonStoreDao = db.jsonStoreDao()
}
