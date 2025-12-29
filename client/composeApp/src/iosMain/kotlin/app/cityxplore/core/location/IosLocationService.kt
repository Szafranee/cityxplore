package app.cityxplore.core.location

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class IosLocationService : LocationService {
    override fun observeLocation(): Flow<Location> {
        // TODO: Implement CoreLocation
        return flowOf()
    }

    override suspend fun getCurrentLocation(): Location? {
        return null
    }
}
