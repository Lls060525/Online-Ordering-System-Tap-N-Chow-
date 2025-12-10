package com.example.miniproject

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.miniproject.service.DatabaseService
import com.example.miniproject.service.PayPalService
import com.example.miniproject.ui.theme.MiniProjectTheme
import kotlinx.coroutines.launch

class PayPalWebViewActivity : ComponentActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.getStringExtra("PAYPAL_URL") ?: ""
        val orderId = intent.getStringExtra("ORDER_ID") ?: ""

        setContent {
            MiniProjectTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PayPalWebView(url, orderId)
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PayPalWebView(url: String, orderId: String) {
    val context = LocalContext.current
    val databaseService = DatabaseService()
    val payPalService = PayPalService()
    val coroutineScope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36"

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        url: String
                    ): Boolean {
                        Log.d("PayPalWebView", "Loading URL: $url")

                        // Check if this is our return URL
                        if (url.contains("com.example.miniproject.paypeltest")) {
                            // Extract token from URL
                            val uri = android.net.Uri.parse(url)
                            val token = uri.getQueryParameter("token")

                            if (token != null) {
                                // Capture the payment
                                coroutineScope.launch {
                                    isLoading = true
                                    try {
                                        val captureResult = payPalService.captureOrder(token)

                                        if (captureResult.isSuccess) {
                                            val captureResponse = captureResult.getOrThrow()

                                            if (captureResponse.status == "COMPLETED") {
                                                // Update order status in database
                                                databaseService.updatePaymentStatus(orderId, "completed")
                                                databaseService.updateOrderStatus(orderId, "confirmed")

                                                // Close WebView and return to app
                                                (context as? PayPalWebViewActivity)?.run {
                                                    val intent = android.content.Intent(this, MainActivity::class.java).apply {
                                                        putExtra("ORDER_ID", orderId)
                                                        putExtra("PAYMENT_SUCCESS", true)
                                                        flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                                                android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                                    }
                                                    startActivity(intent)
                                                    finish()
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
                                errorMessage = "No payment token found in return URL"
                                isLoading = false
                            }
                            return true
                        }
                        return false
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        isLoading = false
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        errorMessage = "Failed to load PayPal: ${error?.description}"
                        isLoading = false
                    }
                }

                loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxSize()
    )

    // Show loading indicator
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(color = Color.White)
                Text(
                    "Redirecting to PayPal...",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }

    // Show error message
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Payment Error") },
            text = { Text(errorMessage ?: "Unknown error occurred") },
            confirmButton = {
                Button(onClick = {
                    (context as? PayPalWebViewActivity)?.finish()
                }) {
                    Text("OK")
                }
            }
        )
    }
}