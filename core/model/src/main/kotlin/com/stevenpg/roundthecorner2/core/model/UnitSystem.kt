package com.stevenpg.roundthecorner2.core.model

/**
 * How distances are shown to the user. Internally every distance is stored and
 * compared in **meters**; this only affects display and input.
 */
enum class UnitSystem {
    /** Follow the device locale (e.g. en-US -> imperial, de-DE -> metric). */
    SYSTEM,

    /** Kilometers / meters. */
    METRIC,

    /** Miles / feet. */
    IMPERIAL,
}

/**
 * The concrete unit system after [UnitSystem.SYSTEM] has been resolved against
 * the device locale. UI code should only ever format with this.
 */
enum class ResolvedUnitSystem { METRIC, IMPERIAL }

/**
 * Resolves the user's preference to a concrete system.
 *
 * @param localeDefault what the device locale prefers; supplied by the Android
 *   layer so this module stays free of Android APIs.
 */
fun UnitSystem.resolve(localeDefault: ResolvedUnitSystem): ResolvedUnitSystem = when (this) {
    UnitSystem.SYSTEM -> localeDefault
    UnitSystem.METRIC -> ResolvedUnitSystem.METRIC
    UnitSystem.IMPERIAL -> ResolvedUnitSystem.IMPERIAL
}
