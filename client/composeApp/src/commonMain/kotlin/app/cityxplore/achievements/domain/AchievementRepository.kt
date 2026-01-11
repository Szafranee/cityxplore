package app.cityxplore.achievements.domain

interface AchievementRepository {
    suspend fun getMyAchievements(): Result<List<Achievement>>
    suspend fun getAllAchievements(): Result<List<Achievement>>
}
