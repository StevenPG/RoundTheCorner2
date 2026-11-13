package com.stevenpg.roundthecorner2.feature.recipients

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.stevenpg.roundthecorner2.feature.recipients.editor.RecipientEditorRoute
import com.stevenpg.roundthecorner2.feature.recipients.list.RecipientsRoute
import kotlinx.serialization.Serializable

/** The Recipients tab. */
@Serializable
data object RecipientsNavKey : NavKey

/** Create (recipientId = null) or edit a recipient. */
@Serializable
data class RecipientEditorNavKey(val recipientId: Long? = null) : NavKey

fun EntryProviderScope<NavKey>.recipientsEntries(
    navigate: (NavKey) -> Unit,
    goBack: () -> Unit,
) {
    entry<RecipientsNavKey> {
        RecipientsRoute(
            onAddRecipient = { navigate(RecipientEditorNavKey()) },
            onEditRecipient = { id -> navigate(RecipientEditorNavKey(id.value)) },
        )
    }
    entry<RecipientEditorNavKey> { key ->
        RecipientEditorRoute(recipientId = key.recipientId, onClose = goBack)
    }
}
