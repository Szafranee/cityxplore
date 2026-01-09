package app.cityxplore.achievements.data

import app.cityxplore.achievements.domain.Achievement
import app.cityxplore.achievements.domain.AchievementRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Instant

class AchievementRepositoryImpl(
    private val client: HttpClient
) : AchievementRepository {
    override suspend fun getMyAchievements(): Result<List<Achievement>> = runCatching {
        val dtos = client.get("https://api.cityxplore.app/api/achievements/mine").body<List<UserAchievementDto>>()
        dtos.map { dto ->
            // Try parsing timestamp
            val unlockedAt = dto.achievedAt?.let { str ->
                try {
                    Instant.parse(str)
                } catch (_: Exception) {
                    try {
                        LocalDateTime.parse(str).toInstant(TimeZone.UTC)
                    } catch (_: Exception) {
                        null
                    }
                }
            }

            Achievement(
                id = dto.achievement.id,
                name = dto.achievement.name,
                description = dto.achievement.description,
                category = dto.achievement.category,
                iconUrl = dto.achievement.iconUrl,
                points = dto.achievement.points,
                isUnlocked = dto.achievedAt != null,
                unlockedAt = unlockedAt
            )
        }
    }

    override suspend fun getAllAchievements(): Result<List<Achievement>> = runCatching {
        val dtos = client.get("https://api.cityxplore.app/api/achievements").body<List<AchievementDto>>()
        dtos.map { dto ->
            Achievement(
                id = dto.id,
                name = dto.name,
                description = dto.description,
                category = dto.category,
                iconUrl = dto.iconUrl,
                points = dto.points,
                isUnlocked = false, // Default for "all" list, logic in VM will merge
                unlockedAt = null
            )
        }
    }
}
