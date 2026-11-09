package com.stevenpg.roundthecorner2.core.location

import android.Manifest
import android.app.Application
import android.location.Location
import android.location.LocationManager
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.async
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlatformLocationClientTest {

    private val app = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `platform fixes are mapped to the domain`() = runBlocking {
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        val manager = app.getSystemService(LocationManager::class.java)
        shadowOf(manager).setProviderEnabled(LocationManager.FUSED_PROVIDER, true)

        val fix = async { withTimeout(5_000) { PlatformLocationClient(app).updates(10.seconds).first() } }
        repeat(5) { yield(); ShadowLooper.idleMainLooper() }

        val location = Location(LocationManager.FUSED_PROVIDER).apply {
            latitude = 39.7392
            longitude = -104.9903
            accuracy = 8f
            speed = 12f
            time = 1_700_000_000_000
        }
        shadowOf(manager).simulateLocation(location)
        repeat(5) { yield(); ShadowLooper.idleMainLooper() }

        val result = fix.await()
        assertEquals(39.7392, result.coordinates.latitude, 1e-9)
        assertEquals(8.0, result.accuracyMeters!!, 1e-6)
        assertEquals(12.0, result.speedMetersPerSecond!!, 1e-6)
        assertEquals(1_700_000_000_000, result.timestamp.toEpochMilli())
    }

    @Test
    fun `missing accuracy and bogus coordinates are handled`() {
        val noAccuracy = Location("test").apply { latitude = 1.0; longitude = 2.0 }
        assertNull(noAccuracy.toFix()!!.accuracyMeters)
        val bogus = Location("test").apply { latitude = 95.0; longitude = 0.0 }
        assertNull(bogus.toFix())
    }
}
