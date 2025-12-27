package app.cityxplore.core

import app.cityxplore.BuildConfig

/**
 * Android implementation of debug configuration.
 * Uses BuildConfig.DEBUG from the com.github.gmazzo.buildconfig plugin.
 */
actual val isDebugBuild: Boolean = BuildConfig.DEBUG
