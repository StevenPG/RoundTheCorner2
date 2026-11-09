package com.stevenpg.roundthecorner2.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.stevenpg.roundthecorner2.core.database.dao.PlaceDao
import com.stevenpg.roundthecorner2.core.database.dao.RecipientDao
import com.stevenpg.roundthecorner2.core.database.dao.SendAttemptDao
import com.stevenpg.roundthecorner2.core.database.dao.TriggerDao
import com.stevenpg.roundthecorner2.core.database.entity.PlaceEntity
import com.stevenpg.roundthecorner2.core.database.entity.RecipientEntity
import com.stevenpg.roundthecorner2.core.database.entity.SendAttemptEntity
import com.stevenpg.roundthecorner2.core.database.entity.TriggerEntity

/**
 * The app's SQLite database.
 *
 * Schema changes: bump [version], add an `AutoMigration` (or a manual
 * `Migration`), build once to export `schemas/<version>.json`, and commit it.
 * Never edit an exported schema file.
 */
@Database(
    version = 1,
    entities = [
        PlaceEntity::class,
        RecipientEntity::class,
        TriggerEntity::class,
        SendAttemptEntity::class,
    ],
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class RoundTheCornerDatabase : RoomDatabase() {
    abstract fun placeDao(): PlaceDao
    abstract fun recipientDao(): RecipientDao
    abstract fun triggerDao(): TriggerDao
    abstract fun sendAttemptDao(): SendAttemptDao

    companion object {
        const val NAME = "round_the_corner.db"
    }
}
