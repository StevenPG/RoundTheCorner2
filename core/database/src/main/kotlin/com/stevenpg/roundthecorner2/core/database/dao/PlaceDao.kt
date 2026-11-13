package com.stevenpg.roundthecorner2.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.stevenpg.roundthecorner2.core.database.entity.PlaceEntity
import kotlinx.coroutines.flow.Flow

/** Data access for saved places. `Flow` queries re-emit whenever the table changes. */
@Dao
interface PlaceDao {

    @Query("SELECT * FROM places ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<PlaceEntity>>

    @Query("SELECT * FROM places WHERE id = :id")
    fun observeById(id: Long): Flow<PlaceEntity?>

    @Query("SELECT * FROM places WHERE id = :id")
    suspend fun getById(id: Long): PlaceEntity?

    /** @return the new row id. */
    @Insert
    suspend fun insert(place: PlaceEntity): Long

    /** @return number of rows updated (0 if the id doesn't exist). */
    @Update
    suspend fun update(place: PlaceEntity): Int

    @Query("DELETE FROM places WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    /** How many triggers reference this place (blocks deletion when > 0). */
    @Query("SELECT COUNT(*) FROM triggers WHERE place_id = :placeId")
    suspend fun countTriggersUsing(placeId: Long): Int
}
