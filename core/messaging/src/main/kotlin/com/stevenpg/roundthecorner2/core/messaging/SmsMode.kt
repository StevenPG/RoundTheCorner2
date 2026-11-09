package com.stevenpg.roundthecorner2.core.messaging

/**
 * How this build sends SMS. Provided by the app module per flavor so UI can
 * explain the behaviour (and ask for SEND_SMS only when it will be used).
 */
enum class SmsMode {
    /** `full` flavor: sent in the background via SmsManager; needs SEND_SMS. */
    AUTOMATIC,

    /** `play` flavor: the messaging app opens pre-filled and the user taps Send. */
    TAP_TO_SEND,
}
