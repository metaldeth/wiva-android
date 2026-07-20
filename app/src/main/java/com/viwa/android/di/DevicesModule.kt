package com.viwa.android.di

import com.viwa.android.hardware.devices.ViwaControllerPortScanPort
import com.viwa.android.hardware.devices.ViwaControllerPortScanner
import com.viwa.android.hardware.serial.PaymentSerialPort
import com.viwa.android.hardware.serial.ViwaPaymentSerialPort
import com.viwa.android.hardware.serial.ViwaSerialPort
import com.viwa.android.hardware.serial.ViwaSerialPortImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DevicesModule {
    @Binds
    @Singleton
    abstract fun bindViwaSerialPort(impl: ViwaSerialPortImpl): ViwaSerialPort

    @Binds
    @Singleton
    abstract fun bindPaymentSerialPort(impl: ViwaPaymentSerialPort): PaymentSerialPort

    @Binds
    @Singleton
    abstract fun bindViwaControllerPortScanPort(impl: ViwaControllerPortScanner): ViwaControllerPortScanPort
}
