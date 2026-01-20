package app.cityxplore.database

import kotlin.time.Clock

/**
 * Utility function to get current time in milliseconds.
 * Used for timestamp fields in Room entities.
 */
fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
