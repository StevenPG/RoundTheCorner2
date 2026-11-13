package com.stevenpg.roundthecorner2.feature.recipients.editor

import com.stevenpg.roundthecorner2.core.domain.validation.RecipientValidation
import com.stevenpg.roundthecorner2.core.messaging.SendResult
import com.stevenpg.roundthecorner2.core.messaging.SmsMode
import com.stevenpg.roundthecorner2.core.model.MessageChannel
import com.stevenpg.roundthecorner2.core.model.RecipientDestination

data class RecipientEditorUiState(
    val isNew: Boolean,
    val smsMode: SmsMode,
    val isLoading: Boolean = false,
    val name: String = "",
    val channel: MessageChannel = MessageChannel.SMS,
    /**
     * One input per channel, so flipping between SMS/Slack/Discord while
     * deciding doesn't wipe what was typed.
     */
    val addresses: Map<MessageChannel, String> = emptyMap(),
    val needsReentry: Boolean = false,
    val isSendingTest: Boolean = false,
    val isSaving: Boolean = false,
) {
    val address: String get() = addresses[channel].orEmpty()

    val destination: RecipientDestination get() = RecipientDestination.of(channel, address.trim())

    /** Shown under the field only once something has been typed. */
    val problem: RecipientValidation.Problem?
        get() = RecipientValidation.validate(destination).takeIf { address.isNotBlank() }

    private val isValid: Boolean get() = RecipientValidation.validate(destination) == null

    val canSave: Boolean get() = !isLoading && !isSaving && name.isNotBlank() && isValid
    val canSendTest: Boolean get() = !isLoading && !isSendingTest && isValid
}

sealed interface RecipientEditorAction {
    data class NameChanged(val name: String) : RecipientEditorAction
    data class ChannelSelected(val channel: MessageChannel) : RecipientEditorAction
    data class AddressChanged(val address: String) : RecipientEditorAction

    /** From the contact picker: fills the number, and the name if still empty. */
    data class ContactPicked(val displayName: String?, val phoneNumber: String) : RecipientEditorAction

    /** [message] is passed in (already localised) by the screen. */
    data class SendTest(val message: String) : RecipientEditorAction
    data object Save : RecipientEditorAction
    data object Delete : RecipientEditorAction
}

sealed interface RecipientEditorEvent {
    data object Saved : RecipientEditorEvent
    data object Deleted : RecipientEditorEvent
    data class InUse(val triggerCount: Int) : RecipientEditorEvent
    data class TestFinished(val result: SendResult) : RecipientEditorEvent
}
