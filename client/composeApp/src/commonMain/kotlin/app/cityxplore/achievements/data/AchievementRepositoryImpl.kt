package app.cityxplore.achievements.data

import app.cityxplore.achievements.domain.Achievement
import app.cityxplore.achievements.domain.AchievementRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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

            val (progress, formatted) = calculateProgress(
                dto.achievement.criteria,
                dto.progress,
                dto.achievedAt != null
            )

            Achievement(
                id = dto.achievement.id,
                name = dto.achievement.name,
                description = dto.achievement.description,
                category = dto.achievement.category,
                iconUrl = dto.achievement.iconUrl,
                points = dto.achievement.points,
                isUnlocked = dto.achievedAt != null,
                unlockedAt = unlockedAt,
                progress = progress,
                progressFormatted = formatted
            )
        }
    }

    override suspend fun getAllAchievements(): Result<List<Achievement>> = runCatching {
        val dtos = client.get("https://api.cityxplore.app/api/achievements").body<List<AchievementDto>>()
        dtos.map { dto ->
            // For general list, we assume 0 progress unless merged later
            val (progress, formatted) = calculateProgress(dto.criteria, null, false)

            Achievement(
                id = dto.id,
                name = dto.name,
                description = dto.description,
                category = dto.category,
                iconUrl = dto.iconUrl,
                points = dto.points,
                isUnlocked = false, // Default for "all" list, logic in VM will merge
                unlockedAt = null,
                progress = progress,
                progressFormatted = formatted
            )
        }
    }

    private fun calculateProgress(
        criteria: kotlinx.serialization.json.JsonElement?,
        progress: kotlinx.serialization.json.JsonElement?,
        isUnlocked: Boolean
    ): Pair<Float, String> {
        if (isUnlocked) return 1f to "Completed"

        try {
            if (criteria != null) {
                val criteriaObj = criteria.jsonObject

                // Identify criteria type
                // 1. Count based (e.g. "count": 50)
                if (criteriaObj.containsKey("count")) {
                    val max = criteriaObj["count"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                    val current = progress?.jsonObject?.get("count")?.jsonPrimitive?.content?.toIntOrNull() ?: 0

                    if (max > 0) {
                        val p = (current.toFloat() / max.toFloat()).coerceIn(0f, 1f)
                        return p to "$current/$max"
                    }
                }

                // 2. Distance based ("distance_km": 42)
                if (criteriaObj.containsKey("distance_km")) {
                    val max = criteriaObj["distance_km"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 0f
                    val current =
                        progress?.jsonObject?.get("distance_km")?.jsonPrimitive?.content?.toFloatOrNull() ?: 0f

                    if (max > 0) {
                        val p = (current / max).coerceIn(0f, 1f)
                        val currentStr = if (current % 1 == 0f) current.toInt().toString() else current.toString()
                        val maxStr = if (max % 1 == 0f) max.toInt().toString() else max.toString()
                        return p to "$currentStr/$maxStr km"
                    }
                }
            }
        } catch (e: Exception) {
            // fallback
        }

        return 0f to ""
    }
}
