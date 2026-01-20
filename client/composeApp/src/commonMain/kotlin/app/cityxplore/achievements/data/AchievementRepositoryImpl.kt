package app.cityxplore.achievements.data

import app.cityxplore.achievements.domain.Achievement
import app.cityxplore.achievements.domain.AchievementRepository
import app.cityxplore.database.dao.AchievementDao
import app.cityxplore.database.dao.getAllWithUserProgress
import app.cityxplore.database.dao.observeAllWithUserProgress
import app.cityxplore.database.entity.AchievementEntity
import app.cityxplore.database.entity.UserAchievementEntity
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Instant

/**
 * Offline-first implementation of [AchievementRepository].
 *
 * Key behaviors:
 * - **Reading:** Flow from local Room database
 * - **Refreshing:** Network → local cache
 * - **Offline:** Returns cached data when network unavailable
 *
 * @param client Ktor HTTP client for making API requests.
 * @param achievementDao Local database access for achievement caching.
 */
class AchievementRepositoryImpl(
    private val client: HttpClient,
    private val achievementDao: AchievementDao
) : AchievementRepository {

    /**
     * Observes achievements for the current user from the local database.
     */
    override fun observeMyAchievements(): Flow<List<Achievement>> {
        return achievementDao.observeAllWithUserProgress().map { list ->
            list.map { (achievement, userProgress) ->
                val unlockedAt = userProgress?.unlockedAtMillis?.let { Instant.fromEpochMilliseconds(it) }
                Achievement(
                    id = achievement.id,
                    name = achievement.name,
                    description = achievement.description,
                    category = achievement.category,
                    iconUrl = achievement.iconUrl,
                    points = achievement.points,
                    isUnlocked = userProgress?.isUnlocked ?: false,
                    unlockedAt = unlockedAt,
                    progress = userProgress?.progress ?: 0f,
                    progressFormatted = userProgress?.progressFormatted ?: ""
                )
            }
        }
    }

    /**
     * Retrieves achievements - first tries local cache, then network.
     */
    override suspend fun getMyAchievements(): Result<List<Achievement>> = runCatching {
        // Try the local cache first
        val cachedAchievements = achievementDao.getAllWithUserProgress()
        if (cachedAchievements.isNotEmpty()) {
            return@runCatching cachedAchievements.map { (achievement, userProgress) ->
                val unlockedAt = userProgress?.unlockedAtMillis?.let { Instant.fromEpochMilliseconds(it) }
                Achievement(
                    id = achievement.id,
                    name = achievement.name,
                    description = achievement.description,
                    category = achievement.category,
                    iconUrl = achievement.iconUrl,
                    points = achievement.points,
                    isUnlocked = userProgress?.isUnlocked ?: false,
                    unlockedAt = unlockedAt,
                    progress = userProgress?.progress ?: 0f,
                    progressFormatted = userProgress?.progressFormatted ?: ""
                )
            }
        }

        // No cache - fetch from network and cache
        refreshFromNetwork()
        achievementDao.getAllWithUserProgress().map { (achievement, userProgress) ->
            val unlockedAt = userProgress?.unlockedAtMillis?.let { Instant.fromEpochMilliseconds(it) }
            Achievement(
                id = achievement.id,
                name = achievement.name,
                description = achievement.description,
                category = achievement.category,
                iconUrl = achievement.iconUrl,
                points = achievement.points,
                isUnlocked = userProgress?.isUnlocked ?: false,
                unlockedAt = unlockedAt,
                progress = userProgress?.progress ?: 0f,
                progressFormatted = userProgress?.progressFormatted ?: ""
            )
        }
    }

    /**
     * Refreshes achievements from the network and updates local cache.
     */
    override suspend fun refreshMyAchievements(): Result<Unit> = runCatching {
        refreshFromNetwork()
    }

    /**
     * Internal function to fetch from network and update local cache.
     */
    private suspend fun refreshFromNetwork() {
        val dtos = client.get("https://api.cityxplore.app/api/achievements/mine").body<List<UserAchievementDto>>()

        dtos.forEach { dto ->
            // Save achievement definition
            val achievementEntity = AchievementEntity.create(
                id = dto.achievement.id,
                name = dto.achievement.name,
                description = dto.achievement.description,
                category = dto.achievement.category,
                iconUrl = dto.achievement.iconUrl,
                points = dto.achievement.points
            )
            achievementDao.upsertAchievement(achievementEntity)

            // Calculate progress
            val (progress, formatted) = calculateProgress(
                dto.achievement.criteria,
                dto.progress,
                dto.achievedAt != null
            )

            // Parse timestamp
            val achievedAtMillis = dto.achievedAt?.let { str ->
                try {
                    Instant.parse(str).toEpochMilliseconds()
                } catch (_: Exception) {
                    try {
                        LocalDateTime.parse(str).toInstant(TimeZone.UTC).toEpochMilliseconds()
                    } catch (_: Exception) {
                        null
                    }
                }
            }

            // Save user progress
            val userAchievementEntity = UserAchievementEntity.create(
                achievementId = dto.achievement.id,
                isUnlocked = dto.achievedAt != null,
                unlockedAtMillis = achievedAtMillis,
                progress = progress,
                progressFormatted = formatted
            )
            achievementDao.upsertUserAchievement(userAchievementEntity)
        }
    }

    /**
     * Clears local achievement cache (used on logout).
     */
    override suspend fun clearLocalCache() {
        achievementDao.clearAllUserAchievements()
    }

    /**
     * Retrieves all available achievement definitions from the backend.
     */
    override suspend fun getAllAchievements(): Result<List<Achievement>> = runCatching {
        val dtos = client.get("https://api.cityxplore.app/api/achievements").body<List<AchievementDto>>()
        dtos.map { dto ->
            val (progress, formatted) = calculateProgress(dto.criteria, null, false)

            Achievement(
                id = dto.id,
                name = dto.name,
                description = dto.description,
                category = dto.category,
                iconUrl = dto.iconUrl,
                points = dto.points,
                isUnlocked = false,
                unlockedAt = null,
                progress = progress,
                progressFormatted = formatted
            )
        }
    }

    /**
     * Retrieves achievements for a specific user by their user ID.
     */
    override suspend fun getUserAchievements(userId: String): Result<List<Achievement>> = runCatching {
        val dtos =
            client.get("https://api.cityxplore.app/api/achievements/user/$userId").body<List<UserAchievementDto>>()
        dtos.map { dto ->
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

    /**
     * Calculates achievement progress based on criteria and current progress data.
     */
    private fun calculateProgress(
        criteria: JsonElement?,
        progress: JsonElement?,
        isUnlocked: Boolean
    ): Pair<Float, String> {
        if (isUnlocked) return 1f to "Completed"

        try {
            if (criteria != null) {
                val criteriaObj = criteria.jsonObject

                if (criteriaObj.containsKey("count")) {
                    val max = criteriaObj["count"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                    val current = progress?.jsonObject?.get("count")?.jsonPrimitive?.content?.toIntOrNull() ?: 0

                    if (max > 0) {
                        val p = (current.toFloat() / max.toFloat()).coerceIn(0f, 1f)
                        return p to "$current/$max"
                    }
                }

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
        } catch (_: Exception) {
            // fallback
        }

        return 0f to ""
    }
}
