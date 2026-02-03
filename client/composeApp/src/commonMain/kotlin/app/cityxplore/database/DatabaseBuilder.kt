package app.cityxplore.database

import androidx.room.RoomDatabase

/**
 * Platform-specific database builder factory.
 *
 * Each platform (Android, iOS) provides its own implementation
 * for creating the Room database instance with platform-specific
 * context and configuration.
 */
expect fun getDatabaseBuilder(): RoomDatabase.Builder<CityXploreDatabase>
