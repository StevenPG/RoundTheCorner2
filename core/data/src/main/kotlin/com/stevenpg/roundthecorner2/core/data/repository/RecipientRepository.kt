package com.stevenpg.roundthecorner2.core.data.repository

import com.stevenpg.roundthecorner2.core.domain.validation.RecipientValidation
import com.stevenpg.roundthecorner2.core.model.Recipient
import com.stevenpg.roundthecorner2.core.model.RecipientDestination
import com.stevenpg.roundthecorner2.core.model.RecipientId
import kotlinx.coroutines.flow.Flow

/** Saved message recipients (SMS numbers, Slack/Discord webhooks). */
interface RecipientRepository {
    fun observeRecipients(): Flow<List<Recipient>>
    suspend fun getRecipient(id: RecipientId): Recipient?
    suspend fun createRecipient(draft: RecipientDraft): RecipientId
    suspend fun updateRecipient(id: RecipientId, draft: RecipientDraft): Boolean
    suspend fun deleteRecipient(id: RecipientId): DeleteResult
}

/** User-editable fields of a recipient. Invalid destinations are rejected at construction. */
data class RecipientDraft(
    val name: String,
    val destination: RecipientDestination,
) {
    init {
        require(name.isNotBlank()) { "Recipient name must not be blank" }
        val problem = RecipientValidation.validate(destination)
        require(problem == null) { "Invalid ${destination.channel} destination: $problem" }
    }
}
