package app.cityxplore.achievements.data

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

@Serializable
data class UserAchievementDto(
    val achievement: AchievementDto,
    val achievedAt: String?,
    val progress: JsonElement?
)
