package app.cityxplore.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Container for coroutine dispatchers used throughout the application.
 *
 * This abstraction allows for easier testing by providing a single point
 * to override dispatchers (e.g. replacing with test dispatchers).
 *
 * @property default Dispatcher for CPU-intensive work (default: [Dispatchers.Default]).
 * @property io Dispatcher for I/O operations like network requests and database access (default: [Dispatchers.Default] on KMP).
 * @property main Dispatcher for UI operations (default: [Dispatchers.Main]).
 */
data class CityXploreDispatchers(
    val default: CoroutineDispatcher = Dispatchers.Default,
    val io: CoroutineDispatcher = Dispatchers.Default,
    val main: CoroutineDispatcher = Dispatchers.Main
)

/**
 * Global singleton instance of [CityXploreDispatchers] used by ViewModels and repositories.
 *
 * Note: In Kotlin Multiplatform, `Dispatchers.IO` is not available, so `Dispatchers.Default`
 * is used for I/O operations on both Android and iOS.
 */
val cityXploreDispatchers = CityXploreDispatchers()
