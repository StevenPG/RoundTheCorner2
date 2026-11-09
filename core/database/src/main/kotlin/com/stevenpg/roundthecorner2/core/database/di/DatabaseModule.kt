package com.stevenpg.roundthecorner2.core.database.di

import android.content.Context
import androidx.room.Room
import com.stevenpg.roundthecorner2.core.database.RoundTheCornerDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RoundTheCornerDatabase =
        Room.databaseBuilder(context, RoundTheCornerDatabase::class.java, RoundTheCornerDatabase.NAME).build()

    // DAOs are cheap accessors on the singleton database; no scoping needed.
    @Provides fun providePlaceDao(db: RoundTheCornerDatabase) = db.placeDao()

    @Provides fun provideRecipientDao(db: RoundTheCornerDatabase) = db.recipientDao()

    @Provides fun provideTriggerDao(db: RoundTheCornerDatabase) = db.triggerDao()

    @Provides fun provideSendAttemptDao(db: RoundTheCornerDatabase) = db.sendAttemptDao()
}
