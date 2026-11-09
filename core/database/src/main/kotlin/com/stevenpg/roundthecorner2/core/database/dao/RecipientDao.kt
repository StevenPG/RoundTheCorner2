package com.stevenpg.roundthecorner2.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.stevenpg.roundthecorner2.core.database.entity.RecipientEntity
import kotlinx.coroutines.flow.Flow

/** Data access for saved recipients. */
@Dao
interface RecipientDao {

    @Query("SELECT * FROM recipients ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<RecipientEntity>>

    @Query("SELECT * FROM recipients WHERE id = :id")
    suspend fun getById(id: Long): RecipientEntity?

    @Insert
    suspend fun insert(recipient: RecipientEntity): Long

    @Update
    suspend fun update(recipient: RecipientEntity): Int

    @Query("DELETE FROM recipients WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("SELECT COUNT(*) FROM triggers WHERE recipient_id = :recipientId")
    suspend fun countTriggersUsing(recipientId: Long): Int
}
