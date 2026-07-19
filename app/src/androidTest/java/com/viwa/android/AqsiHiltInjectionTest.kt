package com.viwa.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.viwa.android.data.payment.aqsi.AqsiLastOperationSnapshotHolder
import com.viwa.android.domain.repository.AqsiRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke-резолв Hilt-графа для aQsi (task-04): бинды [AqsiModule] собираются, [AqsiRepository] доступен;
 * [AqsiLastOperationSnapshotHolder] — один экземпляр на процесс (Singleton).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AqsiHiltInjectionTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var aqsiRepository: AqsiRepository

    @Inject
    lateinit var snapshotHolderOne: AqsiLastOperationSnapshotHolder

    @Inject
    lateinit var snapshotHolderTwo: AqsiLastOperationSnapshotHolder

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun aqsiRepositoryResolvesFromHiltGraph() {
        assertNotNull(aqsiRepository)
    }

    @Test
    fun aqsiLastOperationSnapshotHolderIsSingletonInGraph() {
        assertSame(snapshotHolderOne, snapshotHolderTwo)
    }
}
