package com.stevenpg.roundthecorner2.feature.triggers.editor

import androidx.compose.ui.text.input.TextFieldValue
import com.stevenpg.roundthecorner2.core.data.repository.TriggerDraft
import com.stevenpg.roundthecorner2.core.domain.format.RadiusInput
import com.stevenpg.roundthecorner2.core.domain.message.MessageTemplate
import com.stevenpg.roundthecorner2.core.model.Distance
import com.stevenpg.roundthecorner2.core.model.Place
import com.stevenpg.roundthecorner2.core.model.PlaceId
import com.stevenpg.roundthecorner2.core.model.Recipient
import com.stevenpg.roundthecorner2.core.model.RecipientId
import com.stevenpg.roundthecorner2.core.model.ResolvedUnitSystem
import com.stevenpg.roundthecorner2.core.model.SendAttempt
import com.stevenpg.roundthecorner2.core.model.TriggerDirection

data class TriggerEditorUiState(
    val isNew: Boolean,
    val isLoading: Boolean = true,
    val places: List<Place> = emptyList(),
    val recipients: List<Recipient> = emptyList(),
    val placeId: PlaceId? = null,
    val recipientId: RecipientId? = null,
    val direction: TriggerDirection = TriggerDirection.ARRIVE,
    val unitSystem: ResolvedUnitSystem = ResolvedUnitSystem.METRIC,
    /** Radius as typed, in miles or km depending on [unitSystem]. */
    val radiusText: String = "",
    /** TextFieldValue (not String) so placeholders can be inserted at the cursor. */
    val message: TextFieldValue = TextFieldValue(""),
    /** [message] with example values filled in. */
    val preview: String = "",
    /** Formatted limits for the distance error text, e.g. "25 m" / "80 ft". */
    val minRadiusText: String = "",
    val maxRadiusText: String = "",
    val history: List<SendAttempt> = emptyList(),
    val isSaving: Boolean = false,
) {
    val place: Place? get() = places.find { it.id == placeId }
    val recipient: Recipient? get() = recipients.find { it.id == recipientId }
    val radius: Distance? get() = RadiusInput.parse(radiusText, unitSystem)

    val radiusError: RadiusError?
        get() {
            val r = radius ?: return RadiusError.INVALID
            return when {
                r < TriggerDraft.MIN_RADIUS -> RadiusError.TOO_SMALL
                r > TriggerDraft.MAX_RADIUS -> RadiusError.TOO_LARGE
                else -> null
            }
        }

    val canSave: Boolean
        get() = !isLoading && !isSaving && place != null && recipient?.needsReentry == false &&
            radiusError == null && message.text.isNotBlank()
}

enum class RadiusError { INVALID, TOO_SMALL, TOO_LARGE }

/** Default message texts, localised by the screen and passed in. */
data class MessageDefaults(val arrive: String, val leave: String) {
    fun forDirection(direction: TriggerDirection) = if (direction == TriggerDirection.ARRIVE) arrive else leave
}

sealed interface TriggerEditorAction {
    data class PlaceSelected(val id: PlaceId) : TriggerEditorAction
    data class RecipientSelected(val id: RecipientId) : TriggerEditorAction
    data class DirectionSelected(val direction: TriggerDirection) : TriggerEditorAction
    data class RadiusTextChanged(val text: String) : TriggerEditorAction
    data class RadiusPresetSelected(val value: Double) : TriggerEditorAction
    data class MessageChanged(val value: TextFieldValue) : TriggerEditorAction
    data class InsertPlaceholder(val placeholder: MessageTemplate.Placeholder) : TriggerEditorAction
    data object Save : TriggerEditorAction
    data object Delete : TriggerEditorAction
}

sealed interface TriggerEditorEvent {
    data object Saved : TriggerEditorEvent
    data object Deleted : TriggerEditorEvent
}
