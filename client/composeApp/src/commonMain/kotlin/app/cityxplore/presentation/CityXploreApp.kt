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
import app.cityxplore.social.presentation.SocialScreen
import app.cityxplore.social.presentation.profile.OtherProfileScreen
import app.cityxplore.theme.AppColors
import app.cityxplore.theme.CityXploreTheme
import coil3.compose.setSingletonImageLoaderFactory
import org.koin.compose.koinInject

private sealed interface CityXploreDestination {
    data object Map : CityXploreDestination
    data class Friends(
        val initialTab: Int = 0, // 0 = Friends, 1 = Rankings
        val rankingSubTab: Int = 0 // 0 = Global, 1 = Friends (used when initialTab = 1)
    ) : CityXploreDestination

    data object Profile : CityXploreDestination
    data object Journal : CityXploreDestination
    data class OtherProfile(
        val userId: String,
        val previousDestination: CityXploreDestination = Friends()
    ) : CityXploreDestination
}

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

    val currentDestination = remember { mutableStateOf<CityXploreDestination>(CityXploreDestination.Map) }

    if (currentDestination.value == CityXploreDestination.Profile || currentDestination.value is CityXploreDestination.Friends) {
        BackHandler {
            currentDestination.value = CityXploreDestination.Map
        }
    }

    if (currentDestination.value is CityXploreDestination.OtherProfile) {
        val otherProfile = currentDestination.value as CityXploreDestination.OtherProfile
        BackHandler {
            currentDestination.value = otherProfile.previousDestination
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        bottomBar = {
            if (currentDestination.value != CityXploreDestination.Journal) {
                CityXploreBottomBar(
                    destination = currentDestination.value,
                    onDestinationSelected = { currentDestination.value = it }
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
            currentDestination.value == CityXploreDestination.Map

            CityXploreMapScreen(
                state = mapState,
                onAction = mapViewModel::onAction,
                modifier = Modifier.fillMaxSize(),
                onProfileClick = { currentDestination.value = CityXploreDestination.Profile }
            )

            when (currentDestination.value) {
                CityXploreDestination.Map -> Unit

                is CityXploreDestination.Friends -> {
                    val friendsDest = currentDestination.value as CityXploreDestination.Friends
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        SocialScreen(
                            initialTab = friendsDest.initialTab,
                            initialRankingSubTab = friendsDest.rankingSubTab,
                            onUserSelected = { userId, fromRankings, isGlobalRanking ->
                                currentDestination.value = CityXploreDestination.OtherProfile(
                                    userId = userId,
                                    previousDestination = CityXploreDestination.Friends(
                                        initialTab = if (fromRankings) 1 else 0,
                                        rankingSubTab = if (fromRankings && !isGlobalRanking) 1 else 0
                                    )
                                )
                            }
                        )
                    }
                }

                CityXploreDestination.Profile -> {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        ProfileScreen(
                            onSignOut = onSignOut,
                            onOpenJournal = {
                                journalViewModel.loadEntries()
                                currentDestination.value = CityXploreDestination.Journal
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
                            onBack = { currentDestination.value = CityXploreDestination.Profile }
                        )
                    }
                }

                is CityXploreDestination.OtherProfile -> {
                    val otherProfileDest = currentDestination.value as CityXploreDestination.OtherProfile
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        OtherProfileScreen(
                            userId = otherProfileDest.userId,
                            onBack = { currentDestination.value = otherProfileDest.previousDestination }
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
            indicatorColor = MaterialTheme.colorScheme.surface
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
            selected = destination is CityXploreDestination.Friends,
            onClick = { onDestinationSelected(CityXploreDestination.Friends()) },
            colors = navItemColors,
            icon = {
                Icon(
                    imageVector = if (destination is CityXploreDestination.Friends) Icons.Filled.Group else Icons.Outlined.Group,
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
