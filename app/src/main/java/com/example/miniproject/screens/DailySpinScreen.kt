package com.example.miniproject.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.example.miniproject.service.DatabaseService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

// Define the prizes sections
data class WheelSection(
    val label: String,
    val coinValue: Int,
    val color: Color,
    val weight: Double,
    val displayColor: Brush
)
@Composable
fun DailySpinDialog(
    customerId: String,
    onDismiss: () -> Unit,
    onCoinsWon: (Int) -> Unit
) {
    val databaseService = DatabaseService()
    val scope = rememberCoroutineScope()

    // Wheel State
    var isSpinning by remember { mutableStateOf(false) }
    var winMessage by remember { mutableStateOf<String?>(null) }

    // Logic States
    var spinState by remember { mutableStateOf<DatabaseService.SpinState?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Timer State
    var timeRemainingString by remember { mutableStateOf("") }

    // Define Sections (Keep your existing sections code here...)
    val sections = listOf(
        WheelSection("3", 3, Color(0xFFFFCC80), 0.5, Brush.radialGradient(listOf(Color(0xFFFFE0B2), Color(0xFFFFB74D)))),
        WheelSection("10", 10, Color(0xFFFFA726), 0.35, Brush.radialGradient(listOf(Color(0xFFFFCC80), Color(0xFFF57C00)))),
        WheelSection("20", 20, Color(0xFFFF7043), 0.05, Brush.radialGradient(listOf(Color(0xFFFFAB91), Color(0xFFD84315)))),
        WheelSection("50", 50, Color(0xFFEF5350), 0.0001, Brush.radialGradient(listOf(Color(0xFFE1BEE7), Color(0xFF8E24AA))))
    )

    val angle = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        spinState = databaseService.getSpinState(customerId)
        isLoading = false
    }

    // Countdown Timer Logic ---
    LaunchedEffect(spinState) {
        if (spinState != null && !spinState!!.canSpin) {
            while (true) {
                val now = System.currentTimeMillis()
                val diff = spinState!!.nextSpinTimestamp - now

                if (diff <= 0) {
                    // Timer finished, refresh state to enable spin
                    timeRemainingString = "00:00:00"
                    spinState = databaseService.getSpinState(customerId)
                } else {
                    val hours = diff / (1000 * 60 * 60)
                    val minutes = (diff % (1000 * 60 * 60)) / (1000 * 60)
                    val seconds = (diff % (1000 * 60)) / 1000
                    timeRemainingString = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                }
                delay(1000L) // Update every second
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Daily Lucky Spin",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isLoading) {
                    CircularProgressIndicator()
                } else {

                    // --- Balance Display ---
                    if (spinState != null) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(bottom = 24.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text("Balance: ", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "${spinState!!.currentCoins}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.MonetizationOn, null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // --- WHEEL UI (Copy existing Wheel UI code here) ---

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {

                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (-8).dp)
                                .size(60.dp)
                                .zIndex(10f)
                                .shadow(4.dp, shape = CircleShape)
                        )

                        // 2. Wheel Canvas
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .shadow(10.dp, CircleShape)
                                .rotate(angle.value)
                        ) {
                            val center = Offset(size.width / 2, size.height / 2)
                            val radius = size.minDimension / 2
                            val sweepAngle = 360f / sections.size

                            sections.forEachIndexed { index, section ->
                                val startAngle = (index * sweepAngle) - 90f

                                drawArc(
                                    brush = section.displayColor,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = true,
                                    size = size
                                )

                                drawArc(
                                    color = Color.White.copy(alpha = 0.5f),
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = true,
                                    size = size,
                                    style = Stroke(width = 2f)
                                )

                                drawIntoCanvas { canvas ->
                                    val paint = Paint().apply {
                                        color = android.graphics.Color.WHITE
                                        textSize = radius * 0.3f // Responsive text size
                                        textAlign = Paint.Align.CENTER
                                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                        setShadowLayer(5f, 0f, 0f, android.graphics.Color.BLACK)
                                    }

                                    val midAngleRad = Math.toRadians((startAngle + sweepAngle / 2).toDouble())
                                    val textRadius = radius * 0.65f
                                    val x = center.x + cos(midAngleRad) * textRadius
                                    val y = center.y + sin(midAngleRad) * textRadius

                                    canvas.nativeCanvas.drawText(
                                        section.label,
                                        x.toFloat(),
                                        y.toFloat() + (radius * 0.1f), // responsive vertical adjust
                                        paint
                                    )

                                    val smallPaint = Paint().apply {
                                        color = android.graphics.Color.WHITE
                                        textSize = radius * 0.12f
                                        textAlign = Paint.Align.CENTER
                                        typeface = Typeface.DEFAULT
                                    }
                                    canvas.nativeCanvas.drawText(
                                        "COINS",
                                        x.toFloat(),
                                        y.toFloat() + (radius * 0.25f),
                                        smallPaint
                                    )
                                }
                            }

                            drawCircle(
                                color = Color(0xFFFFD700),
                                radius = radius,
                                style = Stroke(width = radius * 0.05f)
                            )
                        }

                        // 3. Center Knob
                        Box(
                            modifier = Modifier
                                .fillMaxSize(0.25f) // Take 25% of the size
                                .aspectRatio(1f)    // Keep knob circular
                                .shadow(8.dp, CircleShape)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color.White, Color(0xFFE0E0E0))
                                    )
                                )
                                .border(4.dp, Color(0xFFFFD700), CircleShape)
                                .zIndex(5f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "SPIN",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp, // You might want to make this responsive too if needed
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))


                    if (winMessage != null) {
                        Text(
                            winMessage!!,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        val canSpin = spinState?.canSpin == true

                        Button(
                            onClick = {
                                if (!isSpinning && canSpin) {
                                    isSpinning = true
                                    scope.launch {
                                        // Random Logic
                                        val rand = Math.random()
                                        var cumulative = 0.0
                                        var winningIndex = 0
                                        for ((index, section) in sections.withIndex()) {
                                            cumulative += section.weight
                                            if (rand <= cumulative) {
                                                winningIndex = index
                                                break
                                            }
                                        }
                                        val wonSection = sections[winningIndex]

                                        // Animation
                                        val sliceAngle = 360f / sections.size
                                        val targetAngle = (360f * 5) + (360f - (winningIndex * sliceAngle))

                                        angle.animateTo(
                                            targetValue = targetAngle,
                                            animationSpec = tween(
                                                durationMillis = 3500,
                                                easing = FastOutSlowInEasing
                                            )
                                        )

                                        // Database Call (No cost passed)
                                        val success = databaseService.performDailySpin(
                                            customerId,
                                            wonSection.coinValue
                                        )

                                        if (success.isSuccess) {
                                            winMessage = "You won ${wonSection.label} Coins!"
                                            delay(1500)
                                            onCoinsWon(wonSection.coinValue)
                                            spinState = databaseService.getSpinState(customerId)
                                        } else {
                                            winMessage = "Network Error"
                                        }
                                        isSpinning = false
                                    }
                                }
                            },
                            enabled = !isSpinning && canSpin,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .shadow(4.dp, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (canSpin) Color(0xFF4CAF50) else Color.Gray,
                                contentColor = Color.White,
                                disabledContainerColor = Color.LightGray,
                                disabledContentColor = Color.DarkGray
                            )
                        ) {
                            if (isSpinning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 3.dp
                                )
                            } else {
                                if (canSpin) {
                                    Text("SPIN NOW", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                } else {
                                    // Show Timer inside disabled button
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("NEXT SPIN IN", fontSize = 12.sp)
                                        Text(timeRemainingString, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }
                            }
                        }

                        // Helper text below button
                        if (!canSpin) {
                            Text(
                                "Come back tomorrow for another chance!",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}