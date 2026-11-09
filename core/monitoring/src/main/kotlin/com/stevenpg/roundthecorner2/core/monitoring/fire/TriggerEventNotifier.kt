package com.stevenpg.roundthecorner2.core.monitoring.fire

import com.stevenpg.roundthecorner2.core.model.TriggerDetails

/** User-facing notifications about individual trigger events (separate from the ongoing status). */
interface TriggerEventNotifier {
    fun sent(details: TriggerDetails, message: String)
    fun willRetry(details: TriggerDetails, reason: String)
    fun failed(details: TriggerDetails, reason: String)

    /** Armed while already on the "fire" side: offer Send now / Keep waiting. */
    fun alreadyHere(details: TriggerDetails)
    fun cancelAlreadyHere(details: TriggerDetails)
}
