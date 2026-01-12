package app.cityxplore.profile.data

import app.cityxplore.achievements.data.UserAchievementDto
import app.cityxplore.achievements.domain.Achievement
import app.cityxplore.profile.domain.DistanceSyncRepository
import app.cityxplore.profile.domain.DistanceSyncResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * Implementation of [DistanceSyncRepository] using Ktor HTTP client.
 *
 * Syncs distance to the backend API and parses newly unlocked achievements.
 *
 * @param client The HTTP client configured with authentication.
 */
class DistanceSyncRepositoryImpl(
    private val client: HttpClient
) : DistanceSyncRepository {

    override suspend fun syncDistance(distanceMeters: Double): Result<DistanceSyncResult> = runCatching {
        val response = client.post("https://api.cityxplore.app/api/users/me/distance") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(AddDistanceRequestDto(distanceMeters))
        }.body<DistanceSyncResponseDto>()

        DistanceSyncResult(
            newlyUnlockedAchievements = response.newlyUnlockedAchievements.map { it.toAchievement() }
        )
    }

    /**
     * Maps a UserAchievementDto to the domain Achievement model.
     */
    private fun UserAchievementDto.toAchievement(): Achievement {
        val unlockedAt = achievedAt?.let { str ->
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

        return Achievement(
            id = achievement.id,
            name = achievement.name,
            description = achievement.description,
            category = achievement.category,
            iconUrl = achievement.iconUrl,
            points = achievement.points,
            isUnlocked = achievedAt != null,
            unlockedAt = unlockedAt,
            progress = 1f, // Newly unlocked = complete
            progressFormatted = "Complete"
        )
    }
}

@Serializable
private data class AddDistanceRequestDto(
    val distanceMeters: Double
)

@Serializable
private data class DistanceSyncResponseDto(
    val profile: ProfileDto,
    val newlyUnlockedAchievements: List<UserAchievementDto> = emptyList()
)
