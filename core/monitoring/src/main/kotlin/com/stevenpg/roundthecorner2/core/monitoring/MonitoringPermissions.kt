package com.stevenpg.roundthecorner2.core.monitoring

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Everything monitoring depends on, checked live (the user can change these any time in Settings). */
data class PermissionSnapshot(
    /** Precise location: required to run at all. */
    val preciseLocation: Boolean,
    /** "Allow all the time": restart after reboot and survive being started in the background. */
    val backgroundLocation: Boolean,
    /** The live distance notification and alerts. */
    val notifications: Boolean,
    /** Not subject to battery optimisation (Doze can otherwise delay location updates). */
    val unrestrictedBattery: Boolean,
)

/** Read-only view of [PermissionSnapshot]; an interface so screens can be tested without Android. */
interface PermissionChecker {
    fun snapshot(): PermissionSnapshot
}

class MonitoringPermissions @Inject constructor(
    @ApplicationContext private val context: Context,
) : PermissionChecker {
    override fun snapshot() = PermissionSnapshot(
        preciseLocation = hasPreciseLocation(),
        backgroundLocation = granted(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
        notifications = NotificationManagerCompat.from(context).areNotificationsEnabled(),
        unrestrictedBattery = context.getSystemService(PowerManager::class.java)
            ?.isIgnoringBatteryOptimizations(context.packageName) == true,
    )

    fun hasPreciseLocation() = granted(Manifest.permission.ACCESS_FINE_LOCATION)

    fun hasBackgroundLocation() = granted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    companion object {
        /** Runtime permission for notifications exists only on Android 13+. */
        val NOTIFICATION_PERMISSION: String? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null
    }
}
