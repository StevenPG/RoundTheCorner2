package com.stevenpg.roundthecorner2.feature.settings

import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.stevenpg.roundthecorner2.core.model.ResolvedUnitSystem
import com.stevenpg.roundthecorner2.core.model.ThemePreference
import com.stevenpg.roundthecorner2.core.model.UnitSystem
import com.stevenpg.roundthecorner2.core.testing.FakeUserPreferencesRepository
import com.stevenpg.roundthecorner2.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Robolectric only supplies a real Context (for the version lookup). */
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeUserPreferencesRepository()

    // Lazy: created after MainDispatcherRule installs the test Main dispatcher.
    private val viewModel by lazy { SettingsViewModel(
        preferencesRepository = repository,
        localeUnitSystemProvider = { ResolvedUnitSystem.METRIC },
        context = ApplicationProvider.getApplicationContext(),
    ) }

    @Test
    fun `state reflects repository and locale default`() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.uiState.test {
            val ready = awaitReady()
            assertEquals(UnitSystem.SYSTEM, ready.preferences.unitSystem)
            assertEquals(ResolvedUnitSystem.METRIC, ready.localeUnitSystem)
        }
    }

    @Test
    fun `user edits are persisted and re-emitted`() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.uiState.test {
            awaitReady()

            viewModel.onUnitSystemSelected(UnitSystem.IMPERIAL)
            assertEquals(UnitSystem.IMPERIAL, awaitReady().preferences.unitSystem)

            viewModel.onThemeSelected(ThemePreference.DARK)
            assertEquals(ThemePreference.DARK, awaitReady().preferences.themePreference)

            viewModel.onDynamicColorToggled(true)
            assertTrue(awaitReady().preferences.useDynamicColor)
        }
    }

    /** Skips the initial Loading emission if present. */
    private suspend fun app.cash.turbine.ReceiveTurbine<SettingsUiState>.awaitReady(): SettingsUiState.Ready {
        var item = awaitItem()
        while (item !is SettingsUiState.Ready) item = awaitItem()
        return item
    }
}
