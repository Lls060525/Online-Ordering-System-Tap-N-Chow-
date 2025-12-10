
package com.example.miniproject.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.service.DatabaseService
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(navController: NavController) {
    val databaseService = DatabaseService()
    val coroutineScope = rememberCoroutineScope()
    val decimalFormat = DecimalFormat("#,##0.00")

    // State variables
    var totalVendors by remember { mutableStateOf(0) }
    var totalOrders by remember { mutableStateOf(0) }
    var platformRevenue by remember { mutableStateOf(0.0) }
    var totalRevenue by remember { mutableStateOf(0.0) }
    var isLoading by remember { mutableStateOf(true) }

    // Load dashboard data
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                // Get all vendors
                val vendors = databaseService.getAllVendors()
                totalVendors = vendors.size

                // Get all orders
                val orders = databaseService.getAllOrders()
                totalOrders = orders.size

                // Calculate total revenue and platform revenue (10%)
                totalRevenue = orders.sumOf { it.totalPrice }
                platformRevenue = totalRevenue * 0.10

                isLoading = false
            } catch (e: Exception) {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            // Blue background with status bar padding
            Box(
                modifier = Modifier
                    .background(Color(0xFF2196F3)) // Blue color
                    .statusBarsPadding()
            ) {
                TopAppBar(
                    title = {
                        Text(
                            "Tap N Chow - Admin",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White // White text for better contrast on blue
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* Refresh */ }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = {
                            // Logout
                            navController.navigate("adminLogin") {
                                popUpTo("adminDashboard") { inclusive = true }
                            }
                        }) {
                            Icon(
                                Icons.Default.Logout,
                                contentDescription = "Logout",
                                tint = Color.White
                            )
                        }
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
            AdminBottomNavigation(navController)
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Welcome Header
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                "Platform Overview",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Tap N Chow Platform Management",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // Stats Grid
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // First Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Total Vendors Card
                            AdminStatCard(
                                title = "Total Vendors",
                                value = totalVendors.toString(),
                                icon = Icons.Default.Store,
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.weight(1f)
                            )

                            // Total Orders Card
                            AdminStatCard(
                                title = "Total Orders",
                                value = totalOrders.toString(),
                                icon = Icons.Default.Receipt,
                                color = Color(0xFF2196F3),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Second Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Total Revenue Card
                            AdminStatCard(
                                title = "Total Revenue",
                                value = "RM${decimalFormat.format(totalRevenue)}",
                                icon = Icons.Default.AttachMoney,
                                color = Color(0xFF9C27B0),
                                modifier = Modifier.weight(1f)
                            )

                            // Platform Revenue Card
                            AdminStatCard(
                                title = "Platform Revenue (10%)",
                                value = "RM${decimalFormat.format(platformRevenue)}",
                                icon = Icons.Default.AccountBalance,
                                color = Color(0xFFFF9800),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Quick Actions
                item {
                    Text(
                        "Quick Actions",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                    )
                }

                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Vendor Management
                        AdminActionCard(
                            title = "Vendor Management",
                            description = "View, edit, and manage all vendors",
                            icon = Icons.Default.Store,
                            onClick = { navController.navigate("adminVendors") }
                        )

                        // Order Management
                        AdminActionCard(
                            title = "Order Management",
                            description = "View and manage all orders",
                            icon = Icons.Default.Receipt,
                            onClick = { navController.navigate("adminOrders") }
                        )

                        // User Management
                        AdminActionCard(
                            title = "User Management",
                            description = "View and manage customers and vendors",
                            icon = Icons.Default.People,
                            onClick = { navController.navigate("adminUserManagement") }
                        )

                        // Feedback Management
                        AdminActionCard(
                            title = "Feedback Management",
                            description = "View and manage customer feedback",
                            icon = Icons.Default.Reviews,
                            onClick = { navController.navigate("adminFeedback") }
                        )

                        // Sales Analytics
                        AdminActionCard(
                            title = "Platform Analytics",
                            description = "View platform-wide analytics and reports",
                            icon = Icons.Default.Analytics,
                            onClick = { navController.navigate("adminAnalytics") }
                        )
                    }
                }

                // Recent Activity
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Platform Earnings Summary",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    Icons.Default.AccountBalance,
                                    contentDescription = "Earnings",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Earnings breakdown
                            PlatformEarningsBreakdown(
                                platformRevenue = platformRevenue,
                                vendorRevenue = totalRevenue - platformRevenue
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = color
            )
            Text(
                value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun AdminActionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Go",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PlatformEarningsBreakdown(
    platformRevenue: Double,
    vendorRevenue: Double
) {
    val total = platformRevenue + vendorRevenue
    val platformPercentage = if (total > 0) (platformRevenue / total) * 100 else 0.0
    val vendorPercentage = if (total > 0) (vendorRevenue / total) * 100 else 0.0
    val decimalFormat = DecimalFormat("#,##0.00")

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Platform Earnings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color(0xFFFF9800), RoundedCornerShape(4.dp))
                )
                Text("Platform (10%)", fontSize = 14.sp)
            }
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    "RM${decimalFormat.format(platformRevenue)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${String.format("%.1f", platformPercentage)}%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Vendor Earnings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color(0xFF4CAF50), RoundedCornerShape(4.dp))
                )
                Text("Vendors (90%)", fontSize = 14.sp)
            }
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    "RM${decimalFormat.format(vendorRevenue)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${String.format("%.1f", vendorPercentage)}%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(platformPercentage.toFloat() / 100f)
                        .fillMaxHeight()
                        .background(Color(0xFFFF9800), RoundedCornerShape(4.dp))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(vendorPercentage.toFloat() / 100f)
                        .fillMaxHeight()
                        .background(Color(0xFF4CAF50), RoundedCornerShape(4.dp))
                )
            }
        }

        // Total
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Total Revenue:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                "RM${decimalFormat.format(total)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun AdminBottomNavigation(navController: NavController) {
    NavigationBar {
        NavigationBarItem(
            selected = true,
            onClick = { navController.navigate("adminDashboard") },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
            label = { Text("Dashboard") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("adminVendors") },
            icon = { Icon(Icons.Default.Store, contentDescription = "Vendors") },
            label = { Text("Vendors") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("adminUserManagement") },
            icon = { Icon(Icons.Default.People, contentDescription = "Users") },
            label = { Text("Users") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("adminOrders") },
            icon = { Icon(Icons.Default.Receipt, contentDescription = "Orders") },
            label = { Text("Orders") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("adminFeedback") },
            icon = { Icon(Icons.Default.Reviews, contentDescription = "Feedback") },
            label = { Text("Feedback") }
        )

    }
}