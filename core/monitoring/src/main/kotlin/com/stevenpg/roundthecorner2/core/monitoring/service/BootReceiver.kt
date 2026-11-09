package com.stevenpg.roundthecorner2.core.monitoring.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.stevenpg.roundthecorner2.core.data.repository.TriggerRepository
import com.stevenpg.roundthecorner2.core.monitoring.MonitoringController
import com.stevenpg.roundthecorner2.core.monitoring.MonitoringPermissions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Resumes monitoring after a reboot or an app update, if anything is armed.
 *
 * Starting a *location* foreground service from the background requires
 * "Allow all the time" location access; without it Android refuses, and
 * monitoring resumes the next time the app is opened instead.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var triggerRepository: TriggerRepository
    @Inject lateinit var permissions: MonitoringPermissions
    @Inject lateinit var controller: MonitoringController

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        if (!permissions.hasBackgroundLocation()) return
        val pending = goAsync() // Keep the receiver alive while we query the database.
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                if (triggerRepository.observeArmedTriggers().first().isNotEmpty()) controller.tryStart()
            } finally {
                pending.finish()
            }
        }
    }
}
