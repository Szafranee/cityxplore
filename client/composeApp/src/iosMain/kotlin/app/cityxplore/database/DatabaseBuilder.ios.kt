@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package app.cityxplore.database

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * iOS implementation of the database builder.
 *
 * Uses Room with BundledSQLiteDriver for iOS and stores the database
 * in the app's Documents directory.
 */
actual fun getDatabaseBuilder(): RoomDatabase.Builder<CityXploreDatabase> {
    val dbFilePath = documentDirectory() + "/${CityXploreDatabase.DATABASE_NAME}"
    return Room.databaseBuilder<CityXploreDatabase>(
        name = dbFilePath
    ).setDriver(BundledSQLiteDriver())
}

/**
 * Gets the path to the iOS Documents directory.
 * Creates the directory if it doesn't exist.
 */
private fun documentDirectory(): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,  // Create directory if missing
        error = null,
    )
    return requireNotNull(documentDirectory?.path) {
        "Failed to resolve iOS Documents directory path in documentDirectory()"
    }
}
