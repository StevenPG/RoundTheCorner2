package com.stevenpg.roundthecorner2.feature.triggers

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

/** Screenshot baselines for the Triggers tab (empty state). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class TriggersScreenScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyState() {
        composeRule.captureLightAndDark("triggers_empty") {
            TriggersScreen(onAddTrigger = {})
        }
    }
}
