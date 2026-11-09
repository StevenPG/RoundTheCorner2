package com.stevenpg.roundthecorner2.feature.places

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

/** Screenshot baselines for the Places tab (empty state). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class PlacesScreenScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyState() {
        composeRule.captureLightAndDark("places_empty") {
            PlacesScreen(onAddPlace = {})
        }
    }
}
