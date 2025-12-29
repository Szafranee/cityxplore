package app.cityxplore.platform

import kotlinx.coroutines.CoroutineScope

expect abstract class CityXploreBaseViewModel() {
    protected val scope: CoroutineScope

    protected open fun onCleared()
}
