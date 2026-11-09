package com.stevenpg.roundthecorner2.core.data.preferences

import com.stevenpg.roundthecorner2.core.model.ThemePreference
import com.stevenpg.roundthecorner2.core.model.UnitSystem
import com.stevenpg.roundthecorner2.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow

/**
 * Source of truth for user settings.
 *
 * Exposed as an interface so ViewModels can be unit-tested with a fake and so
 * the storage technology (DataStore today) can change without touching callers.
 */
interface UserPreferencesRepository {
    /** Emits the current preferences immediately, then on every change. */
    val preferences: Flow<UserPreferences>

    suspend fun setUnitSystem(unitSystem: UnitSystem)
    suspend fun setThemePreference(themePreference: ThemePreference)
    suspend fun setUseDynamicColor(useDynamicColor: Boolean)
}
