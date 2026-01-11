package app.cityxplore.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cityxplore.auth.presentation.AuthState
import app.cityxplore.auth.presentation.AuthViewModel
import app.cityxplore.auth.presentation.EmailVerificationScreen
import app.cityxplore.auth.presentation.LoginScreen
import app.cityxplore.auth.presentation.RegisterScreen
import app.cityxplore.journal.presentation.JournalScreen
import app.cityxplore.journal.presentation.JournalViewModel
import app.cityxplore.map.presentation.CityXploreMapScreen
import app.cityxplore.map.presentation.MapViewModel
import app.cityxplore.platform.BackHandler
import app.cityxplore.profile.presentation.OnboardingScreen
import app.cityxplore.profile.presentation.ProfileScreen
import app.cityxplore.theme.AppColors
import app.cityxplore.theme.CityXplorePalette
import app.cityxplore.theme.CityXploreTheme
import coil3.compose.setSingletonImageLoaderFactory
import org.koin.compose.koinInject

private enum class CityXploreDestination { Map, Friends, Profile, Journal }
private enum class AuthScreen { Login, Register }

@Composable
fun CityXploreApp() {
    setSingletonImageLoaderFactory { context ->
        getImageLoader(context)
    }

    CityXploreTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val authViewModel: AuthViewModel = koinInject()
            val authState by authViewModel.state.collectAsState()
            val userId by authViewModel.userId.collectAsState()

            when (val state = authState) {
                AuthState.Loading -> SplashScreen()

                AuthState.Authenticated -> key(userId) {
                    MainAppContent(onSignOut = authViewModel::signOut)
                }

                AuthState.Onboarding -> OnboardingScreen(
                    onProfileCreated = {
                        authViewModel.refreshProfileCheck()
                    },
                    onSignOut = authViewModel::signOut
                )

                is AuthState.EmailVerification -> EmailVerificationScreen(
                    email = state.email,
                    onResendEmail = { authViewModel.resendVerificationEmail(state.email) },
                    onBackToLogin = { authViewModel.cancelVerification() }
                )

                AuthState.Unauthenticated, is AuthState.Error -> AuthFlow(state, authViewModel)
            }
        }
    }
}

@Composable
fun AuthFlow(state: AuthState, viewModel: AuthViewModel) {
    var currentScreen by remember { mutableStateOf(AuthScreen.Login) }

    when (currentScreen) {
        AuthScreen.Login -> LoginScreen(
            state = state,
            onLogin = viewModel::signIn,
            onSocialLogin = { viewModel.onSocialLogin(it) },
            onRegisterClick = { currentScreen = AuthScreen.Register },
            onClearError = viewModel::clearError
        )

        AuthScreen.Register -> RegisterScreen(
            state = state,
            onRegister = viewModel::signUp,
            onSocialLogin = { viewModel.onSocialLogin(it) },
            onLoginClick = { currentScreen = AuthScreen.Login },
            onClearError = viewModel::clearError
        )
    }
}

@Composable
fun MainAppContent(onSignOut: () -> Unit) {
    val mapViewModel: MapViewModel = koinInject()
    val journalViewModel: JournalViewModel = koinInject()
    val mapState by mapViewModel.state.collectAsState()
    val journalState by journalViewModel.state.collectAsState()
    var currentDestination by remember { mutableStateOf(CityXploreDestination.Map) }

    // Handle back navigation for screens other than Map
    // JournalScreen has its own BackHandler, but we can have a global one too if we coordinate.
    // However, since JournalScreen is instantiated conditionally, its BackHandler is only active then.
    // For Friends and Profile, they don't have BackHandler, so we add one here.
    // If we are on Profile or Friends, back should go to Map.
    if (currentDestination == CityXploreDestination.Profile || currentDestination == CityXploreDestination.Friends) {
        BackHandler {
            currentDestination = CityXploreDestination.Map
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        bottomBar = {
            if (currentDestination != CityXploreDestination.Journal) {
                CityXploreBottomBar(
                    destination = currentDestination,
                    onDestinationSelected = { currentDestination = it }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Keep MapScreen always in composition to avoid reloading Mapbox/Fog of War
            // Use alpha/visibility to hide it when not active (or z-order)
            // Since MapView is heavy, this is better for UX but consumes more memory.
            currentDestination == CityXploreDestination.Map

            // Render MapScreen if it's the current destination OR if we want to caching it.
            // Using a Box to stack them. Map is always at bottom (index 0).
            // But if we just put it in the Box, it will be covered by others if they have opaque background.
            // We need to ensure we don't dispose it.

            // Map is always rendered, but we can control if other screens are on top.
            // However, Mapbox native view might have Z-ordering issues on Android.
            // Let's try rendering it always, but conditionally rendering others on top.

            CityXploreMapScreen(
                state = mapState,
                onAction = mapViewModel::onAction,
                modifier = Modifier.fillMaxSize(),
                // If map is not visible, we can maybe disable interactions?
                // But generally, keeping it in the tree is enough.
                onProfileClick = { currentDestination = CityXploreDestination.Profile }
            )

            // Overlay other screens
            when (currentDestination) {
                CityXploreDestination.Map -> { /* Already rendered below */
                }

                CityXploreDestination.Friends -> {
                    // Need to ensure background is opaque
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        CityXplorePlaceholderScreen("Friends")
                    }
                }

                CityXploreDestination.Profile -> {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        ProfileScreen(
                            onSignOut = onSignOut,
                            onOpenJournal = {
                                journalViewModel.loadEntries()
                                currentDestination = CityXploreDestination.Journal
                            }
                        )
                    }
                }

                CityXploreDestination.Journal -> {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        JournalScreen(
                            state = journalState,
                            searchQuery = journalViewModel.searchQuery.collectAsState().value,
                            currentFilter = journalViewModel.filter.collectAsState().value,
                            currentSort = journalViewModel.sort.collectAsState().value,
                            onSearchQueryChange = journalViewModel::setSearchQuery,
                            onFilterChange = journalViewModel::setFilter,
                            onSortChange = journalViewModel::setSort,
                            onToggleFavorite = journalViewModel::toggleFavorite,
                            onBack = { currentDestination = CityXploreDestination.Profile }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CityXploreBottomBar(
    destination: CityXploreDestination,
    onDestinationSelected: (CityXploreDestination) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        val navItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = AppColors.green,
            unselectedIconColor = Color.White,
            selectedTextColor = AppColors.green,
            unselectedTextColor = Color.White,
            indicatorColor = MaterialTheme.colorScheme.surface // Or Color.Transparent if we want to hide the pill
        )

        NavigationBarItem(
            selected = destination == CityXploreDestination.Map,
            onClick = { onDestinationSelected(CityXploreDestination.Map) },
            colors = navItemColors,
            icon = {
                Icon(
                    imageVector = if (destination == CityXploreDestination.Map) Icons.Filled.Map else Icons.Outlined.Map,
                    contentDescription = "Discover",
                    modifier = Modifier.size(30.dp)
                )
            },
            label = {
                Text(
                    text = "Explore",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.W600,
                )
            }
        )
        NavigationBarItem(
            selected = destination == CityXploreDestination.Friends,
            onClick = { onDestinationSelected(CityXploreDestination.Friends) },
            colors = navItemColors,
            icon = {
                Icon(
                    imageVector = if (destination == CityXploreDestination.Friends) Icons.Filled.Group else Icons.Outlined.Group,
                    contentDescription = "Friends",
                    modifier = Modifier.size(30.dp)
                )
            },
            label = {
                Text(
                    text = "Friends",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.W600,
                )
            }
        )
        NavigationBarItem(
            selected = destination == CityXploreDestination.Profile,
            onClick = { onDestinationSelected(CityXploreDestination.Profile) },
            colors = navItemColors,
            icon = {
                Icon(
                    imageVector = if (destination == CityXploreDestination.Profile) Icons.Filled.AccountCircle else Icons.Outlined.AccountCircle,
                    contentDescription = "Profile",
                    modifier = Modifier.size(30.dp)
                )
            },
            label = {
                Text(
                    text = "Profile",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.W600,
                )
            }
        )
    }
}

@Composable
private fun CityXplorePlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "$title screen",
            style = MaterialTheme.typography.titleMedium,
            color = CityXplorePalette.TextMuted
        )
    }
}
