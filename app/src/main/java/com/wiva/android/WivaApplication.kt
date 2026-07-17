package com.wiva.android

import android.app.Application
import com.wiva.android.hardware.FlowStripRgbCoordinator
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import timber.log.Timber

@HiltAndroidApp
class WivaApplication : Application() {
 /** Ранняя инициализация: [FlowStripRgbCoordinator] регистрирует колбэк в `init`. */
    @Suppress("unused")
    @Inject
    lateinit var flowStripRgbCoordinator: FlowStripRgbCoordinator

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}
