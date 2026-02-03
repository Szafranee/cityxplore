package app.cityxplore.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Android-specific reference to the application context.
 * Must be initialised in Application.onCreate() or MainActivity.
 */
internal lateinit var applicationContext: Context

/**
 * Initialises the database context. Must be called before database access.
 */
fun initializeDatabaseContext(context: Context) {
    applicationContext = context.applicationContext
}

/**
 * Android implementation of the database builder.
 *
 * Uses Room.databaseBuilder with the application context to create
 * the database instance with proper file storage.
 */
actual fun getDatabaseBuilder(): RoomDatabase.Builder<CityXploreDatabase> {
    return Room.databaseBuilder(
        context = applicationContext,
        klass = CityXploreDatabase::class.java,
        name = CityXploreDatabase.DATABASE_NAME
    )
}
