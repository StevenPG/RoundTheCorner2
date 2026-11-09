package com.stevenpg.roundthecorner2.core.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import com.stevenpg.roundthecorner2.core.model.ThemePreference
import com.stevenpg.roundthecorner2.core.model.UnitSystem
import com.stevenpg.roundthecorner2.core.model.UserPreferences
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Exercises the real DataStore against a temp file - no Android runtime needed. */
class DataStoreUserPreferencesRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val testScope = TestScope(UnconfinedTestDispatcher())

    private fun repository() = DataStoreUserPreferencesRepository(
        PreferenceDataStoreFactory.create(
            scope = testScope.backgroundScope,
            produceFile = { tmp.newFile("prefs.preferences_pb").also { it.delete() } },
        ),
    )

    @Test
    fun `fresh install emits defaults`() = testScope.runTest {
        repository().preferences.test {
            assertEquals(UserPreferences(), awaitItem())
        }
    }

    @Test
    fun `writes are reflected in the flow`() = testScope.runTest {
        val repo = repository()
        repo.preferences.test {
            assertEquals(UnitSystem.SYSTEM, awaitItem().unitSystem)

            repo.setUnitSystem(UnitSystem.METRIC)
            assertEquals(UnitSystem.METRIC, awaitItem().unitSystem)

            repo.setThemePreference(ThemePreference.DARK)
            assertEquals(ThemePreference.DARK, awaitItem().themePreference)

            repo.setUseDynamicColor(true)
            assertEquals(true, awaitItem().useDynamicColor)
        }
    }
}
