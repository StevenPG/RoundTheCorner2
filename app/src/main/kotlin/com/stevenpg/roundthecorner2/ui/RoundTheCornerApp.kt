package com.stevenpg.roundthecorner2.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.stevenpg.roundthecorner2.feature.places.placesEntries
import com.stevenpg.roundthecorner2.feature.recipients.recipientsEntries
import com.stevenpg.roundthecorner2.feature.settings.settingsEntry
import com.stevenpg.roundthecorner2.feature.triggers.triggersEntries
import com.stevenpg.roundthecorner2.navigation.AppNavigationState
import com.stevenpg.roundthecorner2.navigation.TopLevelDestination
import com.stevenpg.roundthecorner2.navigation.rememberAppNavigationState

/**
 * Root composable: adaptive navigation chrome + the current tab's back stack.
 *
 * `NavigationSuiteScaffold` picks the right navigation component for the
 * window size automatically - a bottom bar on phones, a side rail on
 * tablets / foldables / landscape.
 */
@Composable
fun RoundTheCornerApp(
    modifier: Modifier = Modifier,
    navigationState: AppNavigationState = rememberAppNavigationState(),
) {
    // At a non-start tab's root, Back goes to the start tab before exiting.
    BackHandler(enabled = navigationState.shouldReturnToStartTab) { navigationState.goBack() }

    // Detail screens (editors) are full-screen: hide the bar/rail while one is open.
    val adaptiveLayout = NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfoV2())
    val layoutType = if (navigationState.isAtTabRoot) adaptiveLayout else NavigationSuiteType.None

    NavigationSuiteScaffold(
        modifier = modifier,
        layoutType = layoutType,
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                val selected = destination == navigationState.currentDestination
                item(
                    selected = selected,
                    onClick = { navigationState.selectTab(destination) },
                    icon = {
                        Icon(
                            imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                            contentDescription = null, // The label below is read instead.
                        )
                    },
                    label = { Text(stringResource(destination.label)) },
                )
            }
        },
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            navigationRailContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        NavDisplay(
            backStack = navigationState.currentBackStack,
            onBack = navigationState::goBack,
            entryDecorators = listOf(
                // Keeps rememberSaveable state per destination.
                rememberSaveableStateHolderNavEntryDecorator(),
                // Scopes hiltViewModel() to the destination, cleared when popped.
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                // TODO(milestone 5): navigate to the trigger editor.
                triggersEntries(onAddTrigger = {})
                placesEntries(navigate = navigationState::navigate, goBack = navigationState::goBack)
                recipientsEntries(navigate = navigationState::navigate, goBack = navigationState::goBack)
                settingsEntry()
            },
        )
    }
}
