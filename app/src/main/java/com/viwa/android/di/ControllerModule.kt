package com.viwa.android.di

import com.viwa.android.hardware.controller.ControllerGateway
import com.viwa.android.hardware.controller.DelegatingControllerGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ControllerModule {
    @Binds
    @Singleton
    abstract fun bindControllerGateway(impl: DelegatingControllerGateway): ControllerGateway
}
