package app.cityxplore.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.cityxplore.achievements.domain.Achievement
import app.cityxplore.database.currentTimeMillis
import kotlin.time.Instant

/**
 * Room entity for caching achievement definitions.
 *
 * Stores the base achievement information (name, description, criteria).
 * User-specific progress is stored in [UserAchievementEntity].
 */
@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val category: String?,
    val iconUrl: String?,
    val points: Int,
    val lastSyncedAt: Long
) {
    /**
     * Converts this entity to a domain model without user progress.
     */
    fun toDomain(): Achievement = Achievement(
        id = id,
        name = name,
        description = description,
        category = category,
        iconUrl = iconUrl,
        points = points,
        isUnlocked = false,
        unlockedAt = null,
        progress = 0f,
        progressFormatted = ""
    )

    companion object {
        fun create(
            id: String,
            name: String,
            description: String,
            category: String?,
            iconUrl: String?,
            points: Int
        ) = AchievementEntity(
            id = id,
            name = name,
            description = description,
            category = category,
            iconUrl = iconUrl,
            points = points,
            lastSyncedAt = currentTimeMillis()
        )
    }
}

/**
 * Room entity for storing user's achievement progress.
 *
 * Links a user to their achievement progress and unlock status.
 */
@Entity(tableName = "user_achievements")
data class UserAchievementEntity(
    @PrimaryKey
    val achievementId: String,
    val isUnlocked: Boolean,
    val unlockedAtMillis: Long?,
    val progress: Float,
    val progressFormatted: String,
    val lastSyncedAt: Long
) {
    companion object {
        fun create(
            achievementId: String,
            isUnlocked: Boolean,
            unlockedAtMillis: Long?,
            progress: Float,
            progressFormatted: String
        ) = UserAchievementEntity(
            achievementId = achievementId,
            isUnlocked = isUnlocked,
            unlockedAtMillis = unlockedAtMillis,
            progress = progress,
            progressFormatted = progressFormatted,
            lastSyncedAt = currentTimeMillis()
        )
    }
}

/**
 * Combines achievement definition with user progress into a domain model.
 */
fun AchievementEntity.toDomainWithProgress(userProgress: UserAchievementEntity?): Achievement {
    return Achievement(
        id = id,
        name = name,
        description = description,
        category = category,
        iconUrl = iconUrl,
        points = points,
        isUnlocked = userProgress?.isUnlocked ?: false,
        unlockedAt = userProgress?.unlockedAtMillis?.let { Instant.fromEpochMilliseconds(it) },
        progress = userProgress?.progress ?: 0f,
        progressFormatted = userProgress?.progressFormatted ?: ""
    )
}
