package com.stevenpg.roundthecorner2.feature.triggers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.stevenpg.roundthecorner2.core.model.ResolvedUnitSystem
import com.stevenpg.roundthecorner2.core.model.TriggerDirection
import com.stevenpg.roundthecorner2.core.testing.SCREENSHOT_SDK
import com.stevenpg.roundthecorner2.core.testing.captureLightAndDark
import com.stevenpg.roundthecorner2.feature.triggers.editor.RadiusMapContent
import com.stevenpg.roundthecorner2.feature.triggers.editor.TriggerEditorScreen
import com.stevenpg.roundthecorner2.feature.triggers.editor.TriggerEditorUiState
import com.stevenpg.roundthecorner2.feature.triggers.list.TriggersScreen
import com.stevenpg.roundthecorner2.feature.triggers.list.TriggersUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class TriggersScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    /** MapLibre can't render on the JVM: draw a stand-in circle instead. */
    private val fakeMap: RadiusMapContent = { _, _, modifier ->
        Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(140.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
            )
        }
    }

    private val readyEditor = TriggerEditorUiState(
        isNew = true,
        isLoading = false,
        places = listOf(TestData.home, TestData.office),
        recipients = listOf(TestData.alex, TestData.family),
        placeId = TestData.home.id,
        recipientId = TestData.alex.id,
        unitSystem = ResolvedUnitSystem.IMPERIAL,
        radiusText = "2",
        message = TextFieldValue("Almost home! About {distance} away.", TextRange(35)),
        preview = "Almost home! About 2.0 mi away.",
        minRadiusText = "80 ft",
        maxRadiusText = "621 mi",
    )

    @Test
    fun listEmpty(): Unit = composeRule.captureLightAndDark("triggers_empty") {
        TriggersScreen(TriggersUiState.Ready(emptyList(), ResolvedUnitSystem.IMPERIAL), {}, {}, { _, _ -> }, {})
    }

    @Test
    fun listPopulated(): Unit = composeRule.captureLightAndDark("triggers_list") {
        TriggersScreen(
            TriggersUiState.Ready(TestData.listDetails, ResolvedUnitSystem.IMPERIAL),
            {}, {}, { _, _ -> }, {},
            zone = ZoneOffset.UTC,
        )
    }

    @Test
    fun editorNew(): Unit = composeRule.captureLightAndDark("trigger_editor_new") {
        TriggerEditorScreen(readyEditor, onAction = {}, onClose = {}, onCreatePlace = {}, onCreateRecipient = {}, mapContent = fakeMap)
    }

    @Test
    fun editorNothingSavedYet(): Unit = composeRule.captureLightAndDark("trigger_editor_empty") {
        TriggerEditorScreen(
            TriggerEditorUiState(
                isNew = true, isLoading = false, radiusText = "0.01",
                unitSystem = ResolvedUnitSystem.IMPERIAL, minRadiusText = "80 ft",
            ),
            onAction = {}, onClose = {}, onCreatePlace = {}, onCreateRecipient = {}, mapContent = fakeMap,
        )
    }

    @Test
    fun editorLeaveWithHistory(): Unit = composeRule.captureLightAndDark("trigger_editor_history") {
        TriggerEditorScreen(
            readyEditor.copy(
                isNew = false,
                placeId = TestData.office.id,
                recipientId = TestData.family.id,
                direction = TriggerDirection.LEAVE,
                radiusText = "0.5",
                message = TextFieldValue("Leaving work at {time}"),
                preview = "Leaving work at 5:42 PM",
                history = TestData.history,
            ),
            onAction = {}, onClose = {}, onCreatePlace = {}, onCreateRecipient = {}, mapContent = fakeMap,
            zone = ZoneOffset.UTC,
        )
    }
}
