package com.example.miniproject.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun OrderConfirmationScreen(navController: NavController, orderId: String?) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Order Confirmed",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                "Order Confirmed!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            if (!orderId.isNullOrEmpty()) {
                Text(
                    "Order ID: $orderId",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                "Your order has been placed successfully.\nThe vendor will start preparing your order soon.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        // Navigate to orders screen
                        navController.navigate("home") {
                            popUpTo(0) // Clear entire back stack
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.width(200.dp)
                ) {
                    Text("View My Orders")
                }

                OutlinedButton(
                    onClick = {
                        // Continue shopping
                        navController.navigate("home") {
                            popUpTo(0) // Clear entire back stack
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.width(200.dp)
                ) {
                    Text("Continue Shopping")
                }
            }
        }
    }
}