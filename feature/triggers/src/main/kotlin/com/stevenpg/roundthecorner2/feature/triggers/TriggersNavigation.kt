package com.stevenpg.roundthecorner2.feature.triggers

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Navigation 3 destination key for the Triggers list (a top-level tab). */
@Serializable
data object TriggersNavKey : NavKey

/**
 * Registers the Triggers destinations with the app's navigation graph.
 *
 * @param onAddTrigger invoked by the FAB / empty-state button. The editor screen
 *   lands in a later milestone; the app module wires it up then.
 */
fun EntryProviderScope<NavKey>.triggersEntries(onAddTrigger: () -> Unit) {
    entry<TriggersNavKey> { TriggersScreen(onAddTrigger = onAddTrigger) }
}
