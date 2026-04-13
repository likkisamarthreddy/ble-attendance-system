package com.example.practice.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.practice.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ═══════════════════════════════════════════
// GLASSMORPHISM CARD
// ═══════════════════════════════════════════

@Composable
fun GlassmorphismCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(GlassBorder, GlassBorder.copy(alpha = 0.05f))
                ),
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = GlassSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}

// ═══════════════════════════════════════════
// GRADIENT BUTTON
// ═══════════════════════════════════════════

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    gradientColors: List<Color> = GradientPrimary
) {
    val alpha = if (enabled) 1f else 0.5f

    Button(
        onClick = onClick,
        modifier = modifier
            .height(52.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(colors = gradientColors),
                    shape = RoundedCornerShape(14.dp),
                    alpha = alpha
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = text,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// ANIMATED STATUS INDICATOR (Pulsing Dot)
// ═══════════════════════════════════════════

@Composable
fun PulsingDot(
    color: Color = SuccessGreen,
    size: Dp = 12.dp,
    isActive: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.4f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 0.4f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(contentAlignment = Alignment.Center) {
        // Glow ring
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(size * 2)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(color.copy(alpha = alpha * 0.3f))
            )
        }
        // Solid dot
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
        )
    }
}

// ═══════════════════════════════════════════
// SHIMMER EFFECT
// ═══════════════════════════════════════════

@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    durationMillis: Int = 1200
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val offsetX by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 300f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        DarkCard.copy(alpha = 0.6f),
                        DarkCard.copy(alpha = 0.2f),
                        DarkCard.copy(alpha = 0.6f)
                    ),
                    start = Offset(offsetX, 0f),
                    end = Offset(offsetX + 200f, 0f)
                )
            )
    )
}

// ═══════════════════════════════════════════
// COUNTDOWN TIMER (Circular Arc)
// ═══════════════════════════════════════════

@Composable
fun CountdownTimer(
    totalSeconds: Int = 7,
    remainingMillis: Long,
    modifier: Modifier = Modifier,
    activeColor: Color = TertiaryCyan,
    trackColor: Color = DarkSurfaceVariant
) {
    val progress = remainingMillis.toFloat() / (totalSeconds * 1000f)
    val displaySeconds = (remainingMillis / 1000).toInt() + 1

    Box(
        modifier = modifier.size(64.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
            // Progress
            drawArc(
                color = activeColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Text(
            text = "${displaySeconds}s",
            color = activeColor,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}

// ═══════════════════════════════════════════
// RADAR PULSE (Scanning Animation)
// ═══════════════════════════════════════════

@Composable
fun RadarPulse(
    modifier: Modifier = Modifier,
    color: Color = PrimaryIndigo,
    isActive: Boolean = true
) {
    if (!isActive) return

    val infiniteTransition = rememberInfiniteTransition(label = "radar")

    // Three rings with staggered starts
    val rings = listOf(0, 700, 1400)

    Box(modifier = modifier.size(200.dp), contentAlignment = Alignment.Center) {
        rings.forEach { delayMs ->
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2100, easing = EaseOut, delayMillis = delayMs),
                    repeatMode = RepeatMode.Restart
                ),
                label = "radarScale$delayMs"
            )
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2100, easing = EaseOut, delayMillis = delayMs),
                    repeatMode = RepeatMode.Restart
                ),
                label = "radarAlpha$delayMs"
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = color.copy(alpha = alpha),
                    radius = size.minDimension / 2 * scale,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Center dot
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

// ═══════════════════════════════════════════
// WAVE ANIMATION (Broadcasting)
// ═══════════════════════════════════════════

@Composable
fun WaveAnimation(
    modifier: Modifier = Modifier,
    color: Color = SecondaryPurple,
    isActive: Boolean = true
) {
    if (!isActive) return

    val infiniteTransition = rememberInfiniteTransition(label = "wave")

    Box(modifier = modifier.size(200.dp), contentAlignment = Alignment.Center) {
        // Concentric arcs
        (0..2).forEach { i ->
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.4f + (i * 0.1f),
                targetValue = 0.8f + (i * 0.1f),
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = EaseInOut, delayMillis = i * 300),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "waveScale$i"
            )
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 0.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = EaseInOut, delayMillis = i * 300),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "waveAlpha$i"
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = color.copy(alpha = alpha),
                    startAngle = -60f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(
                        size.width * (1 - scale) / 2,
                        size.height * (1 - scale) / 2
                    ),
                    size = size * scale,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // Center icon circle
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(color, color.copy(alpha = 0.7f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Broadcasting",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════
// SECURITY LAYER STATUS ROW
// ═══════════════════════════════════════════

data class SecurityLayerState(
    val icon: ImageVector,
    val label: String,
    val verified: Boolean?,  // null = pending, true = verified, false = failed
)

@Composable
fun SecurityLayerStatus(
    layers: List<SecurityLayerState>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        layers.forEach { layer ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            when (layer.verified) {
                                true -> SuccessGreen.copy(alpha = 0.15f)
                                false -> ErrorCoral.copy(alpha = 0.15f)
                                null -> DarkSurfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (layer.verified) {
                            true -> Icons.Default.Check
                            false -> Icons.Default.Close
                            null -> layer.icon
                        },
                        contentDescription = layer.label,
                        tint = when (layer.verified) {
                            true -> SuccessGreen
                            false -> ErrorCoral
                            null -> TextSecondaryDark
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = layer.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = when (layer.verified) {
                        true -> SuccessGreen
                        false -> ErrorCoral
                        null -> TextSecondaryDark
                    },
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ═══════════════════════════════════════════
// ANIMATED GRADIENT BACKGROUND
// ═══════════════════════════════════════════

@Composable
fun AnimatedGradientBackground(
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(DarkBackground, DarkSurface, PrimaryIndigoDeep.copy(alpha = 0.3f)),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(colors = colors)
            ),
        content = content
    )
}

// ═══════════════════════════════════════════
// FLOATING PARTICLES BACKGROUND
// ═══════════════════════════════════════════

@Composable
fun FloatingParticles(
    modifier: Modifier = Modifier,
    particleCount: Int = 20,
    color: Color = PrimaryIndigo.copy(alpha = 0.15f)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        for (i in 0 until particleCount) {
            val seed = i * 137.5f // Golden angle spread
            val baseX = (seed * 7.3f) % w
            val baseY = (seed * 13.7f) % h
            val radius = 2f + (i % 5) * 1.5f

            // Simple deterministic drift
            val driftX = cos(seed.toDouble()).toFloat() * 20f
            val driftY = sin(seed.toDouble()).toFloat() * 20f

            drawCircle(
                color = color,
                radius = radius,
                center = Offset(baseX + driftX, baseY + driftY)
            )
        }
    }
}

// ═══════════════════════════════════════════
// STATUS CHIP
// ═══════════════════════════════════════════

@Composable
fun StatusChip(
    text: String,
    color: Color = SuccessGreen,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PulsingDot(color = color, size = 8.dp)
            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ═══════════════════════════════════════════
// STUDENT CARD
// ═══════════════════════════════════════════

@Composable
fun StudentCard(student: com.example.practice.ResponsesModel.Student) {
    GlassmorphismCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Text(
                    text = "Roll No: ${student.rollno}",
                    fontSize = 14.sp,
                    color = TextSecondaryDark
                )
            }

            val attendance = student.attendancePercentage.toFloatOrNull() ?: 0f
            val badgeColor = when {
                attendance >= 75f -> SuccessGreen
                attendance >= 60f -> WarningAmber
                else -> ErrorCoral
            }

            Surface(
                color = badgeColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "${student.attendancePercentage}%",
                    color = badgeColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
