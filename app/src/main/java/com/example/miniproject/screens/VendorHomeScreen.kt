package com.example.miniproject.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

sealed class VendorScreen(val title: String, val icon: ImageVector) {
    object Dashboard : VendorScreen("Dashboard", Icons.Default.Store)
    object Products : VendorScreen("Products", Icons.Default.Inventory)
    object Orders : VendorScreen("Orders", Icons.Default.Receipt)
    object Analytics : VendorScreen("Analytics", Icons.Default.Analytics)
    object Account : VendorScreen("Account", Icons.Default.AccountCircle)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorHomeScreen(navController: NavController) {
    var currentScreen by remember { mutableStateOf<VendorScreen>(VendorScreen.Dashboard) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Vendor Dashboard",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar {
                listOf(
                    VendorScreen.Dashboard,
                    VendorScreen.Products,
                    VendorScreen.Orders,
                    VendorScreen.Analytics,
                    VendorScreen.Account
                ).forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                modifier = Modifier.size(24.dp)
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
                is VendorScreen.Dashboard -> VendorDashboardContent()
                is VendorScreen.Products -> VendorProductsContent(navController)
                is VendorScreen.Orders -> VendorOrdersContent()
                is VendorScreen.Analytics -> VendorAnalyticsContent()
                is VendorScreen.Account -> VendorAccountScreen(navController)
            }
        }
    }
}

@Composable
fun VendorDashboardContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Vendor Dashboard",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Welcome to your vendor dashboard",
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun VendorProductsContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Products Management - Coming Soon")
    }
}

@Composable
fun VendorOrdersContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Orders Management - Coming Soon")
    }
}

@Composable
fun VendorAnalyticsContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Analytics - Coming Soon")
    }
}

