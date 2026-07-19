package com.viwa.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.viwa.android.domain.repository.MaxRepository
import com.viwa.android.domain.repository.NanoKassaRepository
import com.viwa.android.domain.repository.SBPRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class IntegrationsHiltInjectionTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var maxRepository: MaxRepository

    @Inject
    lateinit var sbpRepository: SBPRepository

    @Inject
    lateinit var nanoKassaRepository: NanoKassaRepository

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun integrationRepositoriesAreInjectable() {
        assertNotNull(maxRepository)
        assertNotNull(sbpRepository)
        assertNotNull(nanoKassaRepository)
    }
}
