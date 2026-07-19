package com.viwa.android.di

import com.viwa.android.data.payment.aqsi.Arcus2Client
import com.viwa.android.data.payment.aqsi.AqsiRepositoryImpl
import com.viwa.android.data.repository.ConfigRepository
import com.viwa.android.services.payment.CardPaymentEventLogger
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM-контракт провайдеров [AqsiModule] (task-04 §3a): ожидаемые реализации без подъёма Android/Hilt.
 */
class AqsiModuleProvidesContractTest {

    @Test
    fun provideArcus2TerminalClient_returnsArcus2Implementation() {
        val client = AqsiModule.provideArcus2TerminalClient()
        assertTrue(client is Arcus2Client)
    }

    @Test
    fun provideAqsiRepository_returnsAqsiRepositoryImpl() {
        val holder = AqsiModule.provideAqsiLastOperationSnapshotHolder()
        val arcus = AqsiModule.provideArcus2TerminalClient()
        val repo =
            AqsiModule.provideAqsiRepository(
                configRepository = MinimalFakeConfigRepository(),
                arcus2TerminalClient = arcus,
                aqsiLastOperationSnapshotHolder = holder,
                paymentEventLogger = CardPaymentEventLogger(),
            )
        assertTrue(repo is AqsiRepositoryImpl)
    }

    private class MinimalFakeConfigRepository : ConfigRepository {
        override suspend fun get(key: String): String? = null

        override suspend fun set(key: String, value: String) {}

        override suspend fun delete(key: String) {}

        override suspend fun getJson(key: String): String? = null

        override suspend fun setJson(key: String, json: String) {}
    }
}
