package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.AssistantMode
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.GlowAmber
import com.example.ui.theme.LaserPink
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AiOrb(
    mode: AssistantMode,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_infinite")

    // Breathing pulse
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // Fast rotation for Thinking / Speaking
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (mode) {
                    is AssistantMode.Thinking -> 1800
                    is AssistantMode.Speaking -> 3000
                    is AssistantMode.Listening -> 4500
                    else -> 8000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Secondary reverse rotation
    val reverseRotationAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rev_rotation"
    )

    // Calculate dynamic RMS multiplier for listening mode
    val rmsBoost = if (mode is AssistantMode.Listening) {
        val clampedRms = mode.rmsDb.coerceIn(0f, 10f)
        (clampedRms / 10f) * 0.35f
    } else 0f

    val finalScale = breathingScale + rmsBoost

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (this.size.minDimension / 2.6f) * finalScale

            // Select color scheme based on current state
            val (coreColor1, coreColor2, ringColor1, ringColor2) = when (mode) {
                is AssistantMode.Listening -> {
                    listOf(NeonCyan, Color(0xFF00B4D8), NeonCyan.copy(alpha = 0.9f), ElectricViolet)
                }
                is AssistantMode.Speaking -> {
                    listOf(ElectricViolet, LaserPink, NeonCyan, LaserPink)
                }
                is AssistantMode.Thinking -> {
                    listOf(GlowAmber, LaserPink, GlowAmber, ElectricViolet)
                }
                is AssistantMode.Error -> {
                    listOf(Color(0xFFFF3366), Color(0xFFCC0033), Color(0xFFFF6688), Color(0xFFFF0033))
                }
                else -> { // Idle
                    listOf(NeonCyan.copy(alpha = 0.85f), NeonPurple.copy(alpha = 0.85f), NeonCyan.copy(alpha = 0.5f), ElectricViolet.copy(alpha = 0.5f))
                }
            }

            // Outer Atmospheric Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreColor1.copy(alpha = 0.35f),
                        coreColor2.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.55f
                ),
                radius = baseRadius * 1.55f,
                center = center
            )

            // Outer Rotating Dashed Ring 1
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(ringColor1, Color.Transparent, ringColor2, ringColor1),
                    center = center
                ),
                startAngle = rotationAngle,
                sweepAngle = 260f,
                useCenter = false,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
                size = androidx.compose.ui.geometry.Size(baseRadius * 2.2f, baseRadius * 2.2f),
                topLeft = Offset(center.x - baseRadius * 1.1f, center.y - baseRadius * 1.1f)
            )

            // Inner Rotating Ring 2 (reverse)
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(Color.Transparent, ringColor2, Color.Transparent, ringColor1),
                    center = center
                ),
                startAngle = reverseRotationAngle,
                sweepAngle = 220f,
                useCenter = false,
                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round),
                size = androidx.compose.ui.geometry.Size(baseRadius * 1.8f, baseRadius * 1.8f),
                topLeft = Offset(center.x - baseRadius * 0.9f, center.y - baseRadius * 0.9f)
            )

            // Core Orb Gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.9f),
                        coreColor1,
                        coreColor2,
                        Color(0xFF07090E)
                    ),
                    center = Offset(center.x - baseRadius * 0.18f, center.y - baseRadius * 0.18f),
                    radius = baseRadius
                ),
                radius = baseRadius * 0.85f,
                center = center
            )

            // Floating Satellite Nodes
            val nodeCount = 4
            for (i in 0 until nodeCount) {
                val angleRad = Math.toRadians((rotationAngle + (i * 360f / nodeCount)).toDouble())
                val orbitDist = baseRadius * 1.18f
                val nodeX = (center.x + orbitDist * cos(angleRad)).toFloat()
                val nodeY = (center.y + orbitDist * sin(angleRad)).toFloat()

                drawCircle(
                    color = ringColor1,
                    radius = 3.dp.toPx(),
                    center = Offset(nodeX, nodeY)
                )
            }
        }
    }
}
