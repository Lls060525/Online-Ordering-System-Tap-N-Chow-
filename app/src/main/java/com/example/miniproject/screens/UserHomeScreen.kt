package com.example.miniproject.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.screens.order.OrderScreen
import com.example.miniproject.service.AuthService
import kotlinx.coroutines.launch

// Fixed screen definitions to match your screenshot
sealed class UserScreen(val title: String, val icon: ImageVector) {
    object Food : UserScreen("Vendor List", Icons.Default.Store)
    object Feedback : UserScreen("Feedback", Icons.Default.Feedback) // Changed from Grocery to Feedback
    object MyOrder : UserScreen("My Order", Icons.Default.ShoppingCart)
    object Account : UserScreen("Account", Icons.Default.AccountCircle)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserHomeScreen(navController: NavController) {
    var currentScreen by remember { mutableStateOf<UserScreen>(UserScreen.Food) }

    Scaffold(
        topBar = {
            // Green background with status bar padding
            Box(
                modifier = Modifier
                    .background(Color(0xFF4CAF50)) // Green color matching your app
                    .statusBarsPadding()
            ) {
                TopAppBar(
                    title = {
                        Text(
                            "Tap N Chow",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White // White text for better contrast on green
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent, // Make the TopAppBar transparent
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),
                    modifier = Modifier.background(Color.Transparent)
                )
            }
        },
        bottomBar = {
            NavigationBar {
                listOf(
                    UserScreen.Food,
                    UserScreen.Feedback, // This should point to FeedbackScreen
                    UserScreen.MyOrder,
                    UserScreen.Account
                ).forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title) },
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen }
                    )
                }
            }

        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentScreen) {
                is UserScreen.Food -> FoodContentWithVendors(navController)
                is UserScreen.Feedback -> FeedbackScreen(navController) // Show FeedbackScreen
                is UserScreen.MyOrder -> MyOrderContent(navController)
                is UserScreen.Account -> AccountContent(navController)
            }
        }
    }
}

@Composable
fun FoodContent(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "Search for shop & restaurant...",
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Location
        Text(
            text = "location...",
            modifier = Modifier.padding(top = 8.dp, start = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )

        // Restaurants List
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp)
        ) {
            listOf("").forEach { restaurant ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = restaurant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}


@Composable
fun MyOrderContent(navController: NavController) {
    OrderScreen(navController)
}

@Composable
fun AccountContent(navController: NavController) {
    val authService = AuthService()
    val coroutineScope = rememberCoroutineScope()

    var userType by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val customer = authService.getCurrentCustomer()
            val vendor = authService.getCurrentVendor()

            userType = when {
                customer != null -> "customer"
                vendor != null -> "vendor"
                else -> null
            }
        }
    }

    when (userType) {
        "customer" -> CustomerAccountScreen(navController) // Use CustomerProfileScreen
        "vendor" -> VendorAccountScreen(navController) // CORRECTED: Use VendorAccountScreen
        else -> {
            // Show loading or redirect to login
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}