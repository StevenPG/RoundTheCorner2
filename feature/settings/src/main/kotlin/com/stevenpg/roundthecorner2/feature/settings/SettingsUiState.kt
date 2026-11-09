package com.stevenpg.roundthecorner2.feature.settings

import com.stevenpg.roundthecorner2.core.model.ResolvedUnitSystem
import com.stevenpg.roundthecorner2.core.model.UserPreferences

/** Everything the Settings screen renders. Immutable; produced by [SettingsViewModel]. */
sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Ready(
        val preferences: UserPreferences,
        /** What "Automatic" units currently resolve to, shown as a hint. */
        val localeUnitSystem: ResolvedUnitSystem,
        /** Hide the wallpaper-color toggle on devices older than Android 12. */
        val supportsDynamicColor: Boolean,
        val appVersion: String,
    ) : SettingsUiState
}
