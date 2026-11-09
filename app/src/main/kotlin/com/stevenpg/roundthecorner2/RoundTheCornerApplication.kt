package com.stevenpg.roundthecorner2

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. `@HiltAndroidApp` generates the root dependency
 * graph (the singleton component) - comparable to bootstrapping a Spring
 * ApplicationContext.
 */
@HiltAndroidApp
class RoundTheCornerApplication : Application()
