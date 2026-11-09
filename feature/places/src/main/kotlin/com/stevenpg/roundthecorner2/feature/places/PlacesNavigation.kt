package com.stevenpg.roundthecorner2.feature.places

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.stevenpg.roundthecorner2.feature.places.editor.PlaceEditorRoute
import com.stevenpg.roundthecorner2.feature.places.list.PlacesRoute
import kotlinx.serialization.Serializable

/** The Places tab (list of saved places). */
@Serializable
data object PlacesNavKey : NavKey

/** Create (placeId = null) or edit a place. */
@Serializable
data class PlaceEditorNavKey(val placeId: Long? = null) : NavKey

/**
 * Registers the Places destinations.
 *
 * @param navigate pushes a key on the current tab's back stack.
 * @param goBack pops the current destination.
 */
fun EntryProviderScope<NavKey>.placesEntries(
    navigate: (NavKey) -> Unit,
    goBack: () -> Unit,
) {
    entry<PlacesNavKey> {
        PlacesRoute(
            onAddPlace = { navigate(PlaceEditorNavKey()) },
            onEditPlace = { id -> navigate(PlaceEditorNavKey(id.value)) },
        )
    }
    entry<PlaceEditorNavKey> { key ->
        PlaceEditorRoute(placeId = key.placeId, onClose = goBack)
    }
}
