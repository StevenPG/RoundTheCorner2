package com.stevenpg.roundthecorner2.feature.recipients.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stevenpg.roundthecorner2.core.data.repository.DeleteResult
import com.stevenpg.roundthecorner2.core.data.repository.RecipientRepository
import com.stevenpg.roundthecorner2.core.model.Recipient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RecipientsUiState {
    data object Loading : RecipientsUiState
    data class Ready(val recipients: List<Recipient>) : RecipientsUiState
}

sealed interface RecipientsMessage {
    data class Deleted(val name: String) : RecipientsMessage
    data class InUse(val name: String, val triggerCount: Int) : RecipientsMessage
}

@HiltViewModel
class RecipientsViewModel @Inject constructor(
    private val repository: RecipientRepository,
) : ViewModel() {

    val uiState: StateFlow<RecipientsUiState> = repository.observeRecipients()
        .map<List<Recipient>, RecipientsUiState>(RecipientsUiState::Ready)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipientsUiState.Loading)

    private val _messages = Channel<RecipientsMessage>(Channel.BUFFERED)
    val messages: Flow<RecipientsMessage> = _messages.receiveAsFlow()

    fun delete(recipient: Recipient) {
        viewModelScope.launch {
            when (val result = repository.deleteRecipient(recipient.id)) {
                DeleteResult.Deleted -> _messages.send(RecipientsMessage.Deleted(recipient.name))
                is DeleteResult.InUse -> _messages.send(RecipientsMessage.InUse(recipient.name, result.triggerCount))
                DeleteResult.NotFound -> Unit
            }
        }
    }
}
