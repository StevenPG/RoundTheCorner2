package com.stevenpg.roundthecorner2.feature.triggers

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import app.cash.turbine.test
import com.stevenpg.roundthecorner2.core.data.preferences.UnitSystemProvider
import com.stevenpg.roundthecorner2.core.data.repository.PlaceDraft
import com.stevenpg.roundthecorner2.core.domain.message.MessageTemplate.Placeholder
import com.stevenpg.roundthecorner2.core.model.ResolvedUnitSystem
import com.stevenpg.roundthecorner2.core.model.TriggerDirection
import com.stevenpg.roundthecorner2.core.model.TriggerId
import com.stevenpg.roundthecorner2.core.model.TriggerStatus
import com.stevenpg.roundthecorner2.core.model.UnitSystem
import com.stevenpg.roundthecorner2.core.model.UserPreferences
import com.stevenpg.roundthecorner2.core.model.kilometers
import com.stevenpg.roundthecorner2.core.model.miles
import com.stevenpg.roundthecorner2.core.testing.FakePlaceRepository
import com.stevenpg.roundthecorner2.core.testing.FakeRecipientRepository
import com.stevenpg.roundthecorner2.core.testing.FakeSendHistoryRepository
import com.stevenpg.roundthecorner2.core.testing.FakeTriggerRepository
import com.stevenpg.roundthecorner2.core.testing.FakeUserPreferencesRepository
import com.stevenpg.roundthecorner2.core.testing.MainDispatcherRule
import com.stevenpg.roundthecorner2.feature.triggers.editor.MessageDefaults
import com.stevenpg.roundthecorner2.feature.triggers.editor.RadiusError
import com.stevenpg.roundthecorner2.feature.triggers.editor.TriggerEditorAction
import com.stevenpg.roundthecorner2.feature.triggers.editor.TriggerEditorEvent
import com.stevenpg.roundthecorner2.feature.triggers.editor.TriggerEditorViewModel
import com.stevenpg.roundthecorner2.feature.triggers.editor.insert
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.ZoneOffset

class TriggerEditorViewModelTest {

    @get:Rule
    val main = MainDispatcherRule(StandardTestDispatcher())

    private val places = FakePlaceRepository(listOf(TestData.home))
    private val recipients = FakeRecipientRepository(listOf(TestData.alex))
    private val triggers = FakeTriggerRepository(places, recipients, listOf(TestData.homeArrive))
    private val history = FakeSendHistoryRepository(TestData.history)
    private val defaults = MessageDefaults(arrive = "Almost at {place}!", leave = "Left {place}.")

    private fun editor(id: Long? = null, units: UnitSystem = UnitSystem.IMPERIAL) = TriggerEditorViewModel(
        triggerId = id,
        defaults = defaults,
        triggerRepository = triggers,
        placeRepository = places,
        recipientRepository = recipients,
        historyRepository = history,
        // "Automatic" resolves to metric in these tests.
        unitSystemProvider = UnitSystemProvider(FakeUserPreferencesRepository(UserPreferences(unitSystem = units))) {
            ResolvedUnitSystem.METRIC
        },
        clock = Clock.fixed(TestData.now, ZoneOffset.UTC),
    )

    private val TriggerEditorViewModel.state get() = uiState.value

    @Test
    fun `new trigger preselects the only place and recipient with sensible defaults`() = runTest(main.testDispatcher) {
        val vm = editor()
        advanceUntilIdle()
        assertFalse(vm.state.isLoading)
        assertEquals(TestData.home.id, vm.state.placeId)
        assertEquals(TestData.alex.id, vm.state.recipientId)
        assertEquals("0.5", vm.state.radiusText)
        assertEquals(ResolvedUnitSystem.IMPERIAL, vm.state.unitSystem)
        assertEquals("Almost at {place}!", vm.state.message.text)
        assertEquals("Almost at Home!", vm.state.preview)
        assertTrue(vm.state.canSave)
    }

    @Test
    fun `saving creates an armed trigger stored in meters`() = runTest(main.testDispatcher) {
        val vm = editor()
        advanceUntilIdle()
        vm.onAction(TriggerEditorAction.RadiusPresetSelected(2.0))
        vm.events.test {
            vm.onAction(TriggerEditorAction.Save)
            assertEquals(TriggerEditorEvent.Saved, awaitItem())
        }
        val created = triggers.get(TriggerId(2))!!
        assertEquals(TriggerStatus.ARMED, created.status)
        assertEquals(2.miles.meters, created.radius.meters, 0.001)
    }

    @Test
    fun `metric users enter kilometres and decimal commas work`() = runTest(main.testDispatcher) {
        val vm = editor(units = UnitSystem.SYSTEM)
        advanceUntilIdle()
        vm.onAction(TriggerEditorAction.RadiusTextChanged("1,5"))
        assertEquals(1.5.kilometers.meters, vm.state.radius!!.meters, 1e-6)
        assertNull(vm.state.radiusError)
    }

    @Test
    fun `radius limits are enforced`() = runTest(main.testDispatcher) {
        val vm = editor()
        advanceUntilIdle()
        vm.onAction(TriggerEditorAction.RadiusTextChanged("0.01")) // ~16 m
        assertEquals(RadiusError.TOO_SMALL, vm.state.radiusError)
        vm.onAction(TriggerEditorAction.RadiusTextChanged("900"))
        assertEquals(RadiusError.TOO_LARGE, vm.state.radiusError)
        vm.onAction(TriggerEditorAction.RadiusTextChanged(""))
        assertEquals(RadiusError.INVALID, vm.state.radiusError)
        assertFalse(vm.state.canSave)
    }

    @Test
    fun `switching direction swaps the default message only until the user edits it`() = runTest(main.testDispatcher) {
        val vm = editor()
        advanceUntilIdle()
        vm.onAction(TriggerEditorAction.DirectionSelected(TriggerDirection.LEAVE))
        assertEquals("Left {place}.", vm.state.message.text)

        vm.onAction(TriggerEditorAction.MessageChanged(TextFieldValue("Bye from {place}")))
        vm.onAction(TriggerEditorAction.DirectionSelected(TriggerDirection.ARRIVE))
        assertEquals("Bye from {place}", vm.state.message.text)
    }

    @Test
    fun `placeholders insert at the cursor and the preview fills them`() = runTest(main.testDispatcher) {
        val vm = editor()
        advanceUntilIdle()
        vm.onAction(TriggerEditorAction.MessageChanged(TextFieldValue("Near  now", TextRange(5))))
        vm.onAction(TriggerEditorAction.InsertPlaceholder(Placeholder.PLACE))
        assertEquals("Near {place} now", vm.state.message.text)
        assertEquals(TextRange(12), vm.state.message.selection)
        assertEquals("Near Home now", vm.state.preview)

        vm.onAction(TriggerEditorAction.MessageChanged(TextFieldValue("{radius}", TextRange(8))))
        assertEquals("0.5 mi", vm.state.preview)
    }

    @Test
    fun `a place created from the editor is auto-selected on return`() = runTest(main.testDispatcher) {
        val vm = editor()
        advanceUntilIdle()
        places.createPlace(PlaceDraft("Cabin", TestData.gym.coordinates))
        advanceUntilIdle()
        assertEquals("Cabin", vm.state.place?.name)
    }

    @Test
    fun `editing loads the trigger and keeps its status`() = runTest(main.testDispatcher) {
        triggers.pause(TestData.homeArrive.id)
        val vm = editor(TestData.homeArrive.id.value)
        advanceUntilIdle()
        assertEquals("2", vm.state.radiusText)
        assertEquals(TestData.homeArrive.messageTemplate, vm.state.message.text)

        vm.onAction(TriggerEditorAction.RadiusPresetSelected(5.0))
        vm.onAction(TriggerEditorAction.Save)
        advanceUntilIdle()
        val updated = triggers.get(TestData.homeArrive.id)!!
        assertEquals(TriggerStatus.PAUSED, updated.status)
        assertEquals(5.miles.meters, updated.radius.meters, 0.001)
    }

    @Test
    fun `insert replaces a selection`() {
        val value = TextFieldValue("Hello world", TextRange(6, 11)).insert("{place}")
        assertEquals("Hello {place}", value.text)
        assertEquals(TextRange(13), value.selection)
    }
}
