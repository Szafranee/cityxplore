package app.cityxplore.achievements.data

import app.cityxplore.achievements.domain.Achievement
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class AchievementDto(
    val id: String,
    val name: String,
    val description: String,
    val category: String?,
    val iconUrl: String?,
    val points: Int,
    val criteria: JsonElement? = null
)

fun AchievementDto.toDomain(): Achievement = Achievement(
    id = id,
    name = name,
    description = description,
    category = category,
    iconUrl = iconUrl,
    points = points,
    isUnlocked = true, // When receiving from backend in discovery response, it's always newly unlocked
    unlockedAt = null, // Will be set by the backend timestamp if needed
    progress = 1f,
    progressFormatted = "Completed"
)

@Serializable
data class UserAchievementDto(
    val achievement: AchievementDto,
    val achievedAt: String?,
    val progress: JsonElement?
)
