package app.cityxplore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import app.cityxplore.database.initializeDatabaseContext
import org.koin.android.ext.koin.androidContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialise database context before Koin modules that need it
        initializeDatabaseContext(applicationContext)

        setContent {
            CityXploreRoot(
                activity = this
            ) {
                androidContext(this@MainActivity)
            }
        }
    }
}
