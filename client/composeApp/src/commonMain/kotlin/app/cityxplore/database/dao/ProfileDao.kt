package app.cityxplore.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import app.cityxplore.database.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for user profile operations.
 *
 * Provides methods to read and write user profile data to the local database.
 */
@Dao
interface ProfileDao {

    /**
     * Observes the current user's profile.
     * Emits whenever the profile data changes.
     */
    @Query("SELECT * FROM user_profiles LIMIT 1")
    fun observeCurrentProfile(): Flow<UserProfileEntity?>

    /**
     * Gets the current user's profile synchronously.
     */
    @Query("SELECT * FROM user_profiles LIMIT 1")
    suspend fun getCurrentProfile(): UserProfileEntity?

    /**
     * Gets a profile by user ID.
     */
    @Query("SELECT * FROM user_profiles WHERE id = :userId")
    suspend fun getProfileById(userId: String): UserProfileEntity?

    /**
     * Inserts or updates a user profile.
     */
    @Upsert
    suspend fun upsertProfile(profile: UserProfileEntity)

    /**
     * Updates the total distance for the current user.
     */
    @Query("UPDATE user_profiles SET totalDistance = :distance, lastSyncedAt = :syncedAt WHERE id = :userId")
    suspend fun updateDistance(userId: String, distance: Double, syncedAt: Long)

    /**
     * Clears all profile data (used on logout).
     */
    @Query("DELETE FROM user_profiles")
    suspend fun clearAll()
}
