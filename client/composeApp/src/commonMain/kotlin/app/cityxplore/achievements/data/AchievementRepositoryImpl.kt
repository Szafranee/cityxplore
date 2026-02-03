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
     *
     * Supported criteria types:
     * - poi_count: Number of POIs discovered (returns "42/50")
     * - distance_km: Distance traveled in kilometers (returns "21.5/42 km")
     * - friend_count: Number of friends (returns "3/5")
     * - time_range: Time-based achievements (returns "Not yet" or "Completed")
     */
    private fun calculateProgress(
        criteria: JsonElement?,
        progress: JsonElement?,
        isUnlocked: Boolean
    ): Pair<Float, String> {
        if (isUnlocked) return 1f to "Completed"

        try {
            if (criteria != null && progress != null) {
                val criteriaObj = criteria.jsonObject
                val progressObj = progress.jsonObject

                // POI count criteria: {"poi_count": 50}
                if (criteriaObj.containsKey("poi_count")) {
                    val max = criteriaObj["poi_count"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                    val current = progressObj["poi_count"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0

                    if (max > 0) {
                        val p = (current.toFloat() / max.toFloat()).coerceIn(0f, 1f)
                        return p to "$current/$max"
                    }
                }

                // Distance criteria: {"distance_km": 42}
                if (criteriaObj.containsKey("distance_km")) {
                    val max = criteriaObj["distance_km"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                    val current = progressObj["distance_km"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0

                    if (max > 0) {
                        val p = (current / max).coerceIn(0.0, 1.0).toFloat()
                        val currentStr = formatDistance(current)
                        val maxStr = formatDistance(max)
                        return p to "$currentStr/$maxStr km"
                    }
                }

                // Friend count criteria: {"friend_count": 5}
                if (criteriaObj.containsKey("friend_count")) {
                    val max = criteriaObj["friend_count"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                    val current = progressObj["friend_count"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0

                    if (max > 0) {
                        val p = (current.toFloat() / max.toFloat()).coerceIn(0f, 1f)
                        return p to "$current/$max"
                    }
                }

                // Time range criteria: {"time_range": "22:00-04:00"}
                if (criteriaObj.containsKey("time_range")) {
                    val met = progressObj["time_range_met"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
                    return if (met) 1f to "Completed" else 0f to "Not yet"
                }
            }
        } catch (_: Exception) {
            // fallback
        }

        return 0f to ""
    }

    /**
     * Formats distance value for display.
     * Shows integer for whole numbers, 1 decimal place otherwise.
     */
    private fun formatDistance(distance: Double): String {
        return if (distance == distance.toLong().toDouble()) {
            distance.toLong().toString()
        } else {
            // Round to 1 decimal place
            ((distance * 10).toLong() / 10.0).toString()
        }
    }
}
