package com.stevenpg.roundthecorner2.core.testing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.stevenpg.roundthecorner2.core.designsystem.theme.RoundTheCornerTheme

/**
 * Robolectric SDK used for screenshot tests. Pinned so baselines don't shift
 * when Robolectric adds a newer default.
 */
const val SCREENSHOT_SDK = 36

/**
 * Renders [content] inside the app theme and records `<name>_light.png` and
 * `<name>_dark.png` under the module's `src/test/screenshots/` folder.
 *
 * Usage (from a Robolectric test with `@GraphicsMode(NATIVE)`):
 * ```
 * composeRule.captureLightAndDark("settings") { SettingsScreen(...) }
 * ```
 * Record baselines: `./gradlew recordRoborazziDebug`
 * Verify against them: `./gradlew verifyRoborazziDebug`
 */
fun ComposeContentTestRule.captureLightAndDark(
    name: String,
    content: @Composable () -> Unit,
) {
    var darkTheme by mutableStateOf(false)
    setContent {
        RoundTheCornerTheme(darkTheme = darkTheme) { content() }
    }
    onRoot().captureRoboImage("src/test/screenshots/${name}_light.png")

    darkTheme = true
    waitForIdle()
    onRoot().captureRoboImage("src/test/screenshots/${name}_dark.png")
}
