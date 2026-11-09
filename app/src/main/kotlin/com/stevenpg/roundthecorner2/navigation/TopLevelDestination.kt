package com.stevenpg.roundthecorner2.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.stevenpg.roundthecorner2.R
import com.stevenpg.roundthecorner2.feature.places.PlacesNavKey
import com.stevenpg.roundthecorner2.feature.recipients.RecipientsNavKey
import com.stevenpg.roundthecorner2.feature.settings.SettingsNavKey
import com.stevenpg.roundthecorner2.feature.triggers.TriggersNavKey

/**
 * The tabs in the bottom navigation bar (or rail, on wide screens). Each tab
 * owns its own back stack whose root is [rootKey].
 */
enum class TopLevelDestination(
    val rootKey: NavKey,
    @StringRes val label: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    TRIGGERS(TriggersNavKey, R.string.nav_triggers, Icons.Filled.NotificationsActive, Icons.Outlined.NotificationsActive),
    PLACES(PlacesNavKey, R.string.nav_places, Icons.Filled.Place, Icons.Outlined.Place),
    RECIPIENTS(RecipientsNavKey, R.string.nav_recipients, Icons.Filled.Group, Icons.Outlined.Group),
    SETTINGS(SettingsNavKey, R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings),
    ;

    companion object {
        /** The tab the app opens on, and where Back returns to before exiting. */
        val START = TRIGGERS
    }
}
