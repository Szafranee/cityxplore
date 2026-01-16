package org.cityxplore.backend.user.dto

import org.cityxplore.backend.achievements.dto.UserAchievementResponse

/**
 * Response DTO after distance sync containing updated profile and newly unlocked achievements.
 *
 * @property profile The updated user profile after distance was added.
 * @property newlyUnlockedAchievements List of achievements unlocked as a result of this distance update.
 */
data class DistanceSyncResponse(
    val profile: UserProfileResponse,
    val newlyUnlockedAchievements: List<UserAchievementResponse> = emptyList()
)
