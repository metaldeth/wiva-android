package com.viwa.android.data.payment.aqsi.setup

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AqsiSetupEntryPoint {
    fun aqsiPaymentStartupInitializer(): AqsiPaymentStartupInitializer
}
