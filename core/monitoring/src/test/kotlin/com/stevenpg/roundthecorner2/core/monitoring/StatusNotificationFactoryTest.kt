package com.stevenpg.roundthecorner2.core.monitoring

import android.app.Notification
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stevenpg.roundthecorner2.core.domain.monitoring.Tick
import com.stevenpg.roundthecorner2.core.domain.monitoring.TriggerProgress
import com.stevenpg.roundthecorner2.core.model.ResolvedUnitSystem
import com.stevenpg.roundthecorner2.core.model.miles
import com.stevenpg.roundthecorner2.core.monitoring.notification.NotificationChannels
import com.stevenpg.roundthecorner2.core.monitoring.notification.StatusNotificationFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
class StatusNotificationFactoryTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val factory = StatusNotificationFactory(context)

    private fun tick(progress: Float?, remaining: Double?, others: Int = 0) = Tick(
        progress = listOf(TriggerProgress(Fixtures.details, remaining?.let { (it + 2.0).miles }, remaining?.miles, progress)) +
            List(others) { TriggerProgress(Fixtures.details, null, null, null) },
        toFire = emptyList(),
        alreadySatisfied = emptyList(),
        waitingForAccuracy = false,
        nextInterval = 10.seconds,
    )

    private val Notification.title get() = extras.getCharSequence(Notification.EXTRA_TITLE).toString()
    private val Notification.text get() = extras.getCharSequence(Notification.EXTRA_TEXT).toString()

    @Test
    @Config(sdk = [36])
    fun `Android 16 gets a progress-style notification that requests promotion`() {
        val n = factory.build(tick(progress = 0.25f, remaining = 1.8, others = 2), ResolvedUnitSystem.IMPERIAL)
        assertEquals(NotificationChannels.STATUS, n.channelId)
        assertEquals("Arrive at Home", n.title)
        assertEquals("1.8 mi to go · 3.8 mi away", n.text)
        assertEquals("+2 more", n.extras.getCharSequence(Notification.EXTRA_SUB_TEXT).toString())
        // Robolectric emulates 36.0, where promotion is requested via the extra
        // (the builder API and status-bar chip arrive in 36.1, guarded at runtime).
        assertTrue(n.extras.getBoolean(Notification.EXTRA_REQUEST_PROMOTED_ONGOING))
        assertEquals(250, n.extras.getInt(Notification.EXTRA_PROGRESS))
        assertEquals(listOf("Send now", "Pause", "Stop all"), n.actions.map { it.title.toString() })
        assertTrue(n.flags and Notification.FLAG_ONGOING_EVENT != 0)
    }

    @Test
    @Config(sdk = [34])
    fun `older Android gets a standard progress notification`() {
        val n = factory.build(tick(progress = 0.5f, remaining = 1.0), ResolvedUnitSystem.METRIC)
        assertEquals("1.6 km to go · 4.8 km away", n.text)
        assertEquals(StatusNotificationFactory.PROGRESS_MAX, n.extras.getInt(Notification.EXTRA_PROGRESS_MAX))
        assertEquals(500, n.extras.getInt(Notification.EXTRA_PROGRESS))
        assertEquals(3, n.actions.size)
    }

    @Test
    @Config(sdk = [36])
    fun `before an accurate fix it says so and the bar is indeterminate`() {
        val n = factory.build(tick(progress = null, remaining = null), ResolvedUnitSystem.IMPERIAL)
        assertEquals("Waiting for an accurate GPS fix…", n.text)
        assertTrue(n.extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE))
    }
}
