package com.stevenpg.roundthecorner2.core.location.di

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.stevenpg.roundthecorner2.core.location.FusedLocationClient
import com.stevenpg.roundthecorner2.core.location.LocationClient
import com.stevenpg.roundthecorner2.core.location.PlatformLocationClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider

@Module
@InstallIn(SingletonComponent::class)
internal object LocationModule {

    /** Fused provider when Google Play services is usable, platform LocationManager otherwise. */
    @Provides
    fun provideLocationClient(
        @ApplicationContext context: Context,
        fused: Provider<FusedLocationClient>,
        platform: Provider<PlatformLocationClient>,
    ): LocationClient {
        val hasPlayServices = runCatching {
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
        }.getOrDefault(false)
        return if (hasPlayServices) fused.get() else platform.get()
    }
}
