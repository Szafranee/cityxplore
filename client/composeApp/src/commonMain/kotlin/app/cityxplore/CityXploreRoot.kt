package app.cityxplore

import androidx.compose.runtime.Composable
import app.cityxplore.di.achievementsModule
import app.cityxplore.di.authModule
import app.cityxplore.di.h3Module
import app.cityxplore.di.journalModule
import app.cityxplore.di.locationModule
import app.cityxplore.di.mapModule
import app.cityxplore.di.networkModule
import app.cityxplore.di.profileModule
import app.cityxplore.di.providePlatformEngine
import app.cityxplore.platform.HandleDeepLinks
import app.cityxplore.presentation.CityXploreApp
import org.koin.compose.KoinApplication
import org.koin.core.KoinApplication

@Composable
fun CityXploreRoot(
    koinInit: (KoinApplication.() -> Unit)? = null
) {
    KoinApplication(application = {
        koinInit?.invoke(this)
        modules(
            providePlatformEngine(),
            authModule,
            networkModule(),
            mapModule,
            locationModule,
            profileModule,
            achievementsModule,
            journalModule,
            h3Module
        )
    }) {
        HandleDeepLinks()
        CityXploreApp()
    }
}
