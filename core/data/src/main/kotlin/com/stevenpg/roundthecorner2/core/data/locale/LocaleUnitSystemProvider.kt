package com.stevenpg.roundthecorner2.core.data.locale

import android.icu.util.LocaleData
import android.icu.util.ULocale
import com.stevenpg.roundthecorner2.core.model.ResolvedUnitSystem
import javax.inject.Inject

/**
 * Determines which unit system the device's locale prefers. This is what the
 * "System default" unit setting resolves to.
 */
fun interface LocaleUnitSystemProvider {
    fun localeDefault(): ResolvedUnitSystem
}

/**
 * Uses ICU's CLDR measurement-system data (API 28+; our minSdk is 29).
 *
 * CLDR reports `US` (United States, Liberia, Myanmar), `UK` (miles on the road,
 * metric elsewhere) or `SI`. For a *road distance* app both US and UK mean
 * miles, so both map to imperial.
 *
 * The locale is read on every call (not cached) so a language/region change in
 * system settings is picked up without restarting the app.
 */
internal class IcuLocaleUnitSystemProvider @Inject constructor() : LocaleUnitSystemProvider {
    override fun localeDefault(): ResolvedUnitSystem {
        val system = LocaleData.getMeasurementSystem(ULocale.getDefault(ULocale.Category.FORMAT))
        return if (system == LocaleData.MeasurementSystem.SI) {
            ResolvedUnitSystem.METRIC
        } else {
            ResolvedUnitSystem.IMPERIAL
        }
    }
}
