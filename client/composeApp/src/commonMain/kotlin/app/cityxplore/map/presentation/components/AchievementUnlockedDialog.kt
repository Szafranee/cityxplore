package app.cityxplore.map.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.cityxplore.achievements.domain.Achievement
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

/**
 * Large, celebratory dialog for achievement unlocks with subtle confetti effect.
 *
 * Features:
 * - Scale-in animation when appearing
 * - Subtle falling confetti particles with rotation and wave motion
 * - Achievement details: icon, name, description, points
 * - "Awesome!" button to dismiss
 *
 * @param achievements List of newly unlocked achievements to display.
 * @param onDismiss Callback when the user dismisses the dialog.
 */
@Composable
fun AchievementUnlockedDialog(
    achievements: List<Achievement>,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    var showConfetti by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        visible = true
        // Hide confetti after 4 seconds
        delay(4000)
        showConfetti = false
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Subtle confetti effect with smooth fade out
            ConfettiEffect(
                visible = showConfetti,
                particleCount = 100 // Increased quantity as requested
            )

            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                exit = scaleOut(animationSpec = tween(200)) + fadeOut(animationSpec = tween(200))
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Trophy icon
                        Text(
                            text = "🏆",
                            style = MaterialTheme.typography.displayLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Text(
                            text = if (achievements.size == 1) "Achievement Unlocked!" else "Achievements Unlocked!",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        // Achievement details
                        achievements.forEach { achievement ->
                            AchievementCard(achievement)
                            if (achievement != achievements.last()) {
                                Spacer(Modifier.height(16.dp))
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Awesome!")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementCard(achievement: Achievement) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            if (achievement.iconUrl != null) {
                AsyncImage(
                    model = achievement.iconUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                )
            } else {
                // Placeholder icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🎖️",
                        style = MaterialTheme.typography.displaySmall
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "⭐",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "+${achievement.points} points",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Enhanced confetti effect with falling particles, rotation, and wave motion.
 *
 * @param visible Controls the visibility (fade in/out) of the confetti.
 * @param particleCount Number of confetti particles to generate.
 */
@Composable
private fun ConfettiEffect(
    visible: Boolean,
    particleCount: Int = 100
) {
    val particles = remember {
        List(particleCount) {
            ConfettiParticle(
                x = Random.nextFloat(),
                startY = Random.nextFloat() * 0.4f - 0.4f,
                color = confettiColors[Random.nextInt(confettiColors.size)],
                size = Random.nextFloat() * 10f + 6f,
                speed = Random.nextFloat() * 0.4f + 0.4f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = Random.nextFloat() * 200f - 100f,
                waveAmplitude = Random.nextFloat() * 0.03f + 0.01f,
                waveFrequency = Random.nextFloat() * 3f + 2f,
                shape = if (Random.nextBoolean()) ConfettiShape.CIRCLE else ConfettiShape.RECTANGLE
            )
        }
    }

    // Smooth fade out animation
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 1000)
    )

    var progress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 5000, easing = LinearEasing), // Slower animation
        label = "confetti"
    )

    LaunchedEffect(Unit) {
        progress = 1f
    }

    if (alpha > 0f) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { particle ->
                val currentY = particle.startY + (animatedProgress * particle.speed * 1.3f)
                if (currentY <= 1.1f) {
                    // Add wave motion to X
                    val waveOffset =
                        sin(animatedProgress * particle.waveFrequency * 6.28f) * particle.waveAmplitude
                    val currentX = particle.x + waveOffset

                    val centerX = currentX * size.width
                    val centerY = currentY * size.height
                    val currentRotation =
                        particle.rotation + (animatedProgress * particle.rotationSpeed)

                    rotate(
                        degrees = currentRotation,
                        pivot = Offset(centerX, centerY)
                    ) {
                        when (particle.shape) {
                            ConfettiShape.CIRCLE -> {
                                drawCircle(
                                    color = particle.color,
                                    radius = particle.size,
                                    center = Offset(centerX, centerY),
                                    alpha = alpha // Apply fade out
                                )
                            }

                            ConfettiShape.RECTANGLE -> {
                                drawRect(
                                    color = particle.color,
                                    topLeft = Offset(
                                        centerX - particle.size / 2,
                                        centerY - particle.size / 2
                                    ),
                                    size = Size(particle.size, particle.size * 0.6f),
                                    alpha = alpha // Apply fade out
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class ConfettiShape {
    CIRCLE,
    RECTANGLE
}

private data class ConfettiParticle(
    val x: Float,
    val startY: Float,
    val color: Color,
    val size: Float,
    val speed: Float,
    val rotation: Float,
    val rotationSpeed: Float,
    val waveAmplitude: Float,
    val waveFrequency: Float,
    val shape: ConfettiShape
)

private val confettiColors = listOf(
    Color(0xFFFFD700), // Gold
    Color(0xFF4CAF50), // Green
    Color(0xFF2196F3), // Blue
    Color(0xFFE91E63), // Pink
    Color(0xFFFF9800), // Orange
    Color(0xFF9C27B0), // Purple
    Color(0xFF00BCD4), // Cyan
    Color(0xFFFF5722)  // Deep Orange
)
