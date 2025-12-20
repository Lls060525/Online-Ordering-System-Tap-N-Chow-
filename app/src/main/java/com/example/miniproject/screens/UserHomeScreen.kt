package com.example.miniproject.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.screens.order.OrderScreen
import com.example.miniproject.service.AuthService
import kotlinx.coroutines.launch

// Fixed screen definitions
sealed class UserScreen(val title: String, val icon: ImageVector) {
    object Food : UserScreen("Vendor List", Icons.Default.Store)
    object Feedback : UserScreen("Feedback", Icons.Default.Feedback)
    object MyOrder : UserScreen("My Order", Icons.Default.ShoppingCart)
    object Account : UserScreen("Account", Icons.Default.AccountCircle)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserHomeScreen(navController: NavController) {
    // 1. Define the list of screens to preserve order
    val screens = listOf(
        UserScreen.Food,
        UserScreen.Feedback,
        UserScreen.MyOrder,
        UserScreen.Account
    )

    // 2. Use rememberSaveable with an Integer to save the tab state across navigation
    var currentScreenIndex by rememberSaveable { mutableIntStateOf(0) }

    // 3. Get the current screen object based on the saved index
    val currentScreen = screens[currentScreenIndex]

    // --- State for Spin Feature ---
    var showSpinDialog by remember { mutableStateOf(false) }
    var currentCustomerId by remember { mutableStateOf<String?>(null) }
    val authService = remember { AuthService() }
    val context = LocalContext.current

    // --- Fetch Customer ID for the Spin Feature ---
    LaunchedEffect(Unit) {
        val customer = authService.getCurrentCustomer()
        currentCustomerId = customer?.customerId
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .background(Color(0xFF4CAF50))
                    .statusBarsPadding()
            ) {
                TopAppBar(
                    title = {
                        Text(
                            "Tap N Chow",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),
                    modifier = Modifier.background(Color.Transparent)
                )
            }
        },
        // --- Floating Action Button for Daily Spin ---
        floatingActionButton = {
            // Only show on Food screen
            if (currentScreen == UserScreen.Food && currentCustomerId != null) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Shake Button (Small)
                    SmallFloatingActionButton(
                        onClick = { navController.navigate("shakeToDecide") },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Icon(Icons.Default.Vibration, contentDescription = "Shake")
                    }

                    // 2. Daily Spin Button (Extended)
                    ExtendedFloatingActionButton(
                        onClick = { showSpinDialog = true },
                        containerColor = Color(0xFFFFD700),
                        contentColor = Color.Black,
                        icon = { Icon(Icons.Default.Star, contentDescription = "Win Prizes") },
                        text = { Text("Daily Spin", fontWeight = FontWeight.Bold) }
                    )
                }
            }
        },



        bottomBar = {
            NavigationBar {
                // 4. Iterate using index to match the state
                screens.forEachIndexed { index, screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title) },
                        selected = currentScreenIndex == index, // Check if this index matches state
                        onClick = { currentScreenIndex = index } // Update the integer state
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
                is UserScreen.Feedback -> FeedbackScreen(navController)
                is UserScreen.MyOrder -> MyOrderContent(navController)
                is UserScreen.Account -> AccountContent(navController)
            }

            // --- The Spin Dialog Overlay ---
            if (showSpinDialog && currentCustomerId != null) {
                DailySpinDialog(
                    customerId = currentCustomerId!!,
                    onDismiss = { showSpinDialog = false },
                    onCoinsWon = { amount ->
                        Toast.makeText(context, "You won $amount Coins!", Toast.LENGTH_LONG).show()
                        showSpinDialog = false
                    }
                )
            }
        }
    }
}

// ... Rest of your existing content ...

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
        "customer" -> CustomerAccountScreen(navController)
        "vendor" -> VendorAccountScreen(navController)
        else -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}