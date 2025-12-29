package app.cityxplore.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

actual abstract class CityXploreBaseViewModel {
    private val job = SupervisorJob()
    protected actual val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + job)

    protected actual open fun onCleared() {
        job.cancel()
    }
}
