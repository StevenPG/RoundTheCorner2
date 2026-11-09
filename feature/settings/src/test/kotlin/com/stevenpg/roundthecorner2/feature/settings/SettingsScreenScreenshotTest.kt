package com.stevenpg.roundthecorner2.feature.settings

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
import com.stevenpg.roundthecorner2.core.model.ResolvedUnitSystem
import com.stevenpg.roundthecorner2.core.model.ThemePreference
import com.stevenpg.roundthecorner2.core.model.UnitSystem
import com.stevenpg.roundthecorner2.core.model.UserPreferences

/** Screenshot baselines for the Settings screen. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class SettingsScreenScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun automaticUnits() = capture(
        name = "settings_automatic",
        preferences = UserPreferences(),
    )

    @Test
    fun metricUnitsDarkThemeDynamicColor() = capture(
        name = "settings_metric",
        preferences = UserPreferences(
            unitSystem = UnitSystem.METRIC,
            themePreference = ThemePreference.DARK,
            useDynamicColor = true,
        ),
    )

    private fun capture(name: String, preferences: UserPreferences) {
        composeRule.captureLightAndDark(name) {
            SettingsScreen(
                uiState = SettingsUiState.Ready(
                    preferences = preferences,
                    localeUnitSystem = ResolvedUnitSystem.IMPERIAL,
                    supportsDynamicColor = true,
                    appVersion = "0.1.0",
                ),
                onUnitSystemSelected = {},
                onThemeSelected = {},
                onDynamicColorToggled = {},
            )
        }
    }
}
