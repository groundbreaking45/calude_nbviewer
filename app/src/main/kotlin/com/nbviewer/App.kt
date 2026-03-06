package com.nbviewer

import android.app.Application
import com.nbviewer.di.AppContainer

/**
 * Application entry point.
 *
 * Owns the [AppContainer] singleton for the lifetime of the process.
 * Components retrieve dependencies via `(requireActivity().application as App).container`.
 *
 * No logic lives here — App is a pure DI root.
 * When Hilt is adopted (ADR-001 upgrade trigger), this class gains @HiltAndroidApp
 * and AppContainer is replaced by @Module classes. No other files change.
 */
class App : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
