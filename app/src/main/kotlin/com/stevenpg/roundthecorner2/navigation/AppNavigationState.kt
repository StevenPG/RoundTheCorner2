package com.stevenpg.roundthecorner2.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack

/**
 * Navigation state for a bottom-tab app using Navigation 3.
 *
 * Navigation 3 models the back stack as a plain observable list that *we*
 * own (instead of an opaque NavController). Here each tab keeps its own list,
 * so switching tabs and coming back preserves where you were in that tab.
 * All stacks and the selected tab survive rotation and process death.
 */
@Stable
class AppNavigationState internal constructor(
    private val stacks: Map<TopLevelDestination, NavBackStack<NavKey>>,
    private val selected: MutableState<TopLevelDestination>,
) {
    /** Currently selected tab. */
    val currentDestination: TopLevelDestination by selected

    /** Back stack of the selected tab - what NavDisplay renders. */
    val currentBackStack: NavBackStack<NavKey>
        get() = stacks.getValue(currentDestination)

    /** True when system Back should switch to the start tab instead of exiting. */
    val shouldReturnToStartTab: Boolean
        get() = currentDestination != TopLevelDestination.START && currentBackStack.size <= 1

    /** Select a tab. Re-selecting the current tab pops it back to its root. */
    fun selectTab(destination: TopLevelDestination) {
        if (destination == currentDestination) {
            val stack = currentBackStack
            while (stack.size > 1) stack.removeAt(stack.lastIndex)
        } else {
            selected.value = destination
        }
    }

    /** Push a destination onto the current tab's stack. */
    fun navigate(key: NavKey) {
        currentBackStack.add(key)
    }

    /** Pop the current tab; at a tab root, fall back to the start tab. */
    fun goBack() {
        val stack = currentBackStack
        when {
            stack.size > 1 -> stack.removeAt(stack.lastIndex)
            shouldReturnToStartTab -> selected.value = TopLevelDestination.START
        }
    }
}

@Composable
fun rememberAppNavigationState(): AppNavigationState {
    // One saveable back stack per tab. The enum order is fixed, so calling
    // rememberNavBackStack in a loop is stable across recompositions.
    val stacks = TopLevelDestination.entries.associateWith { rememberNavBackStack(it.rootKey) }
    val selected = rememberSaveable { mutableStateOf(TopLevelDestination.START) }
    return remember(stacks, selected) { AppNavigationState(stacks, selected) }
}
