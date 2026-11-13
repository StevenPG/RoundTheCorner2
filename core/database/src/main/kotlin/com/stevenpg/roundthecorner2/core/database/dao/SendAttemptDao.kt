package com.stevenpg.roundthecorner2.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.stevenpg.roundthecorner2.core.database.entity.SendAttemptEntity
import kotlinx.coroutines.flow.Flow

/** Data access for delivery history. */
@Dao
interface SendAttemptDao {

    @Insert
    suspend fun insert(attempt: SendAttemptEntity): Long

    @Query("SELECT * FROM send_attempts WHERE trigger_id = :triggerId ORDER BY timestamp DESC")
    fun observeForTrigger(triggerId: Long): Flow<List<SendAttemptEntity>>

    @Query("SELECT * FROM send_attempts ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<SendAttemptEntity>>
}
