package com.example.miniproject.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
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

    // --- NEW: State for Logout Dialog ---
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Load dashboard data
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val vendors = databaseService.getAllVendors()
                totalVendors = vendors.size
                val orders = databaseService.getAllOrders()
                totalOrders = orders.size
                totalRevenue = orders.sumOf { it.totalPrice }
                platformRevenue = totalRevenue * 0.10
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
            }
        }
    }

    Scaffold(
        bottomBar = {
            AdminBottomNavigation(navController)
        },
        containerColor = Color(0xFFF5F6F9)
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Top Gradient Background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF2196F3), // Blue 500
                                Color(0xFF1976D2)  // Blue 700
                            )
                        ),
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = paddingValues.calculateBottomPadding()),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // --- Header Section ---
                    item {
                        Column(modifier = Modifier.padding(top = 20.dp, bottom = 10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Dashboard",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Overview & Statistics",
                                        fontSize = 14.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                                // --- Logout Button triggers dialog ---
                                IconButton(
                                    onClick = { showLogoutDialog = true },
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                        .size(40.dp)
                                ) {
                                    Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color.White)
                                }
                            }
                        }
                    }

                    // --- Key Stats Grid (2x2) ---
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                ModernStatCard(
                                    title = "Total Orders",
                                    value = totalOrders.toString(),
                                    icon = Icons.Default.ReceiptLong,
                                    iconColor = Color(0xFF2196F3),
                                    modifier = Modifier.weight(1f)
                                )
                                ModernStatCard(
                                    title = "Total Vendors",
                                    value = totalVendors.toString(),
                                    icon = Icons.Default.Storefront,
                                    iconColor = Color(0xFF4CAF50),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                ModernStatCard(
                                    title = "Total Revenue",
                                    value = "RM${decimalFormat.format(totalRevenue)}",
                                    icon = Icons.Default.Payments,
                                    iconColor = Color(0xFF9C27B0),
                                    modifier = Modifier.weight(1f)
                                )
                                ModernStatCard(
                                    title = "Platform (10%)",
                                    value = "RM${decimalFormat.format(platformRevenue)}",
                                    icon = Icons.Default.AccountBalanceWallet,
                                    iconColor = Color(0xFFFF9800),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // --- Quick Actions Grid ---
                    item {
                        Text(
                            "Quick Management",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333),
                            modifier = Modifier.padding(top = 10.dp, bottom = 5.dp)
                        )
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Row 1
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                ModernActionCard(
                                    title = "Vendors",
                                    icon = Icons.Default.Store,
                                    color = Color(0xFF5C6BC0),
                                    onClick = { navController.navigate("adminVendors") },
                                    modifier = Modifier.weight(1f)
                                )
                                ModernActionCard(
                                    title = "Orders",
                                    icon = Icons.Default.Receipt,
                                    color = Color(0xFFEC407A),
                                    onClick = { navController.navigate("adminOrders") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Row 2
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                ModernActionCard(
                                    title = "Users",
                                    icon = Icons.Default.Group,
                                    color = Color(0xFF26A69A),
                                    onClick = { navController.navigate("adminUserManagement") },
                                    modifier = Modifier.weight(1f)
                                )
                                ModernActionCard(
                                    title = "Analytics",
                                    icon = Icons.Default.BarChart,
                                    color = Color(0xFFFFA726),
                                    onClick = { navController.navigate("adminAnalytics") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Row 3
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                ModernActionCard(
                                    title = "Feedback",
                                    icon = Icons.Default.Reviews,
                                    color = Color(0xFF78909C),
                                    onClick = { navController.navigate("adminFeedback") },
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    // --- Earnings Summary ---
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    "Earnings Breakdown",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                PlatformEarningsBreakdown(
                                    platformRevenue = platformRevenue,
                                    vendorRevenue = totalRevenue - platformRevenue
                                )
                            }
                        }
                    }
                }
            }

            // --- Logout Confirmation Dialog ---
            if (showLogoutDialog) {
                AlertDialog(
                    onDismissRequest = { showLogoutDialog = false },
                    title = { Text(text = "Confirm Logout") },
                    text = { Text("Are you sure you want to log out from the admin dashboard?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showLogoutDialog = false
                                navController.navigate("adminLogin") {
                                    popUpTo("adminDashboard") { inclusive = true }
                                }
                            }
                        ) {
                            Text("Logout", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showLogoutDialog = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
// --- UI Components ---

@Composable
fun ModernStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // 更明显的阴影
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon in a subtle circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Value
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                lineHeight = 24.sp
            )
            // Title
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF9E9E9E)
            )
        }
    }
}

@Composable
fun ModernActionCard(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(90.dp) // Fixed height for uniformity
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Colorful Icon Box
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF424242)
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
                        .weight(platformPercentage.toFloat())
                        .fillMaxHeight()
                        .background(Color(0xFFFF9800), RoundedCornerShape(4.dp))
                )
                Box(
                    modifier = Modifier
                        .weight(vendorPercentage.toFloat())
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

// Data class for navigation items
data class NavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

@Composable
fun AdminBottomNavigation(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val navItems = listOf(
        NavItem("adminDashboard", Icons.Default.Dashboard, "Dashboard"),
        NavItem("adminVendors", Icons.Default.Store, "Vendors"),
        NavItem("adminUserManagement", Icons.Default.People, "Users"),
        NavItem("adminOrders", Icons.Default.Receipt, "Orders"),
        NavItem("adminFeedback", Icons.Default.Reviews, "Feedback"),
        NavItem("adminAnalytics", Icons.Default.Analytics, "Analytics")
    )

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp,
        modifier = Modifier.shadow(8.dp)
    ) {
        navItems.forEach { item ->
            val isSelected = currentDestination?.route == item.route
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        navController.navigate(item.route) {
                            popUpTo(currentDestination?.route ?: "adminDashboard") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        tint = if (isSelected) Color(0xFF2196F3) else Color.Gray
                    )
                },
                label = { Text(item.label, fontSize = 10.sp, color = if (isSelected) Color(0xFF2196F3) else Color.Gray) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0xFFE3F2FD)
                )
            )
        }
    }
}