package com.stevenpg.roundthecorner2.core.model

/**
 * Everything the user can configure on the Settings screen.
 *
 * Immutable snapshot; repositories expose it as a `Flow<UserPreferences>` so
 * the UI recomposes whenever a value changes.
 */
data class UserPreferences(
    val unitSystem: UnitSystem = UnitSystem.SYSTEM,
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    /** Use Material You wallpaper-derived colors (Android 12+). */
    val useDynamicColor: Boolean = false,
)
