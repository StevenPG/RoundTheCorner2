package com.stevenpg.roundthecorner2.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure JVM tests of tab / back-stack behaviour - no Compose UI needed. */
class AppNavigationStateTest {

    @Serializable
    private data object Detail : NavKey

    private fun newState() = AppNavigationState(
        stacks = TopLevelDestination.entries.associateWith { NavBackStack(it.rootKey) },
        selected = mutableStateOf(TopLevelDestination.START),
    )

    @Test
    fun `starts on the start tab`() {
        val state = newState()
        assertEquals(TopLevelDestination.TRIGGERS, state.currentDestination)
        assertFalse(state.shouldReturnToStartTab)
    }

    @Test
    fun `each tab keeps its own back stack`() {
        val state = newState()
        state.selectTab(TopLevelDestination.PLACES)
        state.navigate(Detail)
        state.selectTab(TopLevelDestination.SETTINGS)
        state.selectTab(TopLevelDestination.PLACES)
        assertEquals(listOf(TopLevelDestination.PLACES.rootKey, Detail), state.currentBackStack.toList())
    }

    @Test
    fun `back pops within a tab, then returns to the start tab`() {
        val state = newState()
        state.selectTab(TopLevelDestination.PLACES)
        state.navigate(Detail)

        state.goBack()
        assertEquals(TopLevelDestination.PLACES, state.currentDestination)
        assertTrue(state.shouldReturnToStartTab)

        state.goBack()
        assertEquals(TopLevelDestination.TRIGGERS, state.currentDestination)
    }

    @Test
    fun `reselecting the current tab pops to its root`() {
        val state = newState()
        state.navigate(Detail)
        state.selectTab(TopLevelDestination.TRIGGERS)
        assertEquals(1, state.currentBackStack.size)
    }
}
