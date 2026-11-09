package com.stevenpg.roundthecorner2

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stevenpg.roundthecorner2.core.designsystem.theme.RoundTheCornerTheme
import com.stevenpg.roundthecorner2.core.model.ThemePreference
import com.stevenpg.roundthecorner2.ui.RoundTheCornerApp
import dagger.hilt.android.AndroidEntryPoint

/**
 * The single Activity. All screens are Compose destinations hosted inside
 * [RoundTheCornerApp]; this class only handles window-level concerns
 * (splash screen, edge-to-edge drawing, theme).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate so the splash theme is swapped correctly.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Hold the splash until preferences load (typically a few ms).
        splashScreen.setKeepOnScreenCondition { viewModel.uiState.value is MainActivityUiState.Loading }

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val preferences = (uiState as? MainActivityUiState.Ready)?.preferences ?: return@setContent

            val darkTheme = shouldUseDarkTheme(preferences.themePreference)
            SyncSystemBarsWithTheme(darkTheme)

            RoundTheCornerTheme(darkTheme = darkTheme, dynamicColor = preferences.useDynamicColor) {
                RoundTheCornerApp()
            }
        }
    }

    /**
     * Draw behind the status/navigation bars and make their icons light or
     * dark to match the *app's* theme (which may differ from the system's when
     * the user forces Light/Dark in Settings).
     */
    @Composable
    private fun SyncSystemBarsWithTheme(darkTheme: Boolean) {
        DisposableEffect(darkTheme) {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                navigationBarStyle = SystemBarStyle.auto(LightScrim, DarkScrim) { darkTheme },
            )
            onDispose {}
        }
    }

    private companion object {
        // Same scrims androidx.activity uses by default for 3-button navigation.
        val LightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DarkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}

@Composable
private fun shouldUseDarkTheme(preference: ThemePreference): Boolean = when (preference) {
    ThemePreference.SYSTEM -> isSystemInDarkTheme()
    ThemePreference.LIGHT -> false
    ThemePreference.DARK -> true
}
