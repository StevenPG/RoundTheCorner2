package com.stevenpg.roundthecorner2.core.testing

import com.stevenpg.roundthecorner2.core.data.preferences.UserPreferencesRepository
import com.stevenpg.roundthecorner2.core.model.CheckInterval
import com.stevenpg.roundthecorner2.core.model.ThemePreference
import com.stevenpg.roundthecorner2.core.model.UnitSystem
import com.stevenpg.roundthecorner2.core.model.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** In-memory [UserPreferencesRepository] for ViewModel tests. */
class FakeUserPreferencesRepository(
    initial: UserPreferences = UserPreferences(),
) : UserPreferencesRepository {

    private val state = MutableStateFlow(initial)

    override val preferences: StateFlow<UserPreferences> = state

    override suspend fun setUnitSystem(unitSystem: UnitSystem) =
        state.update { it.copy(unitSystem = unitSystem) }

    override suspend fun setThemePreference(themePreference: ThemePreference) =
        state.update { it.copy(themePreference = themePreference) }

    override suspend fun setUseDynamicColor(useDynamicColor: Boolean) =
        state.update { it.copy(useDynamicColor = useDynamicColor) }

    override suspend fun setCheckInterval(checkInterval: CheckInterval) =
        state.update { it.copy(checkInterval = checkInterval) }
}
