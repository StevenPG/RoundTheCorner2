package com.stevenpg.roundthecorner2.core.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stevenpg.roundthecorner2.core.model.PlaceIcon

/** The vector drawn for each [PlaceIcon]. Exhaustive `when`: a new enum entry won't compile until mapped. */
val PlaceIcon.imageVector: ImageVector
    get() = when (this) {
        PlaceIcon.PIN -> Icons.Outlined.Place
        PlaceIcon.HOME -> Icons.Outlined.Home
        PlaceIcon.WORK -> Icons.Outlined.Work
        PlaceIcon.SCHOOL -> Icons.Outlined.School
        PlaceIcon.FAMILY -> Icons.Outlined.FamilyRestroom
        PlaceIcon.FRIEND -> Icons.Outlined.People
        PlaceIcon.GYM -> Icons.Outlined.FitnessCenter
        PlaceIcon.SHOP -> Icons.Outlined.ShoppingBag
        PlaceIcon.RESTAURANT -> Icons.Outlined.Restaurant
        PlaceIcon.TRAVEL -> Icons.Outlined.Flight
    }

/** Short, translatable name for each icon (also its accessibility label). */
@get:StringRes
val PlaceIcon.label: Int
    get() = when (this) {
        PlaceIcon.PIN -> R.string.place_icon_pin
        PlaceIcon.HOME -> R.string.place_icon_home
        PlaceIcon.WORK -> R.string.place_icon_work
        PlaceIcon.SCHOOL -> R.string.place_icon_school
        PlaceIcon.FAMILY -> R.string.place_icon_family
        PlaceIcon.FRIEND -> R.string.place_icon_friend
        PlaceIcon.GYM -> R.string.place_icon_gym
        PlaceIcon.SHOP -> R.string.place_icon_shop
        PlaceIcon.RESTAURANT -> R.string.place_icon_restaurant
        PlaceIcon.TRAVEL -> R.string.place_icon_travel
    }

/** A place's icon in a tonal circle - the standard leading visual for place rows. */
@Composable
fun PlaceIconBadge(
    icon: PlaceIcon,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) = IconBadge(icon.imageVector, modifier, size, containerColor, contentColor)

/** Any icon centred in a filled circle. */
@Composable
fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(containerColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(size * 0.5f))
    }
}
