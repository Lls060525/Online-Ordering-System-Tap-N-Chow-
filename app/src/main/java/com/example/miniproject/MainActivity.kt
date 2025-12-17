package com.example.miniproject

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.miniproject.screens.CustomerAccountScreen
import com.example.miniproject.screens.CustomerProfileScreen
import com.example.miniproject.screens.FeedbackScreen
import com.example.miniproject.screens.FoodMenuScreen
import com.example.miniproject.screens.LoginScreen
import com.example.miniproject.screens.OrderConfirmationScreen
import com.example.miniproject.screens.PaymentGatewayScreen
import com.example.miniproject.screens.RateOrderScreen
import com.example.miniproject.screens.RegisterScreen
import com.example.miniproject.screens.SalesAnalyticsScreen
import com.example.miniproject.screens.UserHomeScreen
import com.example.miniproject.screens.VendorAnalyticsContent
import com.example.miniproject.screens.VendorFeedbackAnalyticsScreen
import com.example.miniproject.screens.VendorFeedbackStatisticsScreen
import com.example.miniproject.screens.VendorHomeScreen
import com.example.miniproject.screens.VendorLoginScreen
import com.example.miniproject.screens.VendorProductsContent
import com.example.miniproject.screens.VendorRegisterScreen
import com.example.miniproject.screens.VendorReviewsScreen
import com.example.miniproject.screens.AdminAnalyticsScreen
import com.example.miniproject.screens.AdminDashboardScreen
import com.example.miniproject.screens.AdminFeedbackListScreen
import com.example.miniproject.screens.AdminOrderListScreen
import com.example.miniproject.screens.AdminUserManagementScreen
import com.example.miniproject.screens.AdminVendorListScreen
import com.example.miniproject.screens.AdminVendorSalesReportScreen
import com.example.miniproject.screens.AdminLoginScreen
import com.example.miniproject.screens.AdminOrderDetailsScreen
import com.example.miniproject.screens.ForgotPasswordScreen
import com.example.miniproject.screens.order.OrderHistoryScreen
import com.example.miniproject.ui.theme.MiniProjectTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.example.miniproject.screens.CustomerVoucherScreen
import com.example.miniproject.screens.VendorVoucherScreen

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
                if (paymentSuccess == true && !orderId.isNullOrEmpty()) {
                    LaunchedEffect(Unit) {
                        navController.navigate("orderConfirmation/$orderId") {
                            popUpTo(0) // Clear back stack
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = "login"
                ) {
                    // Add this route to your navigation setup
                    composable("adminVendorSalesReport/{vendorId}") { backStackEntry ->
                        val vendorId = backStackEntry.arguments?.getString("vendorId") ?: ""
                        AdminVendorSalesReportScreen(navController, vendorId)
                    }
                    // Admin routes
                    composable("adminLogin") {
                        AdminLoginScreen(navController)
                    }
                    composable("adminDashboard") {
                        AdminDashboardScreen(navController)
                    }
                    composable("adminVendors") {
                        AdminVendorListScreen(navController)
                    }

                    composable("customer_vouchers") {
                        CustomerVoucherScreen(navController)
                    }

                    composable("vendorAccountSettings") {
                        // You'll need to create this screen or point it to a settings page
                        // VendorAccountSettingsScreen(navController)

                        // For now, you can just show a placeholder text if the screen doesn't exist yet
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Account Settings Page")
                        }
                    }

                    composable("vendorVouchers") {
                        VendorVoucherScreen(navController)
                    }

                    composable("forgotPassword") {
                        ForgotPasswordScreen(navController = navController)
                    }

                    composable("adminUserManagement") {
                        AdminUserManagementScreen(navController)
                    }
                    composable("adminOrders") {
                        AdminOrderListScreen(navController)
                    }
                    composable("adminFeedback") {
                        AdminFeedbackListScreen(navController)
                    }
                    composable("adminAnalytics") {
                        AdminAnalyticsScreen(navController)
                    }
                    composable("adminVendorDetails/{vendorId}") { backStackEntry ->
                        // You can create a detailed vendor view screen
                    }
                    composable("adminOrderDetails/{orderId}") { backStackEntry ->
                        // You can create a detailed order view screen
                    }
                    composable("login") {
                        LoginScreen(navController = navController)
                    }
                    composable("register") {
                        RegisterScreen(navController = navController)
                    }
                    composable("vendorRegister") {
                        VendorRegisterScreen(navController = navController)
                    }
                    composable("vendorLogin") {
                        VendorLoginScreen(navController = navController)
                    }
                    composable("home") {
                        UserHomeScreen(navController = navController)
                    }
                    composable("vendorHome") {
                        VendorHomeScreen(navController = navController)
                    }
                    // In your NavHost setup
                    composable("customer_account") {
                        CustomerAccountScreen(navController = navController)
                    }

                    composable("customerProfile") {
                        CustomerProfileScreen(navController = navController)
                    }

                    composable("foodMenu/{vendorId}") { backStackEntry ->
                        val vendorId = backStackEntry.arguments?.getString("vendorId")
                        FoodMenuScreen(navController, vendorId)

                    }

                    composable("foodMenu/{vendorId}") { backStackEntry ->
                        val vendorId = backStackEntry.arguments?.getString("vendorId")
                        FoodMenuScreen(navController, vendorId)
                    }

                    composable("payment/{vendorId}/{cartJson}") { backStackEntry ->
                        val vendorId = backStackEntry.arguments?.getString("vendorId")
                        val cartJson = backStackEntry.arguments?.getString("cartJson")
                        PaymentGatewayScreen(navController, vendorId, cartJson)
                    }

                    composable("orderConfirmation/{orderId}") { backStackEntry ->
                        val orderId = backStackEntry.arguments?.getString("orderId")
                        OrderConfirmationScreen(navController, orderId)
                    }

                    // Add these composables to your NavHost in MainActivity.kt
                    composable("feedback") {
                        FeedbackScreen(navController = navController)
                    }

                    composable("rateOrder/{orderId}") { backStackEntry ->
                        val orderId = backStackEntry.arguments?.getString("orderId")
                        RateOrderScreen(navController, orderId)
                    }

                    composable("rateOrder/{orderId}") { backStackEntry ->
                        val orderId = backStackEntry.arguments?.getString("orderId")
                        RateOrderScreen(navController, orderId)
                    }

                    composable("orderConfirmation/{orderId}") { backStackEntry ->
                        val orderId = backStackEntry.arguments?.getString("orderId")
                        OrderConfirmationScreen(navController, orderId)
                    }

                    composable("home") {
                        UserHomeScreen(navController = navController)
                    }

                    composable("vendorReviews") {
                        VendorReviewsScreen(navController = navController)
                    }
                    composable("vendorFeedbackAnalytics") {
                        VendorFeedbackAnalyticsScreen(navController = navController)
                    }

                    composable("orderHistory") {
                        OrderHistoryScreen(navController = navController)
                    }

                    composable("vendorProduct") {
                        VendorProductsContent(navController = navController)
                    }

                    composable("vendorAnalytics") {
                        VendorAnalyticsContent(navController = navController)
                    }
                    composable("vendorFeedbackStatistics") {
                        VendorFeedbackStatisticsScreen(navController = navController)
                    }

                    composable("salesAnalytics") {
                        SalesAnalyticsScreen(navController = navController)
                    }

                    composable("adminOrderDetails/{orderId}") { backStackEntry ->
                        val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                        AdminOrderDetailsScreen(navController = navController, orderId = orderId)
                    }

                }
            }
        }
    }
}