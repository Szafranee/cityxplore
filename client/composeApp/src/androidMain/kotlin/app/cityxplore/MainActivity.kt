package app.cityxplore

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.edit
import app.cityxplore.core.notifications.AndroidNotificationService
import app.cityxplore.database.initializeDatabaseContext
import org.koin.android.ext.koin.androidContext

class MainActivity : ComponentActivity() {

    companion object {
        private const val PREFS_NAME = "cityxplore_nav_prefs"
        private const val KEY_PENDING_NAVIGATION = "pending_navigation"

        /**
         * Consumes and returns pending navigation target, clearing it afterwards.
         * Returns null if no navigation is pending.
         */
        fun consumeNavigation(context: Context): String? {
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val target = prefs.getString(KEY_PENDING_NAVIGATION, null)
            if (target != null) {
                prefs.edit { remove(KEY_PENDING_NAVIGATION) }
            }
            return target
        }

        /**
         * Sets pending navigation target.
         */
        fun setPendingNavigation(context: Context, target: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            prefs.edit { putString(KEY_PENDING_NAVIGATION, target) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialise database context before Koin modules that need it
        initializeDatabaseContext(applicationContext)

        // Check if launched from notification
        handleNotificationIntent(intent)

        setContent {
            CityXploreRoot(activity = this) {
                androidContext(this@MainActivity)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val navigateTo = intent?.getStringExtra(AndroidNotificationService.EXTRA_NAVIGATE_TO)
        if (navigateTo != null) {
            setPendingNavigation(this, navigateTo)
            intent.removeExtra(AndroidNotificationService.EXTRA_NAVIGATE_TO)
        }
    }
}
