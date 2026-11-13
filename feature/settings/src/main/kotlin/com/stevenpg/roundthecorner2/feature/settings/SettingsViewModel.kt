package com.stevenpg.roundthecorner2.feature.settings

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stevenpg.roundthecorner2.core.data.locale.LocaleUnitSystemProvider
import com.stevenpg.roundthecorner2.core.data.preferences.UserPreferencesRepository
import com.stevenpg.roundthecorner2.core.model.CheckInterval
import com.stevenpg.roundthecorner2.core.model.ThemePreference
import com.stevenpg.roundthecorner2.core.model.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Turns the preferences stream into [SettingsUiState] and forwards user edits
 * to the repository. Contains no Android UI code, so it is unit-testable.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val localeUnitSystemProvider: LocaleUnitSystemProvider,
    @ApplicationContext context: Context,
) : ViewModel() {

    private val appVersion: String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: "-"

    val uiState: StateFlow<SettingsUiState> = preferencesRepository.preferences
        .map { prefs ->
            SettingsUiState.Ready(
                preferences = prefs,
                localeUnitSystem = localeUnitSystemProvider.localeDefault(),
                supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                appVersion = appVersion,
            )
        }
        // Keep the upstream alive briefly across config changes (rotation)
        // instead of restarting the DataStore read each time.
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), SettingsUiState.Loading)

    fun onUnitSystemSelected(unitSystem: UnitSystem) {
        viewModelScope.launch { preferencesRepository.setUnitSystem(unitSystem) }
    }

    fun onThemeSelected(theme: ThemePreference) {
        viewModelScope.launch { preferencesRepository.setThemePreference(theme) }
    }

    fun onCheckIntervalSelected(interval: CheckInterval) {
        viewModelScope.launch { preferencesRepository.setCheckInterval(interval) }
    }

    fun onDynamicColorToggled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setUseDynamicColor(enabled) }
    }
}
