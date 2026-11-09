package com.stevenpg.roundthecorner2.core.messaging

/** Outcome of one delivery attempt. */
sealed interface SendResult {
    /** Accepted by the service / carrier. */
    data object Sent : SendResult

    /**
     * Handed to the user to finish (Play-build SMS: the composer is pre-filled
     * and they tap Send). We can't observe whether they did.
     */
    data object AwaitingUser : SendResult

    data class Failed(val reason: FailureReason, val detail: String) : SendResult {
        /** Worth trying again later without the user changing anything. */
        val retryable: Boolean get() = reason.retryable
    }
}

/** Why a send failed; decides whether a retry could help and what the UI says. */
enum class FailureReason(val retryable: Boolean) {
    /** No connectivity, DNS failure, timeout. */
    NETWORK(retryable = true),

    /** The service asked us to slow down (HTTP 429). */
    RATE_LIMITED(retryable = true),

    /** 5xx from the service. */
    SERVER_ERROR(retryable = true),

    /** Cellular radio off / no service / airplane mode. */
    NO_SERVICE(retryable = true),

    /** Webhook deleted or revoked, invalid phone number, unreadable stored details. */
    DESTINATION_INVALID(retryable = false),

    /** The service refused this specific message (other 4xx). */
    REJECTED(retryable = false),

    /** SEND_SMS not granted. */
    PERMISSION_DENIED(retryable = false),

    /** Play-build SMS needs a notification to hand off, and they're blocked. */
    NOTIFICATIONS_BLOCKED(retryable = false),

    /** This device can't send SMS at all (e.g. a Wi-Fi-only tablet). */
    UNSUPPORTED(retryable = false),

    UNKNOWN(retryable = false),
}
