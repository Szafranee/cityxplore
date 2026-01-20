package app.cityxplore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import app.cityxplore.core.lifecycle.AppLifecycleObserver
import app.cityxplore.di.achievementsModule
import app.cityxplore.di.authModule
import app.cityxplore.di.databaseModule
import app.cityxplore.di.h3Module
import app.cityxplore.di.journalModule
import app.cityxplore.di.locationModule
import app.cityxplore.di.mapModule
import app.cityxplore.di.networkModule
import app.cityxplore.di.notificationModule
import app.cityxplore.di.platformModule
import app.cityxplore.di.profileModule
import app.cityxplore.di.providePlatformEngine
import app.cityxplore.di.socialModule
import app.cityxplore.di.syncModule
import app.cityxplore.platform.HandleDeepLinks
import app.cityxplore.presentation.CityXploreApp
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.core.KoinApplication

@Composable
fun CityXploreRoot(
    activity: LifecycleOwner? = null,
    koinInit: (KoinApplication.() -> Unit)? = null
) {
    KoinApplication(application = {
        koinInit?.invoke(this)
        modules(
            providePlatformEngine(),
            platformModule,
            databaseModule(),
            authModule,
            networkModule(),
            syncModule(),
            notificationModule(),
            mapModule,
            locationModule,
            profileModule,
            achievementsModule,
            socialModule,
            journalModule,
            h3Module
        )
    }) {
        // Setup lifecycle observer for optimised data loading
        activity?.let { lifecycleOwner ->
            LifecycleObserverEffect(lifecycleOwner)
        }

        HandleDeepLinks()
        CityXploreApp()
    }
}

/**
 * Composable that observes lifecycle events and updates AppLifecycleObserver.
 * This prevents unnecessary data reloads when quickly switching between apps.
 */
@Composable
private fun LifecycleObserverEffect(lifecycleOwner: LifecycleOwner) {
    val appLifecycleObserver: AppLifecycleObserver = koinInject()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> appLifecycleObserver.onForeground()
                Lifecycle.Event.ON_STOP -> appLifecycleObserver.onBackground()
                else -> { /* ignore other events */
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
