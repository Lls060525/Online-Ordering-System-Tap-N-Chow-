package com.example.miniproject

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.miniproject.screens.*
import com.example.miniproject.screens.admin.AdminAnalyticsScreen
import com.example.miniproject.screens.admin.AdminDashboardScreen
import com.example.miniproject.screens.admin.AdminFeedbackListScreen
import com.example.miniproject.screens.admin.AdminLoginScreen
import com.example.miniproject.screens.admin.AdminOrderDetailsScreen
import com.example.miniproject.screens.admin.AdminOrderListScreen
import com.example.miniproject.screens.admin.AdminUserManagementScreen
import com.example.miniproject.screens.admin.AdminVendorListScreen
import com.example.miniproject.screens.admin.AdminVendorSalesReportScreen
import com.example.miniproject.screens.customer.CustomerAccountScreen
import com.example.miniproject.screens.customer.CustomerProfileScreen
import com.example.miniproject.screens.customer.CustomerVoucherScreen
import com.example.miniproject.screens.customer.FeedbackScreen
import com.example.miniproject.screens.customer.FoodMenuScreen
import com.example.miniproject.screens.customer.ForgotPasswordScreen
import com.example.miniproject.screens.customer.LoginScreen
import com.example.miniproject.screens.customer.OrderConfirmationScreen
import com.example.miniproject.screens.customer.PaymentGatewayScreen
import com.example.miniproject.screens.customer.RateOrderScreen
import com.example.miniproject.screens.customer.RegisterScreen
import com.example.miniproject.screens.customer.UserHomeScreen
import com.example.miniproject.screens.gamification.ShakeToDecideScreen
import com.example.miniproject.screens.order.OrderHistoryScreen
import com.example.miniproject.screens.order.OrderScreen
import com.example.miniproject.screens.order.OrderTrackingScreen
import com.example.miniproject.screens.vendor.SalesAnalyticsScreen
import com.example.miniproject.screens.vendor.VendorAnalyticsContent
import com.example.miniproject.screens.vendor.VendorFeedbackAnalyticsScreen
import com.example.miniproject.screens.vendor.VendorFeedbackStatisticsScreen
import com.example.miniproject.screens.vendor.VendorHomeScreen
import com.example.miniproject.screens.vendor.VendorLoginScreen
import com.example.miniproject.screens.vendor.VendorProductsContent
import com.example.miniproject.screens.vendor.VendorRegisterScreen
import com.example.miniproject.screens.vendor.VendorReviewsScreen
import com.example.miniproject.screens.vendor.VendorVoucherScreen
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
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    if (paymentSuccess == true && !orderId.isNullOrEmpty()) {
                        navController.navigate("orderConfirmation/$orderId") {
                            popUpTo(0) // Clear back stack
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = "login"
                ) {
                    // --- Authentication ---
                    composable("login") {
                        LoginScreen(navController = navController)
                    }
                    composable("register") {
                        RegisterScreen(navController = navController)
                    }
                    composable("forgotPassword") {
                        ForgotPasswordScreen(navController = navController)
                    }

                    // --- Vendor Authentication ---
                    composable("vendorLogin") {
                        VendorLoginScreen(navController = navController)
                    }
                    composable("vendorRegister") {
                        VendorRegisterScreen(navController = navController)
                    }

                    // --- Admin Authentication ---
                    composable("adminLogin") {
                        AdminLoginScreen(navController)
                    }

                    // --- Customer Main Screens ---
                    composable("home") {
                        UserHomeScreen(navController = navController)
                    }
                    composable("foodMenu/{vendorId}") { backStackEntry ->
                        val vendorId = backStackEntry.arguments?.getString("vendorId")
                        FoodMenuScreen(navController, vendorId)
                    }
                    composable("customer_account") {
                        CustomerAccountScreen(navController = navController)
                    }
                    composable("customerProfile") {
                        CustomerProfileScreen(navController = navController)
                    }
                    composable("customer_vouchers") {
                        CustomerVoucherScreen(navController)
                    }
                    composable("shakeToDecide") {
                        ShakeToDecideScreen(navController)
                    }

                    // --- Ordering Flow ---
                    composable("order_screen"){
                        OrderScreen(navController = navController)
                    }
                    composable("payment/{vendorId}") { backStackEntry ->
                        val vendorId = backStackEntry.arguments?.getString("vendorId")
                        PaymentGatewayScreen(navController, vendorId)
                    }
                    composable("orderConfirmation/{orderId}") { backStackEntry ->
                        val orderId = backStackEntry.arguments?.getString("orderId")
                        OrderConfirmationScreen(navController, orderId)
                    }
                    composable("orderHistory") {
                        OrderHistoryScreen(navController = navController)
                    }
                    composable("tracking/{orderId}") { backStackEntry ->
                        val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                        OrderTrackingScreen(navController, orderId)
                    }
                    composable("rateOrder/{orderId}") { backStackEntry ->
                        val orderId = backStackEntry.arguments?.getString("orderId")
                        RateOrderScreen(navController, orderId)
                    }
                    composable("feedback") {
                        FeedbackScreen(navController = navController)
                    }

                    // --- Vendor Screens ---
                    composable("vendorHome") {
                        VendorHomeScreen(navController = navController)
                    }
                    composable("vendorProduct") {
                        VendorProductsContent(navController = navController)
                    }
                    composable("vendorAnalytics") {
                        VendorAnalyticsContent(navController = navController)
                    }
                    composable("salesAnalytics") {
                        SalesAnalyticsScreen(navController = navController)
                    }
                    composable("vendorReviews") {
                        VendorReviewsScreen(navController = navController)
                    }
                    composable("vendorFeedbackAnalytics") {
                        VendorFeedbackAnalyticsScreen(navController = navController)
                    }
                    composable("vendorFeedbackStatistics") {
                        VendorFeedbackStatisticsScreen(navController = navController)
                    }
                    composable("vendorVouchers") {
                        VendorVoucherScreen(navController)
                    }

                    // --- Admin Screens ---
                    composable("adminDashboard") {
                        AdminDashboardScreen(navController)
                    }
                    composable("adminVendors") {
                        AdminVendorListScreen(navController)
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

                    // Detail Screens for Admin
                    composable("adminVendorSalesReport/{vendorId}") { backStackEntry ->
                        val vendorId = backStackEntry.arguments?.getString("vendorId") ?: ""
                        AdminVendorSalesReportScreen(navController, vendorId)
                    }

                    composable("adminOrderDetails/{orderId}") { backStackEntry ->
                        val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                        AdminOrderDetailsScreen(navController = navController, orderId = orderId)
                    }

                    // Placeholders (only if you have screens for these, otherwise remove)
                    composable("adminVendorDetails/{vendorId}") { backStackEntry ->
                        // Add AdminVendorDetailsScreen here if you have it
                    }
                }
            }
        }
    }
}