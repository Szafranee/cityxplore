package app.cityxplore.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.cityxplore.auth.presentation.AuthState
import app.cityxplore.auth.presentation.AuthViewModel
import app.cityxplore.auth.presentation.EmailVerificationScreen
import app.cityxplore.auth.presentation.LoginScreen
import app.cityxplore.auth.presentation.RegisterScreen
import app.cityxplore.map.presentation.CityXploreMapScreen
import app.cityxplore.map.presentation.MapViewModel
import app.cityxplore.profile.presentation.OnboardingScreen
import app.cityxplore.profile.presentation.ProfileScreen
import app.cityxplore.theme.AppColors
import app.cityxplore.theme.CityXplorePalette
import app.cityxplore.theme.CityXploreTheme
import coil3.compose.setSingletonImageLoaderFactory
import org.koin.compose.koinInject

private enum class CityXploreDestination { Map, Friends, Profile }
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

            when (val state = authState) {
                AuthState.Loading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                AuthState.Authenticated -> MainAppContent(onSignOut = authViewModel::signOut)

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
    val mapState by mapViewModel.state.collectAsState()
    var currentDestination by remember { mutableStateOf(CityXploreDestination.Map) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        bottomBar = {
            CityXploreBottomBar(
                destination = currentDestination,
                onDestinationSelected = { currentDestination = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (currentDestination) {
                CityXploreDestination.Map -> CityXploreMapScreen(
                    state = mapState,
                    onAction = mapViewModel::onAction,
                    modifier = Modifier.fillMaxSize(),
                    onProfileClick = { currentDestination = CityXploreDestination.Profile }
                )

                CityXploreDestination.Friends -> CityXplorePlaceholderScreen("Friends")
                CityXploreDestination.Profile -> ProfileScreen(onSignOut = onSignOut)
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
            unselectedIconColor = AppColors.green,
            selectedTextColor = AppColors.white,
            unselectedTextColor = AppColors.white,
            indicatorColor = MaterialTheme.colorScheme.surface
        )
        NavigationBarItem(
            selected = destination == CityXploreDestination.Map,
            onClick = { onDestinationSelected(CityXploreDestination.Map) },
            colors = navItemColors,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Map,
                    contentDescription = "Discover"
                )
            },
            label = {
                Text(
                    text = "Explore",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.W600,
                    color = AppColors.white
                )
            }
        )
        NavigationBarItem(
            selected = destination == CityXploreDestination.Friends,
            onClick = { onDestinationSelected(CityXploreDestination.Friends) },
            colors = navItemColors,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Group,
                    contentDescription = "Friends"
                )
            },
            label = {
                Text(
                    text = "Friends",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.W600,
                    color = AppColors.white
                )
            }
        )
        NavigationBarItem(
            selected = destination == CityXploreDestination.Profile,
            onClick = { onDestinationSelected(CityXploreDestination.Profile) },
            colors = navItemColors,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.AccountCircle,
                    contentDescription = "Profile"
                )
            },
            label = {
                Text(
                    text = "Profile",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.W600,
                    color = AppColors.white
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
