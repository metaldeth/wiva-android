package com.viwa.android.di

import com.viwa.android.services.preparing.PreparingStateCallback
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PreparingModule {
    @Provides
    @Singleton
    fun providePreparingStateCallback(): PreparingStateCallback = PreparingStateCallback { }
}
