package com.stevenpg.roundthecorner2.feature.recipients

import app.cash.turbine.test
import com.stevenpg.roundthecorner2.core.domain.validation.RecipientValidation
import com.stevenpg.roundthecorner2.core.messaging.FailureReason
import com.stevenpg.roundthecorner2.core.messaging.SendResult
import com.stevenpg.roundthecorner2.core.messaging.SmsMode
import com.stevenpg.roundthecorner2.core.model.MessageChannel
import com.stevenpg.roundthecorner2.core.model.RecipientDestination
import com.stevenpg.roundthecorner2.core.testing.FakeMessageSender
import com.stevenpg.roundthecorner2.core.testing.FakeRecipientRepository
import com.stevenpg.roundthecorner2.core.testing.MainDispatcherRule
import com.stevenpg.roundthecorner2.core.testing.fakeDispatcher
import com.stevenpg.roundthecorner2.feature.recipients.editor.RecipientEditorAction
import com.stevenpg.roundthecorner2.feature.recipients.editor.RecipientEditorEvent
import com.stevenpg.roundthecorner2.feature.recipients.editor.RecipientEditorViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RecipientEditorViewModelTest {

    @get:Rule
    val main = MainDispatcherRule(StandardTestDispatcher())

    private val repository = FakeRecipientRepository(listOf(TestData.alex, TestData.restored))
    private val sender = FakeMessageSender()

    private fun editor(id: Long? = null) =
        RecipientEditorViewModel(id, repository, fakeDispatcher(sender), SmsMode.TAP_TO_SEND)

    private val RecipientEditorViewModel.state get() = uiState.value

    @Test
    fun `new SMS recipient validates the number and saves`() = runTest(main.testDispatcher) {
        val vm = editor()
        vm.onAction(RecipientEditorAction.NameChanged("Jordan"))
        vm.onAction(RecipientEditorAction.AddressChanged("call me"))
        assertEquals(RecipientValidation.Problem.INVALID_PHONE_NUMBER, vm.state.problem)
        assertFalse(vm.state.canSave)

        vm.onAction(RecipientEditorAction.AddressChanged("+1 555 555 0199"))
        assertNull(vm.state.problem)
        assertTrue(vm.state.canSave)

        vm.events.test {
            vm.onAction(RecipientEditorAction.Save)
            assertEquals(RecipientEditorEvent.Saved, awaitItem())
        }
        val saved = repository.observeRecipients().first().single { it.name == "Jordan" }
        assertEquals(RecipientDestination.Sms("+1 555 555 0199"), saved.destination)
    }

    @Test
    fun `switching channels keeps what was typed for each`() = runTest(main.testDispatcher) {
        val vm = editor()
        vm.onAction(RecipientEditorAction.AddressChanged("+1 555 555 0199"))
        vm.onAction(RecipientEditorAction.ChannelSelected(MessageChannel.SLACK))
        assertEquals("", vm.state.address)
        vm.onAction(RecipientEditorAction.AddressChanged(TestData.SLACK_URL))
        vm.onAction(RecipientEditorAction.ChannelSelected(MessageChannel.SMS))
        assertEquals("+1 555 555 0199", vm.state.address)
        vm.onAction(RecipientEditorAction.ChannelSelected(MessageChannel.SLACK))
        assertEquals(TestData.SLACK_URL, vm.state.address)
    }

    @Test
    fun `a Discord URL pasted under Slack is flagged`() = runTest(main.testDispatcher) {
        val vm = editor()
        vm.onAction(RecipientEditorAction.ChannelSelected(MessageChannel.SLACK))
        vm.onAction(RecipientEditorAction.AddressChanged(TestData.DISCORD_URL))
        assertEquals(RecipientValidation.Problem.NOT_A_SLACK_WEBHOOK, vm.state.problem)
        assertFalse(vm.state.canSendTest)
    }

    @Test
    fun `picked contact fills the number and an empty name only`() = runTest(main.testDispatcher) {
        val vm = editor()
        vm.onAction(RecipientEditorAction.ContactPicked("Jordan Lee", "555-0123"))
        assertEquals("Jordan Lee", vm.state.name)
        assertEquals("555-0123", vm.state.address)

        vm.onAction(RecipientEditorAction.ContactPicked("Someone Else", "555-0456"))
        assertEquals("Jordan Lee", vm.state.name)
        assertEquals("555-0456", vm.state.address)
    }

    @Test
    fun `test message goes to the unsaved details interactively and reports the result`() = runTest(main.testDispatcher) {
        sender.result = SendResult.Failed(FailureReason.DESTINATION_INVALID, "Slack rejected the webhook (HTTP 404)")
        val vm = editor()
        vm.onAction(RecipientEditorAction.ChannelSelected(MessageChannel.SLACK))
        vm.onAction(RecipientEditorAction.AddressChanged(TestData.SLACK_URL))

        vm.events.test {
            vm.onAction(RecipientEditorAction.SendTest("ping"))
            assertTrue(vm.state.isSendingTest)
            assertEquals(RecipientEditorEvent.TestFinished(sender.result), awaitItem())
        }
        assertFalse(vm.state.isSendingTest)
        val call = sender.calls.single()
        assertEquals(RecipientDestination.SlackWebhook(TestData.SLACK_URL), call.destination)
        assertEquals("ping", call.message)
        assertTrue(call.interactive)
        assertTrue("Nothing saved by a test", repository.observeRecipients().first().none { it.name.isBlank() })
    }

    @Test
    fun `editing a restored recipient shows the re-entry warning until fixed`() = runTest(main.testDispatcher) {
        val vm = editor(TestData.restored.id.value)
        advanceUntilIdle()
        assertTrue(vm.state.needsReentry)
        assertEquals("Mom", vm.state.name)
        assertFalse(vm.state.canSave)

        vm.onAction(RecipientEditorAction.AddressChanged("+1 555 555 0142"))
        vm.onAction(RecipientEditorAction.Save)
        advanceUntilIdle()
        assertFalse(repository.getRecipient(TestData.restored.id)!!.needsReentry)
    }

    @Test
    fun `deleting a recipient in use reports the count`() = runTest(main.testDispatcher) {
        repository.triggerUsage[TestData.alex.id] = 3
        val vm = editor(TestData.alex.id.value)
        advanceUntilIdle()
        vm.events.test {
            vm.onAction(RecipientEditorAction.Delete)
            assertEquals(RecipientEditorEvent.InUse(3), awaitItem())
        }
    }
}
