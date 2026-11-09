package com.stevenpg.roundthecorner2.feature.triggers.list

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Returns a function that runs the right system flow for each requirement.
 *
 * Android only shows a permission dialog a limited number of times; after the
 * user denies twice, requests return "denied" instantly. So after a denial the
 * next tap opens the app's system Settings page instead.
 */
@Composable
internal fun rememberRequirementFixer(onResult: () -> Unit): (MonitoringRequirement) -> Unit {
    val context = LocalContext.current
    var locationDenied by rememberSaveable { mutableStateOf(false) }
    var notificationsDenied by rememberSaveable { mutableStateOf(false) }

    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        locationDenied = result[Manifest.permission.ACCESS_FINE_LOCATION] != true
        onResult()
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsDenied = !granted
        onResult()
    }
    // Android 11+ shows the system "Location" page with "Allow all the time".
    val backgroundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onResult() }

    return { requirement ->
        when (requirement) {
            MonitoringRequirement.PRECISE_LOCATION ->
                if (locationDenied) {
                    context.openAppSettings()
                } else {
                    locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }
            MonitoringRequirement.NOTIFICATIONS ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsDenied) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    context.safeStart(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                    )
                }
            MonitoringRequirement.BACKGROUND_LOCATION ->
                backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            // The list screen needs no special permission (the direct "ignore
            // optimisations" request is restricted by Play policy).
            MonitoringRequirement.BATTERY ->
                context.safeStart(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }
}

private fun Context.openAppSettings() =
    safeStart(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))

private fun Context.safeStart(intent: Intent) {
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        openAppSettingsFallback()
    }
}

private fun Context.openAppSettingsFallback() {
    runCatching { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))) }
}
