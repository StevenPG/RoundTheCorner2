package com.stevenpg.roundthecorner2.core.testing

import com.stevenpg.roundthecorner2.core.data.repository.DeleteResult
import com.stevenpg.roundthecorner2.core.data.repository.RecipientDraft
import com.stevenpg.roundthecorner2.core.data.repository.RecipientRepository
import com.stevenpg.roundthecorner2.core.model.Recipient
import com.stevenpg.roundthecorner2.core.model.RecipientId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Instant

/** In-memory [RecipientRepository]; [triggerUsage] simulates recipients referenced by triggers. */
class FakeRecipientRepository(initial: List<Recipient> = emptyList()) : RecipientRepository {

    private val recipients = MutableStateFlow(initial.associateBy { it.id })
    private var nextId = (initial.maxOfOrNull { it.id.value } ?: 0) + 1
    val triggerUsage = mutableMapOf<RecipientId, Int>()

    override fun observeRecipients(): Flow<List<Recipient>> =
        recipients.map { it.values.sortedBy { r -> r.name.lowercase() } }

    override suspend fun getRecipient(id: RecipientId): Recipient? = recipients.value[id]

    override suspend fun createRecipient(draft: RecipientDraft): RecipientId {
        val id = RecipientId(nextId++)
        recipients.update { it + (id to Recipient(id, draft.name.trim(), draft.destination, Instant.EPOCH)) }
        return id
    }

    override suspend fun updateRecipient(id: RecipientId, draft: RecipientDraft): Boolean {
        val existing = recipients.value[id] ?: return false
        recipients.update { it + (id to existing.copy(name = draft.name.trim(), destination = draft.destination, needsReentry = false)) }
        return true
    }

    override suspend fun deleteRecipient(id: RecipientId): DeleteResult {
        triggerUsage[id]?.takeIf { it > 0 }?.let { return DeleteResult.InUse(it) }
        if (recipients.value[id] == null) return DeleteResult.NotFound
        recipients.update { it - id }
        return DeleteResult.Deleted
    }
}
