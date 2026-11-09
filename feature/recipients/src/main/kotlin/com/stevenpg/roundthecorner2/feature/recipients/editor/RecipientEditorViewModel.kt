package com.stevenpg.roundthecorner2.feature.recipients.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stevenpg.roundthecorner2.core.data.repository.DeleteResult
import com.stevenpg.roundthecorner2.core.data.repository.RecipientDraft
import com.stevenpg.roundthecorner2.core.data.repository.RecipientRepository
import com.stevenpg.roundthecorner2.core.messaging.MessageDispatcher
import com.stevenpg.roundthecorner2.core.messaging.SmsMode
import com.stevenpg.roundthecorner2.core.model.RecipientId
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Create/edit a recipient and send a test message to the *unsaved* details, so
 * the user can verify a webhook before relying on it.
 */
@HiltViewModel(assistedFactory = RecipientEditorViewModel.Factory::class)
class RecipientEditorViewModel @AssistedInject constructor(
    @Assisted private val recipientId: Long?,
    private val repository: RecipientRepository,
    private val dispatcher: MessageDispatcher,
    smsMode: SmsMode,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(recipientId: Long?): RecipientEditorViewModel
    }

    private val _uiState = MutableStateFlow(
        RecipientEditorUiState(isNew = recipientId == null, smsMode = smsMode, isLoading = recipientId != null),
    )
    val uiState: StateFlow<RecipientEditorUiState> = _uiState.asStateFlow()

    private val _events = Channel<RecipientEditorEvent>(Channel.BUFFERED)
    val events: Flow<RecipientEditorEvent> = _events.receiveAsFlow()

    init {
        if (recipientId != null) {
            viewModelScope.launch {
                val recipient = repository.getRecipient(RecipientId(recipientId))
                if (recipient == null) {
                    _events.send(RecipientEditorEvent.Deleted)
                    return@launch
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        name = recipient.name,
                        channel = recipient.destination.channel,
                        addresses = mapOf(recipient.destination.channel to recipient.destination.address),
                        needsReentry = recipient.needsReentry,
                    )
                }
            }
        }
    }

    fun onAction(action: RecipientEditorAction) {
        when (action) {
            is RecipientEditorAction.NameChanged -> _uiState.update { it.copy(name = action.name.take(MAX_NAME)) }
            is RecipientEditorAction.ChannelSelected -> _uiState.update { it.copy(channel = action.channel) }
            is RecipientEditorAction.AddressChanged -> _uiState.update {
                it.copy(addresses = it.addresses + (it.channel to action.address))
            }
            is RecipientEditorAction.ContactPicked -> _uiState.update {
                it.copy(
                    addresses = it.addresses + (it.channel to action.phoneNumber),
                    name = it.name.ifBlank { action.displayName.orEmpty().take(MAX_NAME) },
                )
            }
            is RecipientEditorAction.SendTest -> sendTest(action.message)
            RecipientEditorAction.Save -> save()
            RecipientEditorAction.Delete -> delete()
        }
    }

    private fun sendTest(message: String) {
        val state = _uiState.value
        if (!state.canSendTest) return
        _uiState.update { it.copy(isSendingTest = true) }
        viewModelScope.launch {
            // interactive = true: the user is looking at the app, so a Play-build
            // SMS opens the composer directly rather than via a notification.
            val result = dispatcher.send(state.destination, message, interactive = true)
            _uiState.update { it.copy(isSendingTest = false) }
            _events.send(RecipientEditorEvent.TestFinished(result))
        }
    }

    private fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val draft = RecipientDraft(state.name.trim(), state.destination)
            if (recipientId == null) {
                repository.createRecipient(draft)
            } else {
                repository.updateRecipient(RecipientId(recipientId), draft)
            }
            _events.send(RecipientEditorEvent.Saved)
        }
    }

    private fun delete() {
        val id = recipientId ?: return
        viewModelScope.launch {
            when (val result = repository.deleteRecipient(RecipientId(id))) {
                DeleteResult.Deleted, DeleteResult.NotFound -> _events.send(RecipientEditorEvent.Deleted)
                is DeleteResult.InUse -> _events.send(RecipientEditorEvent.InUse(result.triggerCount))
            }
        }
    }

    private companion object {
        const val MAX_NAME = 60
    }
}
