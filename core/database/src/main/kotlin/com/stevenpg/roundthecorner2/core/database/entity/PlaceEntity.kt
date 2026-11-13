package com.stevenpg.roundthecorner2.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.stevenpg.roundthecorner2.core.model.Coordinates
import com.stevenpg.roundthecorner2.core.model.Place
import com.stevenpg.roundthecorner2.core.model.PlaceIcon
import com.stevenpg.roundthecorner2.core.model.PlaceId
import java.time.Instant

/**
 * `places` table. Entities are persistence-only shapes (like JPA @Entity);
 * the rest of the app only sees [Place] via [asExternalModel].
 */
@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: PlaceIcon, // Room stores enums by name.
    val latitude: Double,
    val longitude: Double,
    val address: String?,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
)

fun PlaceEntity.asExternalModel() = Place(
    id = PlaceId(id),
    name = name,
    icon = icon,
    coordinates = Coordinates(latitude, longitude),
    address = address,
    createdAt = createdAt,
)
