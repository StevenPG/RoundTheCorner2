package com.stevenpg.roundthecorner2.feature.triggers

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.stevenpg.roundthecorner2.feature.triggers.editor.TriggerEditorRoute
import com.stevenpg.roundthecorner2.feature.triggers.list.TriggersRoute
import kotlinx.serialization.Serializable

/** The Triggers tab (start destination). */
@Serializable
data object TriggersNavKey : NavKey

/** Create (triggerId = null) or edit a trigger. */
@Serializable
data class TriggerEditorNavKey(val triggerId: Long? = null) : NavKey

/**
 * Registers the Triggers destinations.
 *
 * @param onCreatePlace / [onCreateRecipient] open the other features' editors
 *   (the app module supplies their keys, so features stay independent).
 */
fun EntryProviderScope<NavKey>.triggersEntries(
    navigate: (NavKey) -> Unit,
    goBack: () -> Unit,
    onCreatePlace: () -> Unit,
    onCreateRecipient: () -> Unit,
) {
    entry<TriggersNavKey> {
        TriggersRoute(
            onAddTrigger = { navigate(TriggerEditorNavKey()) },
            onEditTrigger = { id -> navigate(TriggerEditorNavKey(id.value)) },
        )
    }
    entry<TriggerEditorNavKey> { key ->
        TriggerEditorRoute(
            triggerId = key.triggerId,
            onClose = goBack,
            onCreatePlace = onCreatePlace,
            onCreateRecipient = onCreateRecipient,
        )
    }
}
