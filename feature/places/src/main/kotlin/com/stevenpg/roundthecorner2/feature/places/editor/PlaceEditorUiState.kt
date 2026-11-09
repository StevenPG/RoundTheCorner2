package com.stevenpg.roundthecorner2.feature.places.editor

import com.stevenpg.roundthecorner2.core.model.Coordinates
import com.stevenpg.roundthecorner2.core.model.CoordinateParser
import com.stevenpg.roundthecorner2.core.model.GeocodedPlace
import com.stevenpg.roundthecorner2.core.model.PlaceIcon

/** Everything the place editor renders. */
data class PlaceEditorUiState(
    val isNew: Boolean,
    val isLoading: Boolean = false,
    val name: String = "",
    val icon: PlaceIcon = PlaceIcon.PIN,
    /** Raw text of the coordinates field, exactly as typed/pasted. */
    val coordinatesText: String = "",
    /** Parsed from [coordinatesText] or set from the map/search; null = no location yet. */
    val coordinates: Coordinates? = null,
    /** Why [coordinatesText] didn't parse; null while valid or empty. */
    val coordinatesError: CoordinateParser.Error? = null,
    val address: String = "",
    val searchQuery: String = "",
    val search: SearchState = SearchState.Idle,
    val isSaving: Boolean = false,
) {
    /** The two things a place needs: a name and a location. Address is optional. */
    val canSave: Boolean
        get() = !isLoading && !isSaving && name.isNotBlank() && coordinates != null
}

sealed interface SearchState {
    data object Idle : SearchState
    data object Searching : SearchState
    data class Results(val places: List<GeocodedPlace>) : SearchState
    data object NoResults : SearchState
    data object Failed : SearchState
}

/** Everything the user can do on the editor screen. A single entry point keeps the screen API small. */
sealed interface PlaceEditorAction {
    data class NameChanged(val name: String) : PlaceEditorAction
    data class IconSelected(val icon: PlaceIcon) : PlaceEditorAction
    data class CoordinatesTextChanged(val text: String) : PlaceEditorAction
    data class MapMoved(val coordinates: Coordinates) : PlaceEditorAction
    data class AddressChanged(val address: String) : PlaceEditorAction
    data class SearchQueryChanged(val query: String) : PlaceEditorAction
    data object SearchSubmitted : PlaceEditorAction
    data class SearchResultSelected(val result: GeocodedPlace) : PlaceEditorAction
    data object Save : PlaceEditorAction
    data object Delete : PlaceEditorAction
}

/** One-shot results the screen reacts to (navigate away, show a message). */
sealed interface PlaceEditorEvent {
    data object Saved : PlaceEditorEvent
    data object Deleted : PlaceEditorEvent
    data class InUse(val triggerCount: Int) : PlaceEditorEvent
}
