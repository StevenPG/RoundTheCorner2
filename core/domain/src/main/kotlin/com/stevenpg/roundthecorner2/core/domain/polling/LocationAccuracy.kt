package com.stevenpg.roundthecorner2.core.domain.polling

/**
 * How precise the next location fixes need to be. This is the biggest battery
 * lever after the polling interval: GPS costs far more than Wi-Fi or cell positioning.
 */
enum class LocationAccuracy {
    /** GPS-level fixes (a few metres). Needed near a trigger's boundary. */
    PRECISE,

    /**
     * Wi-Fi and cell positioning (roughly 100 m to a few km), no GPS. Good
     * enough to know you're still far away.
     */
    LOW_POWER,
}
