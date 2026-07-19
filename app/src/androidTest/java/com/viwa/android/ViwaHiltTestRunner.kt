package com.viwa.android

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Instrumentation runner required by Hilt: substitutes [HiltTestApplication] for `@HiltAndroidTest`.
 * See https://dagger.dev/hilt/instrumentation-testing.html
 */
class ViwaHiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader, name: String, context: Context): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}
