package com.example.miniproject

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.miniproject.screens.admin.*
import com.example.miniproject.screens.customer.*
import com.example.miniproject.screens.gamification.ShakeToDecideScreen
import com.example.miniproject.screens.order.*
import com.example.miniproject.screens.vendor.*

@Composable
fun AppNavigation(navController: NavHostController) {
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
    }
}