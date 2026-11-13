package com.stevenpg.roundthecorner2.core.monitoring.di

import com.stevenpg.roundthecorner2.core.monitoring.MonitoringController
import com.stevenpg.roundthecorner2.core.monitoring.MonitoringPermissions
import com.stevenpg.roundthecorner2.core.monitoring.MonitoringStarter
import com.stevenpg.roundthecorner2.core.monitoring.PermissionChecker
import com.stevenpg.roundthecorner2.core.monitoring.fire.RetryScheduler
import com.stevenpg.roundthecorner2.core.monitoring.fire.TriggerEventNotifier
import com.stevenpg.roundthecorner2.core.monitoring.fire.WorkManagerRetryScheduler
import com.stevenpg.roundthecorner2.core.monitoring.notification.AndroidTriggerEventNotifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class MonitoringModule {
    @Binds abstract fun bindNotifier(impl: AndroidTriggerEventNotifier): TriggerEventNotifier

    @Binds abstract fun bindRetryScheduler(impl: WorkManagerRetryScheduler): RetryScheduler

    @Binds abstract fun bindPermissionChecker(impl: MonitoringPermissions): PermissionChecker

    @Binds abstract fun bindMonitoringStarter(impl: MonitoringController): MonitoringStarter
}
