package app.cityxplore.platform

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import org.koin.compose.koinInject

@Composable
actual fun HandleDeepLinks() {
    val context = LocalContext.current
    val supabase = koinInject<SupabaseClient>()

    LaunchedEffect(Unit) {
        val activity = context as? Activity
        activity?.intent?.let { intent ->
            supabase.handleDeeplinks(intent)
        }
    }
}
