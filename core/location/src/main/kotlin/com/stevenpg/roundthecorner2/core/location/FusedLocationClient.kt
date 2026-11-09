package com.stevenpg.roundthecorner2.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.stevenpg.roundthecorner2.core.domain.proximity.LocationFix
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import kotlin.time.Duration

/**
 * Google Play services' Fused Location Provider: blends GPS, Wi-Fi and cell
 * signals for better accuracy per milliamp than raw GPS.
 */
internal class FusedLocationClient @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocationClient {

    @SuppressLint("MissingPermission") // Documented precondition; SecurityException surfaces through the flow.
    override fun updates(interval: Duration): Flow<LocationFix> = callbackFlow {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval.inWholeMilliseconds)
            // Accept fixes up to twice as often if another app already has GPS running (free for us).
            .setMinUpdateIntervalMillis(interval.inWholeMilliseconds / 2)
            .setWaitForAccurateLocation(false)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.mapNotNull { it.toFix() }.forEach { trySend(it) }
            }
        }
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
            .addOnFailureListener { close(it) }
        awaitClose { client.removeLocationUpdates(callback) }
    }
}
