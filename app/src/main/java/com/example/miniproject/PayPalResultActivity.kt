// [file name]: PayPalResultActivity.kt
package com.example.miniproject

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.example.miniproject.screens.OrderConfirmationScreen
import com.example.miniproject.service.DatabaseService
import com.example.miniproject.service.PayPalService
import com.example.miniproject.ui.theme.MiniProjectTheme
import kotlinx.coroutines.launch

class PayPalResultActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent.data
        if (uri != null) {
            // Handle the deep link
            handlePayPalReturn(uri)
        }

        setContent {
            MiniProjectTheme {
                PayPalResultScreen(uri)
            }
        }
    }

    private fun handlePayPalReturn(uri: Uri) {
        // Extract parameters from the return URL
        val token = uri.getQueryParameter("token")
        val payerId = uri.getQueryParameter("PayerID")

        if (token != null && payerId != null) {
            // We have a successful return from PayPal
            // You can process the payment capture here
        } else {
            // User cancelled
            finish()
        }
    }
}

@Composable
fun PayPalResultScreen(uri: Uri?) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val databaseService = DatabaseService()
    val payPalService = PayPalService()
    val coroutineScope = rememberCoroutineScope()

    var status by remember { mutableStateOf("Processing PayPal payment...") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var orderId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uri) {
        if (uri != null) {
            val token = uri.getQueryParameter("token")

            if (token != null) {
                // Capture the PayPal order
                coroutineScope.launch {
                    try {
                        status = "Capturing PayPal payment..."

                        val captureResult = payPalService.captureOrder(token)

                        if (captureResult.isSuccess) {
                            val captureResponse = captureResult.getOrThrow()

                            if (captureResponse.status == "COMPLETED") {
                                // Find the custom_id which should be our order ID
                                val purchaseUnit = captureResponse.purchase_units.firstOrNull()
                                val customId = purchaseUnit?.custom_id

                                if (customId != null) {
                                    orderId = customId

                                    // Update order status in database
                                    databaseService.updatePaymentStatus(customId, "completed")
                                    databaseService.updateOrderStatus(customId, "confirmed")

                                    status = "Payment successful! Order confirmed."
                                    isLoading = false

                                    // Navigate to order confirmation after delay
                                    kotlinx.coroutines.delay(2000)
                                    navController.navigate("orderConfirmation/$customId")
                                } else {
                                    errorMessage = "Order ID not found in PayPal response"
                                    isLoading = false
                                }
                            } else {
                                errorMessage = "PayPal payment not completed. Status: ${captureResponse.status}"
                                isLoading = false
                            }
                        } else {
                            errorMessage = "Failed to capture PayPal payment: ${captureResult.exceptionOrNull()?.message}"
                            isLoading = false
                        }
                    } catch (e: Exception) {
                        errorMessage = "Error: ${e.message}"
                        isLoading = false
                    }
                }
            } else {
                errorMessage = "No payment token found"
                isLoading = false
            }
        } else {
            errorMessage = "No URI provided"
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = status,
                style = MaterialTheme.typography.bodyLarge
            )
        } else if (errorMessage != null) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage ?: "Unknown error",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = {
                // Navigate back to home
                navController.navigate("home") {
                    popUpTo(0)
                }
            }) {
                Text("Return to Home")
            }
        } else if (orderId != null) {
            OrderConfirmationScreen(navController = navController, orderId = orderId)
        }
    }
}