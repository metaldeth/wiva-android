package com.viwa.android.data.payment.aqsi.di

import com.viwa.android.data.payment.aqsi.setup.AqsiPaymentStartupInitializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AqsiSetupEntryPoint {
    fun aqsiPaymentStartupInitializer(): AqsiPaymentStartupInitializer
}
