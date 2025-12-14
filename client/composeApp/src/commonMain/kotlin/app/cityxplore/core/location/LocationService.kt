package app.cityxplore.core.location

import kotlinx.coroutines.flow.Flow

data class Location(val latitude: Double, val longitude: Double)

interface LocationService {
    fun observeLocation(): Flow<Location>
    suspend fun getCurrentLocation(): Location?
}
