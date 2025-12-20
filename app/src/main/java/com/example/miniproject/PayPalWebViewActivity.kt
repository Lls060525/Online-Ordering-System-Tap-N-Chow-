package com.example.miniproject

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.miniproject.service.DatabaseService
import com.example.miniproject.service.EmailService // 確保引入了 EmailService
import com.example.miniproject.service.PayPalService
import com.example.miniproject.ui.theme.MiniProjectTheme
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope // 引入 GlobalScope
import kotlinx.coroutines.launch

class PayPalWebViewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val url = intent.getStringExtra("PAYPAL_URL") ?: ""
        val orderId = intent.getStringExtra("ORDER_ID") ?: ""

        setContent {
            MiniProjectTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.systemBars
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        PayPalWebView(url, orderId)
                    }
                }
            }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PayPalWebView(url: String, orderId: String) {
    val context = LocalContext.current
    val databaseService = DatabaseService()
    val payPalService = PayPalService()
    val coroutineScope = rememberCoroutineScope()

    val lifecycleOwner = LocalLifecycleOwner.current
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPaymentCompleted by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                if (!isPaymentCompleted) {
                    Log.d("PayPalWebView", "User closed activity. Deleting order $orderId")
                    kotlinx.coroutines.GlobalScope.launch {
                        databaseService.deleteOrderAndRestoreStock(orderId)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile; rv:88.0) Gecko/88.0 Firefox/88.0"
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String): Boolean {
                        if (url.contains("com.example.miniproject.paypeltest")) {
                            val uri = android.net.Uri.parse(url)
                            val token = uri.getQueryParameter("token")

                            if (token != null) {
                                coroutineScope.launch {
                                    isLoading = true
                                    try {
                                        val captureResult = payPalService.captureOrder(token)

                                        if (captureResult.isSuccess) {
                                            val captureResponse = captureResult.getOrThrow()

                                            if (captureResponse.status == "COMPLETED") {
                                                isPaymentCompleted = true

                                                val purchaseUnit = captureResponse.purchase_units.firstOrNull()
                                                val captureId = purchaseUnit?.payments?.captures?.firstOrNull()?.id
                                                val finalCaptureId = captureId ?: captureResponse.id

                                                Log.d("PayPalSuccess", "Extracted Capture ID: $finalCaptureId")

                                                databaseService.updateOrderWithPayPalId(
                                                    orderId,
                                                    finalCaptureId,
                                                    captureResponse.payer?.payer_id ?: ""
                                                )
                                                databaseService.updatePaymentStatus(orderId, "completed")
                                                databaseService.updateOrderStatus(orderId, "confirmed")

                                                GlobalScope.launch {
                                                    try {
                                                        Log.d("ReceiptSystem", "Background: Preparing to send receipt for $orderId")
                                                        val order = databaseService.getOrderById(orderId)
                                                        if (order != null) {
                                                            val customer = databaseService.getCustomerById(order.customerId)
                                                            val items = databaseService.getOrderDetails(orderId)

                                                            if (customer != null && items.isNotEmpty()) {

                                                                EmailService.sendReceiptEmail(customer, order, items)
                                                                Log.d("ReceiptSystem", "Background: Receipt sent!")
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        Log.e("ReceiptSystem", "Background Error: ${e.message}")
                                                    }
                                                }
                                                // ----------------------------------------

                                                // 頁面跳轉
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
                                                errorMessage = "Payment not completed. Status: ${captureResponse.status}"
                                                isLoading = false
                                                databaseService.deleteOrderAndRestoreStock(orderId)
                                            }
                                        } else {
                                            errorMessage = "Capture Failed: ${captureResult.exceptionOrNull()?.message}"
                                            isLoading = false
                                            databaseService.deleteOrderAndRestoreStock(orderId)
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Error: ${e.message}"
                                        isLoading = false
                                        databaseService.deleteOrderAndRestoreStock(orderId)
                                    }
                                }
                            }
                            return true
                        }
                        return false
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        isLoading = false
                    }

                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                        super.onReceivedError(view, request, error)
                        Log.e("PayPalWebView", "WebView Error: ${error?.description}")
                        isLoading = false
                    }
                }
                loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxSize()
    )

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color.White)
                Text("Processing PayPal...", color = Color.White, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = {
                errorMessage = null
                (context as? PayPalWebViewActivity)?.finish()
            },
            title = { Text("Payment Error") },
            text = { Text(errorMessage ?: "Unknown error") },
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