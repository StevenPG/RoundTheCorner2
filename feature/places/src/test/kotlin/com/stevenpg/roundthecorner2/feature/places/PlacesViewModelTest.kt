package com.stevenpg.roundthecorner2.feature.places

import app.cash.turbine.test
import com.stevenpg.roundthecorner2.core.testing.FakePlaceRepository
import com.stevenpg.roundthecorner2.core.testing.MainDispatcherRule
import com.stevenpg.roundthecorner2.feature.places.list.PlacesMessage
import com.stevenpg.roundthecorner2.feature.places.list.PlacesUiState
import com.stevenpg.roundthecorner2.feature.places.list.PlacesViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlacesViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val repository = FakePlaceRepository(listOf(TestData.office, TestData.home))
    // Lazy: created after MainDispatcherRule installs the test Main dispatcher.
    private val viewModel by lazy { PlacesViewModel(repository) }

    @Test
    fun `lists places alphabetically`() = runTest(main.testDispatcher) {
        viewModel.uiState.test {
            var state = awaitItem()
            if (state is PlacesUiState.Loading) state = awaitItem()
            assertEquals(listOf("Home", "Office"), (state as PlacesUiState.Ready).places.map { it.name })
        }
    }

    @Test
    fun `delete reports success or the blocking trigger count`() = runTest(main.testDispatcher) {
        repository.triggerUsage[TestData.home.id] = 1
        viewModel.messages.test {
            viewModel.delete(TestData.home)
            assertEquals(PlacesMessage.InUse("Home", 1), awaitItem())

            viewModel.delete(TestData.office)
            assertEquals(PlacesMessage.Deleted("Office"), awaitItem())
        }
    }
}
