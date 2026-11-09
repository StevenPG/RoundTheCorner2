package com.stevenpg.roundthecorner2.feature.places

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Navigation 3 destination key for the Places list (a top-level tab). */
@Serializable
data object PlacesNavKey : NavKey

/**
 * Registers the Places destinations with the app's navigation graph.
 *
 * @param onAddPlace invoked by the FAB / empty-state button. The editor screen
 *   lands in a later milestone; the app module wires it up then.
 */
fun EntryProviderScope<NavKey>.placesEntries(onAddPlace: () -> Unit) {
    entry<PlacesNavKey> { PlacesScreen(onAddPlace = onAddPlace) }
}
