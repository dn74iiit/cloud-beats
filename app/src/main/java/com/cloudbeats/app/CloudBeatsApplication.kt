package com.cloudbeats.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * CloudBeats Application class.
 * Annotated with @HiltAndroidApp to trigger Hilt code generation
 * and provide the application-level dependency container.
 */
@HiltAndroidApp
class CloudBeatsApplication : Application()
