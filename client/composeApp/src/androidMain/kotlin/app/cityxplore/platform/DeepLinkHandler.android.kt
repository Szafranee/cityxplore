package app.cityxplore.platform

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
actual fun HandleDeepLinks() {
    val context = LocalContext.current
    val supabase = koinInject<SupabaseClient>()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Handle deep links on initial launch and when the app resumes (e.g. after OAuth redirect)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val activity = context as? Activity
                activity?.intent?.let { intent ->
                    // Check if this intent has deep link data
                    if (intent.data != null) {
                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                supabase.handleDeeplinks(intent)
                            } catch (e: Exception) {
                                println("Error handling deep link: ${e.message}")
                            }
                        }
                    }
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
