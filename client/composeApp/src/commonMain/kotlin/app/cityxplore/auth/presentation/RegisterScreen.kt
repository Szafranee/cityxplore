package app.cityxplore.auth.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.cityxplore.auth.domain.SocialProvider
import cityxplore.client.composeapp.generated.resources.Res
import cityxplore.client.composeapp.generated.resources.discord_logo
import cityxplore.client.composeapp.generated.resources.facebook_logo
import cityxplore.client.composeapp.generated.resources.google_logo
import org.jetbrains.compose.resources.vectorResource

@Composable
fun RegisterScreen(
    state: AuthState,
    onRegister: (String, String) -> Unit,
    onSocialLogin: (SocialProvider) -> Unit,
    onLoginClick: () -> Unit,
    onClearError: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }

    fun validateAndRegister() {
        val isEmailValid = email.isNotBlank() && email.contains("@")
        val isPasswordValid = password.length >= 6

        emailError = !isEmailValid
        passwordError = !isPasswordValid

        if (isEmailValid && isPasswordValid) {
            onRegister(email, password)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Create Account",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Sign up to get started",
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
                if (emailError) emailError = false
            },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            isError = emailError,
            supportingText = { if (emailError) Text("Invalid email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                if (passwordError) passwordError = false
            },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            isError = passwordError,
            supportingText = { if (passwordError) Text("Password must be at least 6 characters") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { validateAndRegister() },
            enabled = state !is AuthState.Loading,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (state is AuthState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Register")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        Text("Or sign up with", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SocialLoginButton(
                altText = "Sign up with Google",
                onClick = { onSocialLogin(SocialProvider.GOOGLE) },
                containerColor = Color.White,
                contentColor = Color.Black,
                logo = vectorResource(Res.drawable.google_logo),
                logoSize = 86
            )

            SocialLoginButton(
                altText = "Sign up with Facebook",
                onClick = { onSocialLogin(SocialProvider.FACEBOOK) },
                containerColor = Color(0xFF1877F2),
                contentColor = Color.White,
                logo = vectorResource(Res.drawable.facebook_logo)
            )

            SocialLoginButton(
                altText = "Sign up with Discord",
                onClick = { onSocialLogin(SocialProvider.DISCORD) },
                containerColor = Color(0xFF5865F2),
                contentColor = Color.White,
                logo = vectorResource(Res.drawable.discord_logo),
                logoSize = 112
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onLoginClick) {
            Text("Already have an account? Login")
        }
    }
}
