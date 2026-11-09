package com.stevenpg.roundthecorner2.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.stevenpg.roundthecorner2.core.model.ThemePreference
import com.stevenpg.roundthecorner2.core.model.UnitSystem
import com.stevenpg.roundthecorner2.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

/**
 * [UserPreferencesRepository] backed by Jetpack DataStore (an async,
 * transactional replacement for SharedPreferences).
 *
 * Enums are persisted by **name**, and unknown/missing values fall back to the
 * defaults, so renaming or removing an enum constant can never crash on read.
 */
internal class DataStoreUserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    override val preferences: Flow<UserPreferences> = dataStore.data
        // A corrupt/unreadable file should degrade to defaults, not crash the app.
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toUserPreferences() }
        .distinctUntilChanged()

    override suspend fun setUnitSystem(unitSystem: UnitSystem) {
        dataStore.edit { it[Keys.UNIT_SYSTEM] = unitSystem.name }
    }

    override suspend fun setThemePreference(themePreference: ThemePreference) {
        dataStore.edit { it[Keys.THEME] = themePreference.name }
    }

    override suspend fun setUseDynamicColor(useDynamicColor: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC_COLOR] = useDynamicColor }
    }

    private fun Preferences.toUserPreferences(): UserPreferences {
        val defaults = UserPreferences()
        return UserPreferences(
            unitSystem = enumOrDefault(this[Keys.UNIT_SYSTEM], defaults.unitSystem),
            themePreference = enumOrDefault(this[Keys.THEME], defaults.themePreference),
            useDynamicColor = this[Keys.DYNAMIC_COLOR] ?: defaults.useDynamicColor,
        )
    }

    private inline fun <reified E : Enum<E>> enumOrDefault(name: String?, default: E): E =
        name?.let { runCatching { enumValueOf<E>(it) }.getOrNull() } ?: default

    /** Storage keys. Never rename these - that would silently reset user settings. */
    private object Keys {
        val UNIT_SYSTEM = stringPreferencesKey("unit_system")
        val THEME = stringPreferencesKey("theme")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    }
}
