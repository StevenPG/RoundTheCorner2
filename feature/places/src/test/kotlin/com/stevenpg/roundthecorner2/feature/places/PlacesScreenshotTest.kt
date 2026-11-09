package com.stevenpg.roundthecorner2.feature.places

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.stevenpg.roundthecorner2.core.model.CoordinateParser
import com.stevenpg.roundthecorner2.core.model.Coordinates
import com.stevenpg.roundthecorner2.core.model.PlaceIcon
import com.stevenpg.roundthecorner2.core.testing.SCREENSHOT_SDK
import com.stevenpg.roundthecorner2.core.testing.captureLightAndDark
import com.stevenpg.roundthecorner2.feature.places.editor.MapContent
import com.stevenpg.roundthecorner2.feature.places.editor.PlaceEditorScreen
import com.stevenpg.roundthecorner2.feature.places.editor.PlaceEditorUiState
import com.stevenpg.roundthecorner2.feature.places.editor.SearchState
import com.stevenpg.roundthecorner2.feature.places.list.PlacesScreen
import com.stevenpg.roundthecorner2.feature.places.list.PlacesUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class PlacesScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    /** Stand-in for MapLibre (native GL can't run on the JVM): a tinted box with the pin. */
    private val fakeMap: MapContent = { _, _, modifier ->
        Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Icon(
                Icons.Filled.Place,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.align(Alignment.Center).offset(y = (-20).dp).size(40.dp),
            )
        }
    }

    @Test
    fun listEmpty(): Unit = composeRule.captureLightAndDark("places_empty") {
        PlacesScreen(PlacesUiState.Ready(emptyList()), onAddPlace = {}, onEditPlace = {}, onDeletePlace = {})
    }

    @Test
    fun listPopulated(): Unit = composeRule.captureLightAndDark("places_list") {
        PlacesScreen(
            PlacesUiState.Ready(listOf(TestData.home, TestData.trailhead, TestData.office)),
            onAddPlace = {},
            onEditPlace = {},
            onDeletePlace = {},
        )
    }

    @Test
    fun editorNew(): Unit = composeRule.captureLightAndDark("place_editor_new") {
        PlaceEditorScreen(PlaceEditorUiState(isNew = true), onAction = {}, onClose = {}, mapContent = fakeMap)
    }

    @Test
    fun editorSearchResults(): Unit = composeRule.captureLightAndDark("place_editor_search") {
        PlaceEditorScreen(
            PlaceEditorUiState(
                isNew = true,
                searchQuery = "Denver",
                search = SearchState.Results(TestData.searchResults),
            ),
            onAction = {},
            onClose = {},
            mapContent = fakeMap,
        )
    }

    @Test
    fun editorCoordinatesOnly(): Unit = composeRule.captureLightAndDark("place_editor_coordinates") {
        PlaceEditorScreen(
            PlaceEditorUiState(
                isNew = true,
                name = "Mount Falcon trailhead",
                icon = PlaceIcon.TRAVEL,
                coordinatesText = """39°38'19"N 105°14'21"W""",
                coordinates = Coordinates(39.638611, -105.239167),
            ),
            onAction = {},
            onClose = {},
            mapContent = fakeMap,
        )
    }

    @Test
    fun editorExistingWithError(): Unit = composeRule.captureLightAndDark("place_editor_error") {
        PlaceEditorScreen(
            PlaceEditorUiState(
                isNew = false,
                name = "Home",
                icon = PlaceIcon.HOME,
                coordinatesText = "97.2, -104.99",
                coordinatesError = CoordinateParser.Error.LATITUDE_OUT_OF_RANGE,
                address = TestData.home.address!!,
            ),
            onAction = {},
            onClose = {},
            mapContent = fakeMap,
        )
    }
}
