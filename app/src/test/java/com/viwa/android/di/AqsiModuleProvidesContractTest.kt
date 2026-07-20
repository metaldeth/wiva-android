package com.viwa.android.di

import com.viwa.android.data.payment.aqsi.AqsiLastOperationSnapshotHolder
import org.junit.Assert.assertNotNull
import org.junit.Test

class AqsiModuleProvidesContractTest {

    @Test
    fun provideAqsiLastOperationSnapshotHolder_returnsHolder() {
        assertNotNull(AqsiModule.provideAqsiLastOperationSnapshotHolder())
    }
}
