package app.cityxplore.auth.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.cityxplore.auth.domain.AuthConstants
import app.cityxplore.auth.domain.SocialProvider
import cityxplore.client.composeapp.generated.resources.Res
import cityxplore.client.composeapp.generated.resources.cityxplore_logo_short
import cityxplore.client.composeapp.generated.resources.discord_logo
import cityxplore.client.composeapp.generated.resources.google_logo
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.compose.auth.composable.NativeSignInResult
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithGoogle
import io.github.jan.supabase.compose.auth.composeAuth
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.koinInject

@Composable
fun LoginScreen(
    state: AuthState,
    onLogin: (String, String) -> Unit,
    onSocialLogin: (SocialProvider) -> Unit,
    onRegisterClick: () -> Unit,
    onClearError: () -> Unit,
    onGoogleSignInError: (String) -> Unit = {}
) {
    val supabaseClient: SupabaseClient = koinInject()

    // Native Google Sign-In state using ComposeAuth
    val googleSignInState = supabaseClient.composeAuth.rememberSignInWithGoogle(
        onResult = { result ->
            when (result) {
                is NativeSignInResult.Success -> {
                    // Session is automatically handled by Supabase
                }

                is NativeSignInResult.ClosedByUser -> {
                    // User cancelled - no action needed
                }

                is NativeSignInResult.Error -> {
                    onGoogleSignInError(result.message)
                }

                is NativeSignInResult.NetworkError -> {
                    onGoogleSignInError(result.message)
                }
            }
        },
        fallback = {
            // Fallback for platforms without native Google Sign-In (uses OAuth redirect)
            onSocialLogin(SocialProvider.GOOGLE)
        }
    )

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }

    fun validateAndLogin() {
        val isEmailValid = email.isNotBlank() // Allow username login, so just check not blank
        val isPasswordValid = password.length >= AuthConstants.MIN_PASSWORD_LENGTH

        emailError = !isEmailValid
        passwordError = !isPasswordValid

        if (isEmailValid && isPasswordValid) {
            onLogin(email, password)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(Res.drawable.cityxplore_logo_short),
                contentDescription = "CityXplore Logo",
                modifier = Modifier.width(200.dp) // Adjust width as needed
            )
            Text(
                "Login to continue",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (state is AuthState.Error) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onClearError) {
                            Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    emailError = false
                    onClearError()
                },
                label = { Text("Email or Username") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                isError = emailError,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (passwordError) passwordError = false
                    onClearError()
                },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                isError = passwordError,
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            if (passwordError) {
                Text(
                    "Password must be at least ${AuthConstants.MIN_PASSWORD_LENGTH} characters",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { validateAndLogin() },
                enabled = state !is AuthState.Loading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (state is AuthState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Login")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Or continue with",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SocialLoginButton(
                    altText = "Continue with Google",
                    onClick = { googleSignInState.startFlow() },
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    logo = vectorResource(Res.drawable.google_logo),
                    logoSize = 86
                )

                // SocialLoginButton(
                //     altText = "Continue with Facebook",
                //     onClick = { onSocialLogin(SocialProvider.FACEBOOK) },
                //     containerColor = Color(0xFF1877F2),
                //     contentColor = Color.White,
                //     logo = vectorResource(Res.drawable.facebook_logo),
                // )

                SocialLoginButton(
                    altText = "Continue with Discord",
                    onClick = { onSocialLogin(SocialProvider.DISCORD) },
                    containerColor = Color(0xFF5865F2),
                    contentColor = Color.White,
                    logo = vectorResource(Res.drawable.discord_logo),
                    logoSize = 112
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = onRegisterClick) {
                Text("Don't have an account? Register", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun SocialLoginButton(
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    logo: ImageVector,
    altText: String = "",
    logoSize: Int = 96,
    logoTint: Color = Color.Unspecified
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Icon(
            imageVector = logo,
            contentDescription = altText,
            modifier = Modifier.size(logoSize.dp),
            tint = logoTint
        )
    }
}
