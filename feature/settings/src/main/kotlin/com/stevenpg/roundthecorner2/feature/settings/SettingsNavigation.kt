package com.stevenpg.roundthecorner2.feature.settings

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Navigation 3 destination key for the Settings screen. */
@Serializable
data object SettingsNavKey : NavKey

/** Registers the Settings destination with the app's navigation graph. */
fun EntryProviderScope<NavKey>.settingsEntry() {
    entry<SettingsNavKey> { SettingsRoute() }
}
