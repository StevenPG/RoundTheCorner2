package com.stevenpg.roundthecorner2.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import com.stevenpg.roundthecorner2.core.domain.polling.LocationAccuracy
import com.stevenpg.roundthecorner2.core.domain.proximity.LocationFix
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import kotlin.time.Duration

/**
 * Fallback for devices without Google Play services (e.g. de-Googled ROMs):
 * Android's own LocationManager. Uses the platform "fused" provider where
 * available (API 31+), otherwise GPS.
 */
internal class PlatformLocationClient @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocationClient {

    @SuppressLint("MissingPermission")
    override fun updates(interval: Duration, accuracy: LocationAccuracy): Flow<LocationFix> = callbackFlow {
        val manager = context.getSystemService(LocationManager::class.java)
        val provider = if (accuracy == LocationAccuracy.LOW_POWER && manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            // Wi-Fi and cell positioning, no GPS. If it's missing or switched off, fall through to precise.
            LocationManager.NETWORK_PROVIDER
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            manager.hasProvider(LocationManager.FUSED_PROVIDER)
        ) {
            LocationManager.FUSED_PROVIDER
        } else {
            LocationManager.GPS_PROVIDER
        }
        val listener = LocationListener { location -> location.toFix()?.let { trySend(it) } }
        manager.requestLocationUpdates(provider, interval.inWholeMilliseconds, 0f, listener, Looper.getMainLooper())
        awaitClose { manager.removeUpdates(listener) }
    }
}
