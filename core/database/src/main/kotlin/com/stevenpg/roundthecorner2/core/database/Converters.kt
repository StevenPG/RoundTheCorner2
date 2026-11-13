package com.stevenpg.roundthecorner2.core.database

import androidx.room.TypeConverter
import java.time.Instant

/** Column type mappings Room can't infer. Instants are stored as epoch millis. */
internal class Converters {
    @TypeConverter
    fun instantToEpochMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun epochMillisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)
}
