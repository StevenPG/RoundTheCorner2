package com.stevenpg.roundthecorner2

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.stevenpg.roundthecorner2.core.testing.SCREENSHOT_SDK
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * End-to-end shell: launches the real MainActivity with the real Hilt graph
 * (Room, DataStore, repositories) on a fresh install, and screenshots the start
 * screen. Catches DI wiring mistakes that per-screen tests can't.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = RobolectricDeviceQualifiers.Pixel7, application = HiltTestApplication::class)
class AppShellScreenshotTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun capture(name: String) {
        // Room loads on a background thread: wait for the real (empty) list,
        // not the loading spinner, so the screenshot is deterministic.
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("No triggers yet").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test
    fun phone() = capture("app_shell_phone_light")

    @Test
    @Config(qualifiers = "+night")
    fun phoneDark() = capture("app_shell_phone_dark")

    /** Wide screens get a navigation rail instead of a bottom bar. */
    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet)
    fun tablet() = capture("app_shell_tablet_light")

    @Test
    @Config(qualifiers = TABLET_NIGHT)
    fun tabletDark() = capture("app_shell_tablet_dark")

    private companion object {
        // Night mode must precede the density qualifier in Android's ordering.
        const val TABLET_NIGHT = "w1280dp-h800dp-xlarge-notlong-notround-any-night-xhdpi-keyshidden-nonav"
    }
}
