package com.wiva.android.di

import com.wiva.android.data.repository.MachineInventoryRepositoryImpl
import com.wiva.android.domain.repository.MachineInventoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InventoryModule {
    @Binds
    @Singleton
    abstract fun bindMachineInventoryRepository(impl: MachineInventoryRepositoryImpl): MachineInventoryRepository
}
