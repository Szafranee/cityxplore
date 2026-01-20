package app.cityxplore.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Database migrations for CityXplore.
 *
 * These migrations preserve user data (especially SyncQueue with pending offline operations)
 * when the database schema changes, instead of dropping all tables.
 */
object Migrations {

    /**
     * Migration from version 1 to 2.
     *
     * Changes:
     * - Added `photosJson` column to `pois` table (default: "[]")
     * - Added `metadataJson` column to `pois` table (default: "{}")
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(connection: SQLiteConnection) {
            // Add photosJson column with default empty array
            connection.execSQL(
                "ALTER TABLE pois ADD COLUMN photosJson TEXT NOT NULL DEFAULT '[]'"
            )
            // Add metadataJson column with default empty object
            connection.execSQL(
                "ALTER TABLE pois ADD COLUMN metadataJson TEXT NOT NULL DEFAULT '{}'"
            )
        }
    }

    /**
     * List of all migrations to be applied.
     */
    val ALL_MIGRATIONS = arrayOf(
        MIGRATION_1_2
    )
}
