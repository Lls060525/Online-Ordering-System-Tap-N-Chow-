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

    // Define Sections
    val sections = listOf(
        WheelSection(
            label = "3",
            coinValue = 3,
            color = Color(0xFFFFCC80),
            weight = 0.5,
            displayColor = Brush.radialGradient(listOf(Color(0xFFFFE0B2), Color(0xFFFFB74D)))
        ),
        WheelSection(
            label = "10",
            coinValue = 10,
            color = Color(0xFFFFA726),
            weight = 0.35,
            displayColor = Brush.radialGradient(listOf(Color(0xFFFFCC80), Color(0xFFF57C00)))
        ),
        WheelSection(
            label = "20",
            coinValue = 20,
            color = Color(0xFFFF7043),
            weight = 0.05,
            displayColor = Brush.radialGradient(listOf(Color(0xFFFFAB91), Color(0xFFD84315)))
        ),
        WheelSection(
            label = "50",
            coinValue = 50,
            color = Color(0xFFEF5350),
            weight = 0.0001,
            displayColor = Brush.radialGradient(listOf(Color(0xFFE1BEE7), Color(0xFF8E24AA)))
        )
    )

    val angle = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        spinState = databaseService.getSpinState(customerId)
        isLoading = false
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp), // Added padding to ensure it fits screen width
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- Header ---
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
                                Icon(
                                    Icons.Default.MonetizationOn,
                                    null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // --- THE WHEEL UI ---
                    // FIX: Use aspectRatio(1f) to enforce a perfect circle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f) // Take 90% of the available width
                            .aspectRatio(1f),   // Force Height = Width (Perfect Square Container)
                        contentAlignment = Alignment.Center
                    ) {
                        // 1. Pointer
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

                    // --- Result / Button ---
                    if (winMessage != null) {
                        Text(
                            winMessage!!,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        val isFree = spinState?.isFree == true
                        val canAfford = spinState?.canAfford == true
                        val cost = if (isFree) 0 else 5

                        Button(
                            onClick = {
                                if (!isSpinning && (isFree || canAfford)) {
                                    isSpinning = true
                                    scope.launch {
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

                                        val sliceAngle = 360f / sections.size
                                        val targetAngle = (360f * 5) + (360f - (winningIndex * sliceAngle))

                                        angle.animateTo(
                                            targetValue = targetAngle,
                                            animationSpec = tween(
                                                durationMillis = 3500,
                                                easing = FastOutSlowInEasing
                                            )
                                        )

                                        val success = databaseService.performDailySpin(
                                            customerId,
                                            wonSection.coinValue,
                                            cost
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
                            enabled = !isSpinning && (isFree || canAfford),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .shadow(4.dp, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFree) Color(0xFF4CAF50) else Color(0xFFFFD700),
                                contentColor = if (isFree) Color.White else Color.Black
                            )
                        ) {
                            if (isSpinning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = if (isFree) Color.White else Color.Black,
                                    strokeWidth = 3.dp
                                )
                            } else {
                                if (isFree) {
                                    Text("SPIN FREE", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("SPIN AGAIN ", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Text("(-5 ", fontSize = 14.sp)
                                        Icon(
                                            Icons.Default.MonetizationOn,
                                            null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(")", fontSize = 14.sp)
                                    }
                                }
                            }
                        }

                        if (!isFree && canAfford) {
                            Text(
                                "Spend 5 coins for another chance",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        } else if (!isFree && !canAfford) {
                            Text(
                                "Not enough coins to spin again",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}