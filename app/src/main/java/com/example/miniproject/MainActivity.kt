package com.example.miniproject

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.example.miniproject.paypal.PayPalResultActivity
import com.example.miniproject.ui.theme.MiniProjectTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        actionBar?.hide()

        // Check for PayPal return with order ID
        val orderId = intent?.getStringExtra("ORDER_ID")
        val paymentSuccess = intent?.getBooleanExtra("PAYMENT_SUCCESS", false)

        // Handle deep link from PayPal
        intent?.data?.let { uri ->
            if (uri.scheme == "com.example.miniproject.paypeltest" && uri.host == "demoapp") {
                // This is a PayPal return
                val intent = Intent(this, PayPalResultActivity::class.java)
                intent.data = uri
                startActivity(intent)
                finish()
                return
            }
        }

        setContent {
            MiniProjectTheme {
                // Navigation setup
                val navController = rememberNavController()

                // If we have a successful PayPal payment with order ID, navigate to confirmation
                LaunchedEffect(Unit) {
                    if (paymentSuccess == true && !orderId.isNullOrEmpty()) {
                        navController.navigate("orderConfirmation/$orderId") {
                            popUpTo(0) // Clear back stack
                        }
                    }
                }

                // Call the separate navigation composable
                AppNavigation(navController = navController)
            }
        }
    }
}