package com.stevenpg.roundthecorner2.core.network.di

import android.content.Context
import com.stevenpg.roundthecorner2.core.network.nominatim.NominatimClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {

    /**
     * One shared OkHttp client (shared connection pool and threads). Every
     * request identifies the app, as the OSM services require.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        val version = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "dev"
        val userAgent = "RoundTheCorner2/$version (Android; +https://github.com/StevenPG/RoundTheCorner2)"
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().header("User-Agent", userAgent).build())
            }
            .build()
    }

    @Provides
    @Named(NominatimClient.BASE_URL_NAME)
    fun provideNominatimBaseUrl(): String = NominatimClient.DEFAULT_BASE_URL
}
