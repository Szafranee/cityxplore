package app.cityxplore.achievements.domain

import kotlin.time.Instant

data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val category: String?,
    val iconUrl: String?,
    val points: Int,
    val isUnlocked: Boolean,
    val unlockedAt: Instant?
)
