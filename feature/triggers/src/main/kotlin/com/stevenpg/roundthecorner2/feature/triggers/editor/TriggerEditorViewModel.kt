package com.stevenpg.roundthecorner2.feature.triggers.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stevenpg.roundthecorner2.core.data.preferences.UnitSystemProvider
import com.stevenpg.roundthecorner2.core.data.repository.PlaceRepository
import com.stevenpg.roundthecorner2.core.data.repository.RecipientRepository
import com.stevenpg.roundthecorner2.core.data.repository.SendHistoryRepository
import com.stevenpg.roundthecorner2.core.data.repository.TriggerDraft
import com.stevenpg.roundthecorner2.core.data.repository.TriggerRepository
import com.stevenpg.roundthecorner2.core.domain.format.DistanceFormatter
import com.stevenpg.roundthecorner2.core.domain.format.RadiusInput
import com.stevenpg.roundthecorner2.core.domain.message.MessageTemplate
import com.stevenpg.roundthecorner2.core.model.Place
import com.stevenpg.roundthecorner2.core.model.Recipient
import com.stevenpg.roundthecorner2.core.model.ResolvedUnitSystem
import com.stevenpg.roundthecorner2.core.model.TriggerId
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Create/edit a trigger: where (place + arrive/leave + distance), who
 * (recipient) and what (message template with a live preview).
 */
@HiltViewModel(assistedFactory = TriggerEditorViewModel.Factory::class)
class TriggerEditorViewModel @AssistedInject constructor(
    @Assisted private val triggerId: Long?,
    @Assisted private val defaults: MessageDefaults,
    private val triggerRepository: TriggerRepository,
    placeRepository: PlaceRepository,
    recipientRepository: RecipientRepository,
    historyRepository: SendHistoryRepository,
    unitSystemProvider: UnitSystemProvider,
    private val clock: Clock,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(triggerId: Long?, defaults: MessageDefaults): TriggerEditorViewModel
    }

    private val formatter = DistanceFormatter(Locale.getDefault())
    private val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

    private val _uiState = MutableStateFlow(TriggerEditorUiState(isNew = triggerId == null))
    val uiState: StateFlow<TriggerEditorUiState> = _uiState.asStateFlow()

    private val _events = Channel<TriggerEditorEvent>(Channel.BUFFERED)
    val events: Flow<TriggerEditorEvent> = _events.receiveAsFlow()

    /** True once the user typed in the message: stops us swapping in direction defaults. */
    private var messageEdited = triggerId != null

    init {
        viewModelScope.launch {
            var initialised = false
            combine(
                placeRepository.observePlaces(),
                recipientRepository.observeRecipients(),
                unitSystemProvider.unitSystem,
            ) { places, recipients, units -> Triple(places, recipients, units) }
                .collect { (places, recipients, units) ->
                    val before = _uiState.value
                    if (!initialised) {
                        initialised = true
                        initialise(places, recipients, units)
                    } else {
                        // A place/recipient created via "New place…" while editing: select it.
                        val newPlace = places.filter { p -> before.places.none { it.id == p.id } }.maxByOrNull { it.id.value }
                        val newRecipient =
                            recipients.filter { r -> before.recipients.none { it.id == r.id } }.maxByOrNull { it.id.value }
                        update {
                            it.copy(
                                places = places,
                                recipients = recipients,
                                placeId = newPlace?.id ?: it.placeId,
                                recipientId = newRecipient?.id ?: it.recipientId,
                            )
                        }
                    }
                }
        }
        if (triggerId != null) {
            viewModelScope.launch {
                historyRepository.observeForTrigger(TriggerId(triggerId)).collect { history ->
                    _uiState.update { it.copy(history = history) }
                }
            }
        }
    }

    private suspend fun initialise(places: List<Place>, recipients: List<Recipient>, units: ResolvedUnitSystem) {
        val limits = Pair(formatter.format(TriggerDraft.MIN_RADIUS, units), formatter.format(TriggerDraft.MAX_RADIUS, units))
        val existing = triggerId?.let { triggerRepository.observeTrigger(TriggerId(it)).first() }
        if (triggerId != null && existing == null) {
            _events.send(TriggerEditorEvent.Deleted) // Deleted elsewhere.
            return
        }
        update {
            if (existing != null) {
                val t = existing.trigger
                it.copy(
                    isLoading = false, places = places, recipients = recipients, unitSystem = units,
                    minRadiusText = limits.first, maxRadiusText = limits.second,
                    placeId = t.placeId, recipientId = t.recipientId, direction = t.direction,
                    radiusText = RadiusInput.format(t.radius, units),
                    message = TextFieldValue(t.messageTemplate, TextRange(t.messageTemplate.length)),
                )
            } else {
                val message = defaults.forDirection(it.direction)
                it.copy(
                    isLoading = false, places = places, recipients = recipients, unitSystem = units,
                    minRadiusText = limits.first, maxRadiusText = limits.second,
                    // Only one option? Pick it - one less tap.
                    placeId = places.singleOrNull()?.id,
                    recipientId = recipients.singleOrNull()?.id,
                    radiusText = RadiusInput.format(RadiusInput.toDistance(DEFAULT_RADIUS, units), units),
                    message = TextFieldValue(message, TextRange(message.length)),
                )
            }
        }
    }

    fun onAction(action: TriggerEditorAction) {
        when (action) {
            is TriggerEditorAction.PlaceSelected -> update { it.copy(placeId = action.id) }
            is TriggerEditorAction.RecipientSelected -> update { it.copy(recipientId = action.id) }
            is TriggerEditorAction.DirectionSelected -> update {
                val swapDefault = !messageEdited && it.message.text == defaults.forDirection(it.direction)
                val message = if (swapDefault) defaults.forDirection(action.direction).let { m -> TextFieldValue(m, TextRange(m.length)) } else it.message
                it.copy(direction = action.direction, message = message)
            }
            is TriggerEditorAction.RadiusTextChanged ->
                update { it.copy(radiusText = action.text.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(8)) }
            is TriggerEditorAction.RadiusPresetSelected ->
                update { it.copy(radiusText = RadiusInput.format(RadiusInput.toDistance(action.value, it.unitSystem), it.unitSystem)) }
            is TriggerEditorAction.MessageChanged -> {
                if (action.value.text != _uiState.value.message.text) messageEdited = true
                update { it.copy(message = action.value.copy(text = action.value.text.take(TriggerDraft.MAX_MESSAGE_LENGTH))) }
            }
            is TriggerEditorAction.InsertPlaceholder -> {
                messageEdited = true
                update { it.copy(message = it.message.insert("{${action.placeholder.token}}")) }
            }
            TriggerEditorAction.Save -> save()
            TriggerEditorAction.Delete -> delete()
        }
    }

    /** Applies [transform] and recomputes the message preview. */
    private fun update(transform: (TriggerEditorUiState) -> TriggerEditorUiState) {
        _uiState.update { state -> transform(state).let { it.copy(preview = preview(it)) } }
    }

    /** The message as the recipient would see it, using the radius as the example distance. */
    private fun preview(state: TriggerEditorUiState): String {
        if (state.isLoading) return ""
        val radius = state.radius
        val radiusText = radius?.let { RadiusInput.display(it, state.unitSystem) } ?: "–"
        val distanceText = radius?.let { formatter.format(it, state.unitSystem) } ?: "–"
        return MessageTemplate.render(
            state.message.text,
            MessageTemplate.Values(
                place = state.place?.name ?: "…",
                distance = distanceText, // At fire time, roughly the radius.
                radius = radiusText,
                time = LocalTime.now(clock.withZone(ZoneId.systemDefault())).format(timeFormatter),
            ),
        )
    }

    private fun save() {
        val state = _uiState.value
        val place = state.place
        val recipient = state.recipient
        val radius = state.radius
        if (!state.canSave || place == null || recipient == null || radius == null) return
        update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val draft = TriggerDraft(place.id, recipient.id, state.direction, radius, state.message.text.trim())
            if (triggerId == null) {
                triggerRepository.createTrigger(draft, armed = true)
            } else {
                triggerRepository.updateTrigger(TriggerId(triggerId), draft)
            }
            _events.send(TriggerEditorEvent.Saved)
        }
    }

    private fun delete() {
        val id = triggerId ?: return
        viewModelScope.launch {
            triggerRepository.deleteTrigger(TriggerId(id))
            _events.send(TriggerEditorEvent.Deleted)
        }
    }

    companion object {
        /** 0.5 mi or 0.5 km - a sensible "almost there" default. */
        const val DEFAULT_RADIUS = 0.5
    }
}

/** Replaces the selection (or inserts at the cursor) with [text], placing the cursor after it. */
internal fun TextFieldValue.insert(text: String): TextFieldValue {
    val start = minOf(selection.start, selection.end).coerceIn(0, this.text.length)
    val end = maxOf(selection.start, selection.end).coerceIn(0, this.text.length)
    val newText = this.text.replaceRange(start, end, text)
    return TextFieldValue(newText, TextRange(start + text.length))
}
