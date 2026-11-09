package com.stevenpg.roundthecorner2.core.data.preferences

import com.stevenpg.roundthecorner2.core.data.locale.LocaleUnitSystemProvider
import com.stevenpg.roundthecorner2.core.model.ResolvedUnitSystem
import com.stevenpg.roundthecorner2.core.model.resolve
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * The unit system screens should display right now: the user's choice, with
 * "Automatic" resolved against the device locale.
 */
class UnitSystemProvider @Inject constructor(
    private val preferences: UserPreferencesRepository,
    private val locale: LocaleUnitSystemProvider,
) {
    val unitSystem: Flow<ResolvedUnitSystem> = preferences.preferences
        .map { it.unitSystem.resolve(locale.localeDefault()) }
        .distinctUntilChanged()
}
