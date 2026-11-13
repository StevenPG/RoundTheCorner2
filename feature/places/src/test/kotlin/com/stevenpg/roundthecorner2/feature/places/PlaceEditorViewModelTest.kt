package com.stevenpg.roundthecorner2.feature.places

import app.cash.turbine.test
import com.stevenpg.roundthecorner2.core.model.CoordinateParser
import com.stevenpg.roundthecorner2.core.model.Coordinates
import com.stevenpg.roundthecorner2.core.model.PlaceIcon
import com.stevenpg.roundthecorner2.core.testing.FakeGeocodingRepository
import com.stevenpg.roundthecorner2.core.testing.FakePlaceRepository
import com.stevenpg.roundthecorner2.core.testing.MainDispatcherRule
import com.stevenpg.roundthecorner2.feature.places.editor.PlaceEditorAction
import com.stevenpg.roundthecorner2.feature.places.editor.PlaceEditorEvent
import com.stevenpg.roundthecorner2.feature.places.editor.PlaceEditorViewModel
import com.stevenpg.roundthecorner2.feature.places.editor.SearchState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class PlaceEditorViewModelTest {

    @get:Rule
    val main = MainDispatcherRule(StandardTestDispatcher())

    private val places = FakePlaceRepository(listOf(TestData.home))
    private val geocoder = FakeGeocodingRepository(results = TestData.searchResults)

    private fun newEditor() = PlaceEditorViewModel(null, places, geocoder)
    private fun editorFor(id: Long) = PlaceEditorViewModel(id, places, geocoder)

    private val PlaceEditorViewModel.state get() = uiState.value

    // --- Coordinates only (no address lookup needed) -------------------------

    @Test
    fun `a place can be saved from typed coordinates alone`() = runTest(main.testDispatcher) {
        val vm = newEditor()
        vm.onAction(PlaceEditorAction.NameChanged("Trailhead"))
        assertFalse("No location yet", vm.state.canSave)

        vm.onAction(PlaceEditorAction.CoordinatesTextChanged("""39°38'19"N 105°14'21"W"""))
        assertEquals(39.638611, vm.state.coordinates!!.latitude, 1e-6)
        assertTrue(vm.state.canSave)

        vm.events.test {
            vm.onAction(PlaceEditorAction.Save)
            assertEquals(PlaceEditorEvent.Saved, awaitItem())
        }
        val saved = places.observePlaces().first().single { it.name == "Trailhead" }
        assertNull("No address was required", saved.address)
        assertTrue(geocoder.queries.isEmpty())
    }

    @Test
    fun `half-typed coordinates are not an error, out-of-range ones are`() = runTest(main.testDispatcher) {
        val vm = newEditor()
        vm.onAction(PlaceEditorAction.CoordinatesTextChanged("39.7"))
        assertEquals(CoordinateParser.Error.UNRECOGNIZED_FORMAT, vm.state.coordinatesError)
        assertNull(vm.state.coordinates)

        vm.onAction(PlaceEditorAction.CoordinatesTextChanged("95, 10"))
        assertEquals(CoordinateParser.Error.LATITUDE_OUT_OF_RANGE, vm.state.coordinatesError)

        vm.onAction(PlaceEditorAction.CoordinatesTextChanged(""))
        assertNull("Empty is not an error", vm.state.coordinatesError)
    }

    // --- Search ------------------------------------------------------------

    @Test
    fun `search waits for a typing pause and only sends the final query`() = runTest(main.testDispatcher) {
        val vm = newEditor()
        vm.onAction(PlaceEditorAction.SearchQueryChanged("Den"))
        advanceTimeBy(200)
        vm.onAction(PlaceEditorAction.SearchQueryChanged("Denver air"))
        advanceUntilIdle()

        assertEquals(listOf("Denver air"), geocoder.queries)
        assertEquals(SearchState.Results(TestData.searchResults), vm.state.search)
    }

    @Test
    fun `short queries never hit the network`() = runTest(main.testDispatcher) {
        val vm = newEditor()
        vm.onAction(PlaceEditorAction.SearchQueryChanged("De"))
        advanceUntilIdle()
        assertTrue(geocoder.queries.isEmpty())
        assertEquals(SearchState.Idle, vm.state.search)
    }

    @Test
    fun `network failure is reported without blocking manual entry`() = runTest(main.testDispatcher) {
        geocoder.failWith = IOException("offline")
        val vm = newEditor()
        vm.onAction(PlaceEditorAction.SearchQueryChanged("Denver"))
        advanceUntilIdle()
        assertEquals(SearchState.Failed, vm.state.search)

        vm.onAction(PlaceEditorAction.CoordinatesTextChanged("39.7392, -104.9903"))
        vm.onAction(PlaceEditorAction.NameChanged("Downtown"))
        assertTrue(vm.state.canSave)
    }

    @Test
    fun `selecting a result fills location, address and a suggested name`() = runTest(main.testDispatcher) {
        val vm = newEditor()
        vm.onAction(PlaceEditorAction.SearchResultSelected(TestData.searchResults[0]))

        assertEquals(TestData.searchResults[0].coordinates, vm.state.coordinates)
        assertEquals("39.856100, -104.673700", vm.state.coordinatesText)
        assertEquals(TestData.searchResults[0].fullAddress, vm.state.address)
        assertEquals("Denver International Airport", vm.state.name)
        assertEquals(SearchState.Idle, vm.state.search)
        assertEquals("", vm.state.searchQuery)
    }

    @Test
    fun `selecting a result keeps a name the user already typed`() = runTest(main.testDispatcher) {
        val vm = newEditor()
        vm.onAction(PlaceEditorAction.NameChanged("Flight home"))
        vm.onAction(PlaceEditorAction.SearchResultSelected(TestData.searchResults[0]))
        assertEquals("Flight home", vm.state.name)
    }

    // --- Map ---------------------------------------------------------------

    @Test
    fun `nudging the pin keeps the address, moving it far away clears it`() = runTest(main.testDispatcher) {
        val vm = newEditor()
        val result = TestData.searchResults[0]
        vm.onAction(PlaceEditorAction.SearchResultSelected(result))

        val nudge = Coordinates(result.coordinates.latitude + 0.0005, result.coordinates.longitude) // ~55 m
        vm.onAction(PlaceEditorAction.MapMoved(nudge))
        assertEquals(result.fullAddress, vm.state.address)
        assertEquals(nudge, vm.state.coordinates)
        assertEquals(nudge.toString(), vm.state.coordinatesText)

        vm.onAction(PlaceEditorAction.MapMoved(Coordinates(39.0, -105.0)))
        assertEquals("", vm.state.address)
    }

    // --- Editing existing places ---------------------------------------------

    @Test
    fun `editing loads the place and saves changes in place`() = runTest(main.testDispatcher) {
        val vm = editorFor(TestData.home.id.value)
        assertTrue(vm.state.isLoading)
        advanceUntilIdle()
        assertFalse(vm.state.isNew)
        assertEquals("Home", vm.state.name)
        assertEquals(TestData.home.address, vm.state.address)

        vm.onAction(PlaceEditorAction.IconSelected(PlaceIcon.FAMILY))
        vm.onAction(PlaceEditorAction.Save)
        advanceUntilIdle()
        assertEquals(PlaceIcon.FAMILY, places.getPlace(TestData.home.id)!!.icon)
        assertEquals(1, places.observePlaces().first().size)
    }

    @Test
    fun `deleting a place in use reports the trigger count`() = runTest(main.testDispatcher) {
        places.triggerUsage[TestData.home.id] = 2
        val vm = editorFor(TestData.home.id.value)
        advanceUntilIdle()
        vm.events.test {
            vm.onAction(PlaceEditorAction.Delete)
            assertEquals(PlaceEditorEvent.InUse(2), awaitItem())
        }
    }

    @Test
    fun `names are capped at the maximum length`() = runTest(main.testDispatcher) {
        val vm = newEditor()
        vm.onAction(PlaceEditorAction.NameChanged("x".repeat(100)))
        assertEquals(60, vm.state.name.length)
    }
}
