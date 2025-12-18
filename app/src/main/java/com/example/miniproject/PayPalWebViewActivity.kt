package com.example.miniproject

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge // 確保引入這個
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

        // 1. 啟用 Edge-to-Edge，讓畫面鋪滿，但我們會在 Compose 中處理避讓
        enableEdgeToEdge()

        val url = intent.getStringExtra("PAYPAL_URL") ?: ""
        val orderId = intent.getStringExtra("ORDER_ID") ?: ""

        setContent {
            MiniProjectTheme {
                // 2. 使用 Scaffold 並利用 contentWindowInsets 來自動處理系統欄高度
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.systemBars // 關鍵：自動計算狀態欄和導航欄的高度
                ) { innerPadding ->
                    // 3. 將 innerPadding 傳遞給 WebView 容器
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
                // --- 4. WebView 設置優化 (關鍵部分) ---
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    // 設置 UserAgent，確保加載移動版網頁
                    userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile; rv:88.0) Gecko/88.0 Firefox/88.0"

                    // 讓網頁適配手機屏幕寬度
                    useWideViewPort = true
                    loadWithOverviewMode = true

                    // 允許縮放 (可選，通常支付頁面不需要)
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        url: String
                    ): Boolean {
                        Log.d("PayPalWebView", "Loading URL: $url")

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
                                                databaseService.updatePaymentStatus(orderId, "completed")
                                                databaseService.updateOrderStatus(orderId, "confirmed")

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
                        // 忽略一些無關緊要的網絡錯誤，避免用戶體驗中斷
                        // errorMessage = "Failed to load PayPal: ${error?.description}"
                        // isLoading = false
                        Log.e("PayPalWebView", "WebView Error: ${error?.description}")
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