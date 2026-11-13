package com.stevenpg.roundthecorner2.core.data.repository

/** Outcome of deleting something other records may depend on. */
sealed interface DeleteResult {
    data object Deleted : DeleteResult
    data object NotFound : DeleteResult

    /** Still referenced by [triggerCount] triggers; delete or edit those first. */
    data class InUse(val triggerCount: Int) : DeleteResult
}
