package com.stevenpg.roundthecorner2.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** True when the OS supports Material You wallpaper colors (Android 12+). */
val supportsDynamicColor: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * Root theme for the whole app. Every screen and every preview/screenshot test
 * must be wrapped in this so colors, type and shapes stay consistent.
 *
 * @param darkTheme force light/dark; defaults to the system setting.
 * @param dynamicColor use wallpaper-derived colors when the OS supports it.
 *   Off by default so the brand palette is what people see first.
 */
@Composable
fun RoundTheCornerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && supportsDynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RtcTypography,
        shapes = RtcShapes,
        content = content,
    )
}
