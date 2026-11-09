package com.stevenpg.roundthecorner2.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.stevenpg.roundthecorner2.core.database.entity.TriggerEntity
import com.stevenpg.roundthecorner2.core.database.entity.TriggerWithRelations
import com.stevenpg.roundthecorner2.core.model.TriggerStatus
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** Data access for triggers. */
@Dao
interface TriggerDao {

    /**
     * Every trigger with its place and recipient, armed first, then newest
     * first. `@Transaction` makes the three underlying queries consistent.
     */
    @Transaction
    @Query(
        """
        SELECT * FROM triggers
        ORDER BY CASE status WHEN 'ARMED' THEN 0 WHEN 'PAUSED' THEN 1 ELSE 2 END, created_at DESC
        """,
    )
    fun observeAllWithRelations(): Flow<List<TriggerWithRelations>>

    /** What the monitoring service watches. */
    @Transaction
    @Query("SELECT * FROM triggers WHERE status = :status ORDER BY created_at")
    fun observeByStatusWithRelations(status: TriggerStatus): Flow<List<TriggerWithRelations>>

    @Transaction
    @Query("SELECT * FROM triggers WHERE id = :id")
    fun observeByIdWithRelations(id: Long): Flow<TriggerWithRelations?>

    @Query("SELECT * FROM triggers WHERE id = :id")
    suspend fun getById(id: Long): TriggerEntity?

    @Insert
    suspend fun insert(trigger: TriggerEntity): Long

    @Update
    suspend fun update(trigger: TriggerEntity): Int

    @Query("UPDATE triggers SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: TriggerStatus): Int

    /**
     * Marks an ARMED trigger as FIRED. The status guard makes this a
     * compare-and-set: if two location updates race, only one call returns 1,
     * so the message is sent at most once.
     */
    @Query("UPDATE triggers SET status = 'FIRED', last_fired_at = :firedAt WHERE id = :id AND status = 'ARMED'")
    suspend fun markFiredIfArmed(id: Long, firedAt: Instant): Int

    @Query("DELETE FROM triggers WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
