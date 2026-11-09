package com.stevenpg.roundthecorner2

import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.stevenpg.roundthecorner2.core.testing.SCREENSHOT_SDK
import com.stevenpg.roundthecorner2.core.testing.captureLightAndDark
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import com.stevenpg.roundthecorner2.ui.RoundTheCornerApp

/** Whole-app chrome: navigation bar + start tab. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class AppShellScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun phone() {
        composeRule.captureLightAndDark("app_shell_phone") { RoundTheCornerApp() }
    }

    /** Wide screens get a navigation rail instead of a bottom bar. */
    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet)
    fun tablet() {
        composeRule.captureLightAndDark("app_shell_tablet") { RoundTheCornerApp() }
    }
}
