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
    /** Slowest location check rate (used when far away). */
    val checkInterval: CheckInterval = CheckInterval.ONE_MINUTE,
)

/**
 * The longest gap between location checks. Monitoring automatically checks
 * more often as you approach a boundary (down to every 10 s); this caps how
 * lazy it may be while you're far away - the main battery trade-off.
 * Persisted by name, so only ever append new entries.
 */
enum class CheckInterval(val seconds: Int) {
    THIRTY_SECONDS(30),
    ONE_MINUTE(60),
    TWO_MINUTES(120),
    FIVE_MINUTES(300),
}
