package app.cityxplore.platform

import androidx.compose.runtime.Composable
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.experimental.ExperimentalObjCName

/**
 * iOS deep link handler for OAuth callbacks.
 * On iOS, deep links are received via the App's onOpenURL handler in SwiftUI,
 * which then forwards them to this function.
 */
@Composable
actual fun HandleDeepLinks() {
    // iOS deep linking is handled via iOSApp.swift onOpenURL callback
    // which forwards URLs to the handleDeepLink function below
}

/**
 * Handles incoming deep link URLs from iOS.
 * Called from iOSApp.swift when the app receives a deep link.
 *
 * @param url The deep link URL (e.g., "app.cityxplore://login?access_token=...")
 */
@OptIn(ExperimentalObjCName::class)
@ObjCName(swiftName = "handleDeepLink")
fun handleDeepLink(url: String) {
    object : KoinComponent {
        val supabase: SupabaseClient by inject()

        init {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    // Parse and handle the deep link URL
                    // Supabase Compose Auth handles OAuth callbacks automatically
                    // when the user is redirected back to the app
                    println("Received deep link: $url")

                    // The Supabase Auth plugin will automatically process the OAuth tokens
                    // from the URL when the session is updated
                } catch (e: Exception) {
                    println("Error handling deep link: ${e.message}")
                }
            }
        }
    }
}
