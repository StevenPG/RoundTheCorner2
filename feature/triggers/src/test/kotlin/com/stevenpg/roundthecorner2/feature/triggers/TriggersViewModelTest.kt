package com.stevenpg.roundthecorner2.feature.triggers

import app.cash.turbine.test
import com.stevenpg.roundthecorner2.core.data.preferences.UnitSystemProvider
import com.stevenpg.roundthecorner2.core.model.ResolvedUnitSystem
import com.stevenpg.roundthecorner2.core.model.TriggerStatus
import com.stevenpg.roundthecorner2.core.testing.FakePlaceRepository
import com.stevenpg.roundthecorner2.core.testing.FakeRecipientRepository
import com.stevenpg.roundthecorner2.core.testing.FakeTriggerRepository
import com.stevenpg.roundthecorner2.core.testing.FakeUserPreferencesRepository
import com.stevenpg.roundthecorner2.core.testing.MainDispatcherRule
import com.stevenpg.roundthecorner2.feature.triggers.list.TriggersUiState
import com.stevenpg.roundthecorner2.feature.triggers.list.TriggersViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TriggersViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val places = FakePlaceRepository(listOf(TestData.home, TestData.office, TestData.gym))
    private val recipients = FakeRecipientRepository(listOf(TestData.alex, TestData.family))
    private val triggers = FakeTriggerRepository(
        places, recipients, listOf(TestData.homeArrive, TestData.officeLeave, TestData.gymArrive),
    )
    // Lazy: must be created after MainDispatcherRule has installed the test
    // Main dispatcher, or viewModelScope work escapes the test scheduler.
    private val viewModel by lazy {
        TriggersViewModel(
            triggers,
            UnitSystemProvider(FakeUserPreferencesRepository()) { ResolvedUnitSystem.IMPERIAL },
        )
    }

    @Test
    fun `lists armed first and the switch arms, re-arms and pauses`() = runTest(main.testDispatcher) {
        viewModel.uiState.test {
            var state = awaitItem()
            while (state !is TriggersUiState.Ready) state = awaitItem()
            assertEquals(ResolvedUnitSystem.IMPERIAL, state.unitSystem)
            assertEquals(
                listOf(TriggerStatus.ARMED, TriggerStatus.PAUSED, TriggerStatus.FIRED),
                state.triggers.map { it.trigger.status },
            )
            cancelAndIgnoreRemainingEvents()
        }
        viewModel.setWatching(TestData.officeLeave.id, true) // Re-arm a trigger that already sent.
        viewModel.setWatching(TestData.homeArrive.id, false)
        advanceUntilIdle()

        assertEquals(TriggerStatus.ARMED, triggers.get(TestData.officeLeave.id)!!.status)
        assertEquals(TriggerStatus.PAUSED, triggers.get(TestData.homeArrive.id)!!.status)
        assertEquals(
            "History is kept when re-arming",
            TestData.officeLeave.lastFiredAt,
            triggers.get(TestData.officeLeave.id)!!.lastFiredAt,
        )
    }

    @Test
    fun `delete announces itself`() = runTest(main.testDispatcher) {
        viewModel.deleted.test {
            viewModel.delete(TestData.gymArrive.id)
            awaitItem()
        }
    }
}
