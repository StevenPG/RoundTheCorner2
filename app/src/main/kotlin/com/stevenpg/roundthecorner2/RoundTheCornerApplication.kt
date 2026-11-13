package com.stevenpg.roundthecorner2

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.stevenpg.roundthecorner2.core.monitoring.MonitoringController
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point. `@HiltAndroidApp` generates the root dependency
 * graph (the singleton component) - comparable to bootstrapping a Spring
 * ApplicationContext.
 *
 * Also provides WorkManager's configuration so workers (send retries) get
 * their dependencies from Hilt; the default auto-initializer is removed in
 * the manifest for this to take effect.
 */
@HiltAndroidApp
class RoundTheCornerApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var monitoringController: MonitoringController

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // Start location monitoring whenever any trigger is armed.
        monitoringController.observe()
    }
}
