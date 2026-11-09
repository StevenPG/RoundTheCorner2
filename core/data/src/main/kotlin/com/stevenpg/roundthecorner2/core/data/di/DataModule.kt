package com.stevenpg.roundthecorner2.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.stevenpg.roundthecorner2.core.data.locale.IcuLocaleUnitSystemProvider
import com.stevenpg.roundthecorner2.core.data.locale.LocaleUnitSystemProvider
import com.stevenpg.roundthecorner2.core.data.preferences.DataStoreUserPreferencesRepository
import com.stevenpg.roundthecorner2.core.data.preferences.UserPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt bindings for the data layer. `@Binds` maps an interface to its
 * implementation (like `@Bean` returning an interface type in Spring).
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {

    @Binds
    abstract fun bindUserPreferencesRepository(impl: DataStoreUserPreferencesRepository): UserPreferencesRepository

    @Binds
    abstract fun bindLocaleUnitSystemProvider(impl: IcuLocaleUnitSystemProvider): LocaleUnitSystemProvider

    companion object {
        /** Exactly one DataStore per file per process - DataStore enforces this. */
        @Provides
        @Singleton
        fun provideUserPreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create(
                produceFile = { context.preferencesDataStoreFile("user_preferences") },
            )
    }
}
