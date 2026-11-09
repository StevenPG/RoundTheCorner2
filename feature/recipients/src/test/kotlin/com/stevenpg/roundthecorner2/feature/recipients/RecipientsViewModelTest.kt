package com.stevenpg.roundthecorner2.feature.recipients

import app.cash.turbine.test
import com.stevenpg.roundthecorner2.core.testing.FakeRecipientRepository
import com.stevenpg.roundthecorner2.core.testing.MainDispatcherRule
import com.stevenpg.roundthecorner2.feature.recipients.list.RecipientsMessage
import com.stevenpg.roundthecorner2.feature.recipients.list.RecipientsUiState
import com.stevenpg.roundthecorner2.feature.recipients.list.RecipientsViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RecipientsViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val repository = FakeRecipientRepository(listOf(TestData.family, TestData.alex))
    private val viewModel = RecipientsViewModel(repository)

    @Test
    fun `lists alphabetically and reports delete outcomes`() = runTest {
        viewModel.uiState.test {
            var state = awaitItem()
            if (state is RecipientsUiState.Loading) state = awaitItem()
            assertEquals(listOf("#family", "Alex"), (state as RecipientsUiState.Ready).recipients.map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
        repository.triggerUsage[TestData.alex.id] = 1
        viewModel.messages.test {
            viewModel.delete(TestData.alex)
            assertEquals(RecipientsMessage.InUse("Alex", 1), awaitItem())
            viewModel.delete(TestData.family)
            assertEquals(RecipientsMessage.Deleted("#family"), awaitItem())
        }
    }
}
