package app.cityxplore.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import app.cityxplore.database.entity.AchievementEntity
import app.cityxplore.database.entity.UserAchievementEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Data Access Object for achievement operations.
 *
 * Manages both achievement definitions and user-specific progress.
 */
@Dao
interface AchievementDao {

    // --- Achievement Definitions ---

    /**
     * Observes all achievement definitions.
     */
    @Query("SELECT * FROM achievements ORDER BY points DESC")
    fun observeAllAchievements(): Flow<List<AchievementEntity>>

    /**
     * Gets all achievement definitions synchronously.
     */
    @Query("SELECT * FROM achievements ORDER BY points DESC")
    suspend fun getAllAchievements(): List<AchievementEntity>

    /**
     * Gets an achievement by ID.
     */
    @Query("SELECT * FROM achievements WHERE id = :achievementId")
    suspend fun getAchievementById(achievementId: String): AchievementEntity?

    /**
     * Inserts or updates achievement definitions.
     */
    @Upsert
    suspend fun upsertAchievements(achievements: List<AchievementEntity>)

    /**
     * Inserts or updates a single achievement definition.
     */
    @Upsert
    suspend fun upsertAchievement(achievement: AchievementEntity)

    // --- User Achievement Progress ---

    /**
     * Observes all user achievement progress.
     */
    @Query("SELECT * FROM user_achievements")
    fun observeUserAchievements(): Flow<List<UserAchievementEntity>>

    /**
     * Gets all user achievement progress synchronously.
     */
    @Query("SELECT * FROM user_achievements")
    suspend fun getUserAchievements(): List<UserAchievementEntity>

    /**
     * Gets user progress for a specific achievement.
     */
    @Query("SELECT * FROM user_achievements WHERE achievementId = :achievementId")
    suspend fun getUserAchievementProgress(achievementId: String): UserAchievementEntity?

    /**
     * Inserts or updates user achievement progress.
     */
    @Upsert
    suspend fun upsertUserAchievements(userAchievements: List<UserAchievementEntity>)

    /**
     * Inserts or updates single user achievement progress.
     */
    @Upsert
    suspend fun upsertUserAchievement(userAchievement: UserAchievementEntity)

    /**
     * Marks an achievement as unlocked.
     */
    @Query(
        """
        UPDATE user_achievements 
        SET isUnlocked = 1, 
            unlockedAtMillis = :unlockedAt, 
            progress = 1.0,
            lastSyncedAt = :syncedAt 
        WHERE achievementId = :achievementId
    """
    )
    suspend fun markAsUnlocked(
        achievementId: String,
        unlockedAt: Long,
        syncedAt: Long
    )

    /**
     * Gets count of unlocked achievements.
     */
    @Query("SELECT COUNT(*) FROM user_achievements WHERE isUnlocked = 1")
    suspend fun getUnlockedCount(): Int

    /**
     * Clears all achievement data (used on logout).
     */
    @Transaction
    suspend fun clearAll() {
        clearAchievements()
        clearUserAchievements()
    }

    @Query("DELETE FROM achievements")
    suspend fun clearAchievements()

    @Query("DELETE FROM user_achievements")
    suspend fun clearUserAchievements()

    /**
     * Clears only user achievement data (keeps definitions).
     */
    @Query("DELETE FROM user_achievements")
    suspend fun clearAllUserAchievements()
}

/**
 * Extension function to observe achievements with their user progress.
 */
fun AchievementDao.observeAllWithUserProgress(): Flow<List<Pair<AchievementEntity, UserAchievementEntity?>>> {
    return combine(
        observeAllAchievements(),
        observeUserAchievements()
    ) { achievements, userAchievements ->
        val progressMap = userAchievements.associateBy { it.achievementId }
        achievements.map { achievement ->
            achievement to progressMap[achievement.id]
        }
    }
}

/**
 * Extension function to get achievements with their user progress synchronously.
 */
suspend fun AchievementDao.getAllWithUserProgress(): List<Pair<AchievementEntity, UserAchievementEntity?>> {
    val achievements = getAllAchievements()
    val userAchievements = getUserAchievements()
    val progressMap = userAchievements.associateBy { it.achievementId }
    return achievements.map { achievement ->
        achievement to progressMap[achievement.id]
    }
}
