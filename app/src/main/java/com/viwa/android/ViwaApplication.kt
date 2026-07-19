package com.viwa.android

import android.app.Application
import com.viwa.android.hardware.FlowStripRgbCoordinator
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import timber.log.Timber

@HiltAndroidApp
class ViwaApplication : Application() {
 /** Ранняя инициализация: [FlowStripRgbCoordinator] регистрирует колбэк в `init`. */
    @Suppress("unused")
    @Inject
    lateinit var flowStripRgbCoordinator: FlowStripRgbCoordinator

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}
