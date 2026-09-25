package com.example.travelroulette24.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelroulette24.engine.TravelIntelligenceEngine
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated Roulette Wheel for TravelRoulette24.
 *
 * Renders an authentic European/Vegas style circular multi-sector wheel with:
 * - Alternating vibrant sector slices with airport/city icons and destination labels
 * - Center chrome hub with spinning compass indicator
 * - Physical top ticker / pointer pin
 * - Dynamic deceleration physics using cubic-bezier easing to mimic a real casino wheel
 * - Visual ticker feedback and active winner highlight
 */
@Composable
fun AnimatedRouletteWheel(
    items: List<TravelIntelligenceEngine.ResultItem>,
    isSpinning: Boolean,
    onSpinFinished: (TravelIntelligenceEngine.ResultItem) -> Unit,
    modifier: Modifier = Modifier
) {
    // Take up to 8 representative candidates for clean sector distribution
    val sectors = remember(items) {
        if (items.isNotEmpty()) items.take(8) else emptyList()
    }

    val sectorCount = sectors.size.coerceAtLeast(6)
    val sliceAngle = 360f / sectorCount

    // Vibrant travel colors for wheel sectors
    val sectorColors = remember {
        listOf(
            Color(0xFFE53935), // Red
            Color(0xFF1E88E5), // Blue
            Color(0xFF43A047), // Green
            Color(0xFFFB8C00), // Orange
            Color(0xFF8E24AA), // Purple
            Color(0xFF00ACC1), // Cyan
            Color(0xFFD81B60), // Magenta
            Color(0xFFFFB300)  // Gold
        )
    }

    val rotation = remember { Animatable(0f) }
    var winningSectorIndex by remember { mutableStateOf(0) }
    var currentHighlightCity by remember { mutableStateOf<String?>(null) }

    // Spin animation whenever isSpinning transitions to true
    LaunchedEffect(isSpinning) {
        if (isSpinning && sectors.isNotEmpty()) {
            val targetIndex = sectors.indices.random()
            winningSectorIndex = targetIndex
            val winningItem = sectors[targetIndex]
            
            // Calculate final target angle: 5 full 360 rotations + offset to align target sector with top pointer (270 deg)
            val fullRotations = (4..6).random() * 360f
            val sectorCenterAngle = targetIndex * sliceAngle + (sliceAngle / 2f)
            // Top pointer is at -90 / 270 degrees
            val targetFinalAngle = fullRotations + (360f - sectorCenterAngle + 270f) % 360f

            rotation.snapTo(rotation.value % 360f)
            rotation.animateTo(
                targetValue = rotation.value + targetFinalAngle + 360f * 2,
                animationSpec = tween(
                    durationMillis = 2800,
                    easing = CubicBezierEasing(0.12f, 0.8f, 0.32f, 1.0f) // realistic wheel friction decelerating
                )
            )
            currentHighlightCity = HubCoordinates.getCity(winningItem.destinationHub)
            onSpinFinished(winningItem)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("animated_roulette_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🎰 Ruleta de Viajes (Salida <24h)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "¡Salimos mañana! Viaje a tu medida",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSpinning) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isSpinning) "⚡ ¡Girando!" else "Listo para girar",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSpinning) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Wheel Container with Pointer Pin
            Box(
                modifier = Modifier.size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                // Circular Canvas Wheel
                Canvas(
                    modifier = Modifier
                        .size(230.dp)
                        .rotate(rotation.value)
                ) {
                    val canvasSize = size.minDimension
                    val radius = canvasSize / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // Outer golden casino rim
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFFD54F), Color(0xFFFFB300), Color(0xFFB78103)),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )

                    // Inner border
                    drawCircle(
                        color = Color(0xFF1E1E24),
                        radius = radius - 8.dp.toPx(),
                        center = center
                    )

                    val sliceRadius = radius - 10.dp.toPx()
                    val arcRect = Size(sliceRadius * 2, sliceRadius * 2)
                    val topLeft = Offset(center.x - sliceRadius, center.y - sliceRadius)

                    // Draw sectors
                    for (i in 0 until sectorCount) {
                        val startAngle = i * sliceAngle
                        val color = sectorColors[i % sectorColors.size]
                        
                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = sliceAngle,
                            useCenter = true,
                            topLeft = topLeft,
                            size = arcRect
                        )

                        // Outer rim dots (casino lights)
                        val dotAngleRad = Math.toRadians((startAngle + sliceAngle / 2f).toDouble())
                        val dotX = center.x + (radius - 4.dp.toPx()) * cos(dotAngleRad).toFloat()
                        val dotY = center.y + (radius - 4.dp.toPx()) * sin(dotAngleRad).toFloat()
                        drawCircle(
                            color = Color.White,
                            radius = 2.5.dp.toPx(),
                            center = Offset(dotX, dotY)
                        )
                    }

                    // Sector separator lines
                    for (i in 0 until sectorCount) {
                        val angleRad = Math.toRadians((i * sliceAngle).toDouble())
                        val endX = center.x + sliceRadius * cos(angleRad).toFloat()
                        val endY = center.y + sliceRadius * sin(angleRad).toFloat()
                        drawLine(
                            color = Color.White.copy(alpha = 0.8f),
                            start = center,
                            end = Offset(endX, endY),
                            strokeWidth = 2.dp.toPx()
                        )
                    }

                    // Center Wheel Chrome Hub
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, Color(0xFFCFD8DC), Color(0xFF455A64)),
                            center = center,
                            radius = 28.dp.toPx()
                        ),
                        radius = 28.dp.toPx(),
                        center = center
                    )

                    drawCircle(
                        color = Color(0xFF263238),
                        radius = 16.dp.toPx(),
                        center = center
                    )

                    drawCircle(
                        color = Color(0xFFFFD54F),
                        radius = 6.dp.toPx(),
                        center = center
                    )
                }

                // Sector text / label ring overlay (static sectors representation)
                if (sectors.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(230.dp)
                            .rotate(rotation.value),
                        contentAlignment = Alignment.Center
                    ) {
                        sectors.forEachIndexed { index, item ->
                            val angle = (index * sliceAngle) + (sliceAngle / 2f)
                            val city = HubCoordinates.getCity(item.destinationHub).take(4)
                            val price = "${item.price.toInt()}€"
                            
                            Box(
                                modifier = Modifier
                                    .size(230.dp)
                                    .rotate(angle),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Column(
                                    modifier = Modifier.padding(top = 18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = item.destinationHub,
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = price,
                                        color = Color.Yellow,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Top Fixed Ticker / Indicator Pin
                Box(
                    modifier = Modifier
                        .size(240.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(width = 24.dp, height = 28.dp)
                            .shadow(4.dp, shape = CircleShape)
                    ) {
                        val path = Path().apply {
                            moveTo(size.width / 2f, size.height) // tip pointing down into wheel
                            lineTo(0f, 0f)
                            lineTo(size.width, 0f)
                            close()
                        }
                        drawPath(
                            path = path,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFFFD54F), Color(0xFFD32F2F))
                            )
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 4.dp.toPx(),
                            center = Offset(size.width / 2f, 8.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer info
            Text(
                text = if (isSpinning) "La rueda está decidiendo tu próxima aventura..."
                else currentHighlightCity?.let { "🎯 Destino seleccionado: $it" } ?: "Pulsa 'Girar Ruleta' para hacerla girar",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
