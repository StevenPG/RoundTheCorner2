package com.stevenpg.roundthecorner2.feature.places.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stevenpg.roundthecorner2.core.data.repository.DeleteResult
import com.stevenpg.roundthecorner2.core.data.repository.GeocodingRepository
import com.stevenpg.roundthecorner2.core.data.repository.PlaceDraft
import com.stevenpg.roundthecorner2.core.data.repository.PlaceRepository
import com.stevenpg.roundthecorner2.core.domain.geo.Geodesy
import com.stevenpg.roundthecorner2.core.model.CoordinateParser
import com.stevenpg.roundthecorner2.core.model.Coordinates
import com.stevenpg.roundthecorner2.core.model.GeocodedPlace
import com.stevenpg.roundthecorner2.core.model.PlaceId
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

/**
 * State holder for creating or editing a place.
 *
 * A place's location can come from three interchangeable sources, all of which
 * keep each other in sync:
 *  1. the **coordinates field** (typed or pasted; no network needed),
 *  2. an **address search** result (Nominatim),
 *  3. **panning the map** under the fixed centre pin.
 *
 * `placeId` is passed in with Hilt *assisted injection* (a constructor arg
 * supplied at runtime alongside injected dependencies).
 */
@HiltViewModel(assistedFactory = PlaceEditorViewModel.Factory::class)
class PlaceEditorViewModel @AssistedInject constructor(
    @Assisted private val placeId: Long?,
    private val placeRepository: PlaceRepository,
    private val geocodingRepository: GeocodingRepository,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(placeId: Long?): PlaceEditorViewModel
    }

    private val _uiState = MutableStateFlow(PlaceEditorUiState(isNew = placeId == null, isLoading = placeId != null))
    val uiState: StateFlow<PlaceEditorUiState> = _uiState.asStateFlow()

    private val _events = Channel<PlaceEditorEvent>(Channel.BUFFERED)
    val events: Flow<PlaceEditorEvent> = _events.receiveAsFlow()

    /**
     * Typed queries (debounced). A StateFlow, not a SharedFlow, so a query typed
     * before the collector starts is never lost - it always holds the latest.
     */
    private val typedQuery = MutableStateFlow("")

    /** Keyboard "search" submits skip the debounce. Conflated: only the latest matters. */
    private val immediateSearches = Channel<String>(Channel.CONFLATED)

    /** Where the current address came from; moving the pin far from it invalidates the address. */
    private var addressAnchor: Coordinates? = null

    init {
        if (placeId != null) loadExisting(PlaceId(placeId))

        // Typing: wait for a pause, and cancel any in-flight search when the query changes.
        viewModelScope.launch {
            typedQuery.debounce(SEARCH_DEBOUNCE).collectLatest(::runSearch)
        }
        viewModelScope.launch { immediateSearches.receiveAsFlow().collectLatest(::runSearch) }
    }

    fun onAction(action: PlaceEditorAction) {
        when (action) {
            is PlaceEditorAction.NameChanged ->
                _uiState.update { it.copy(name = action.name.take(PlaceDraft.MAX_NAME_LENGTH)) }

            is PlaceEditorAction.IconSelected -> _uiState.update { it.copy(icon = action.icon) }

            is PlaceEditorAction.CoordinatesTextChanged -> onCoordinatesTextChanged(action.text)

            is PlaceEditorAction.MapMoved -> setCoordinatesFrom(action.coordinates)

            is PlaceEditorAction.AddressChanged -> {
                addressAnchor = null // User-typed address: never clear it automatically.
                _uiState.update { it.copy(address = action.address) }
            }

            is PlaceEditorAction.SearchQueryChanged -> {
                _uiState.update { it.copy(searchQuery = action.query) }
                if (action.query.isBlank()) {
                    _uiState.update { it.copy(search = SearchState.Idle) }
                }
                typedQuery.value = action.query
            }

            PlaceEditorAction.SearchSubmitted -> immediateSearches.trySend(_uiState.value.searchQuery)

            is PlaceEditorAction.SearchResultSelected -> onSearchResultSelected(action.result)

            PlaceEditorAction.Save -> save()

            PlaceEditorAction.Delete -> delete()
        }
    }

    private fun loadExisting(id: PlaceId) = viewModelScope.launch {
        val place = placeRepository.getPlace(id)
        if (place == null) {
            // Deleted elsewhere (e.g. another screen); nothing to edit.
            _events.send(PlaceEditorEvent.Deleted)
            return@launch
        }
        addressAnchor = place.coordinates.takeIf { place.address != null }
        _uiState.update {
            it.copy(
                isLoading = false,
                name = place.name,
                icon = place.icon,
                coordinates = place.coordinates,
                coordinatesText = place.coordinates.toString(),
                address = place.address.orEmpty(),
            )
        }
    }

    private fun onCoordinatesTextChanged(text: String) {
        when (val result = CoordinateParser.parse(text)) {
            is CoordinateParser.Result.Success -> {
                clearAddressIfMovedAway(result.coordinates)
                _uiState.update {
                    it.copy(coordinatesText = text, coordinates = result.coordinates, coordinatesError = null)
                }
            }
            is CoordinateParser.Result.Failure -> _uiState.update {
                it.copy(
                    coordinatesText = text,
                    coordinates = null,
                    coordinatesError = result.error.takeUnless { e -> e == CoordinateParser.Error.EMPTY },
                )
            }
        }
    }

    /** Map pan or search result: update the coordinates and rewrite the text field to match. */
    private fun setCoordinatesFrom(coordinates: Coordinates) {
        clearAddressIfMovedAway(coordinates)
        _uiState.update {
            it.copy(coordinates = coordinates, coordinatesText = coordinates.toString(), coordinatesError = null)
        }
    }

    private fun onSearchResultSelected(result: GeocodedPlace) {
        addressAnchor = result.coordinates
        typedQuery.value = "" // So typing the same query again triggers a fresh search.
        _uiState.update {
            it.copy(
                coordinates = result.coordinates,
                coordinatesText = result.coordinates.toString(),
                coordinatesError = null,
                address = result.fullAddress,
                // Suggest a name only if the user hasn't typed one.
                name = it.name.ifBlank { result.title.take(PlaceDraft.MAX_NAME_LENGTH) },
                searchQuery = "",
                search = SearchState.Idle,
            )
        }
    }

    /**
     * Small pin nudges (finding the right driveway) keep the searched address;
     * moving it somewhere else entirely clears it, since it would be wrong.
     */
    private fun clearAddressIfMovedAway(newCoordinates: Coordinates) {
        val anchor = addressAnchor ?: return
        if (Geodesy.distance(anchor, newCoordinates).meters > ADDRESS_KEEP_RADIUS_METERS) {
            addressAnchor = null
            _uiState.update { it.copy(address = "") }
        }
    }

    private suspend fun runSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < MIN_QUERY_LENGTH) {
            _uiState.update { it.copy(search = SearchState.Idle) }
            return
        }
        _uiState.update { it.copy(search = SearchState.Searching) }
        val next = try {
            val results = geocodingRepository.search(trimmed)
            if (results.isEmpty()) SearchState.NoResults else SearchState.Results(results)
        } catch (e: IOException) {
            SearchState.Failed
        }
        // Ignore results for a query the user has since cleared.
        if (_uiState.value.searchQuery.isNotBlank()) _uiState.update { it.copy(search = next) }
    }

    private fun save() {
        val state = _uiState.value
        val coordinates = state.coordinates
        if (!state.canSave || coordinates == null) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val draft = PlaceDraft(
                name = state.name.trim(),
                coordinates = coordinates,
                icon = state.icon,
                address = state.address.trim().ifEmpty { null },
            )
            if (placeId == null) {
                placeRepository.createPlace(draft)
            } else {
                placeRepository.updatePlace(PlaceId(placeId), draft)
            }
            _events.send(PlaceEditorEvent.Saved)
        }
    }

    private fun delete() {
        val id = placeId ?: return
        viewModelScope.launch {
            when (val result = placeRepository.deletePlace(PlaceId(id))) {
                DeleteResult.Deleted, DeleteResult.NotFound -> _events.send(PlaceEditorEvent.Deleted)
                is DeleteResult.InUse -> _events.send(PlaceEditorEvent.InUse(result.triggerCount))
            }
        }
    }

    companion object {
        /** Long enough to wait for a typing pause; also keeps us polite to Nominatim. */
        val SEARCH_DEBOUNCE = 600.milliseconds
        const val MIN_QUERY_LENGTH = 3
        const val ADDRESS_KEEP_RADIUS_METERS = 150.0
    }
}
