package app.cityxplore.core

import platform.Foundation.NSProcessInfo

/**
 * iOS implementation of debug configuration.
 * Returns true if running in debug mode (simulator or debug build).
 */
actual val isDebugBuild: Boolean = NSProcessInfo.processInfo.environment["DEBUG_MODE"] == "1"
