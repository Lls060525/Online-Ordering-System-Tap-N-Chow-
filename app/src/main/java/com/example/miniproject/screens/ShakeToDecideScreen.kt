package com.example.miniproject.screens.gamification

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.miniproject.model.Product
import com.example.miniproject.service.DatabaseService
import com.example.miniproject.util.ShakeDetector
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShakeToDecideScreen(navController: NavController) {
    val context = LocalContext.current
    val databaseService = DatabaseService()

    // State
    var allProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
    var recommendedProduct by remember { mutableStateOf<Product?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // --- NEW: State to prevent double clicks (Crash Prevention) ---
    var lastBackClickTime by remember { mutableLongStateOf(0L) }

    // Animation State
    val shakeIconRotation = remember { Animatable(0f) }

    // Sensor Management
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    val vibrator = remember {
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    // Load Products with Strict Filtering
    LaunchedEffect(Unit) {
        isLoading = true
        // 1. Fetch Products
        val products = databaseService.getAllProducts()

        // 2. Fetch Vendors to check who is frozen
        val vendors = databaseService.getAllVendors()

        // 3. Create a list of Valid Vendor IDs (Not Frozen)
        val validVendorIds = vendors.filter { !it.isFrozen }.map { it.vendorId }.toSet()

        // 4. Filter the products
        allProducts = products.filter { product ->
            val hasStock = product.stock > 0
            val isVendorActive = validVendorIds.contains(product.vendorId)
            hasStock && isVendorActive
        }

        isLoading = false
    }

    // Shake Handler
    val shakeDetector = remember {
        ShakeDetector {
            if (!showDialog && allProducts.isNotEmpty()) {
                // 1. Vibrate phone
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(100)
                }

                // 2. Pick Random from the Filtered List
                recommendedProduct = allProducts.random()
                showDialog = true
            }
        }
    }

    // Register/Unregister Sensor
    DisposableEffect(Unit) {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager.unregisterListener(shakeDetector)
        }
    }

    // Idle Animation
    LaunchedEffect(Unit) {
        while (true) {
            shakeIconRotation.animateTo(15f, animationSpec = tween(150))
            shakeIconRotation.animateTo(-15f, animationSpec = tween(150))
            shakeIconRotation.animateTo(0f, animationSpec = tween(150))
            delay(2000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shake to Decide") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            // --- UPDATED: Safe Back Button Logic ---
                            val currentTime = System.currentTimeMillis()
                            // Only allow click if 500ms have passed since the last click
                            if (currentTime - lastBackClickTime > 500) {
                                lastBackClickTime = currentTime
                                navController.popBackStack()
                            }
                        }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4CAF50),
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (allProducts.isEmpty()) {
                // Handle case where no products are available in DB
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.RestaurantMenu, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    Text("No food available to recommend right now!", color = Color.Gray)
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = "Shake Phone",
                        modifier = Modifier
                            .size(120.dp)
                            .rotate(shakeIconRotation.value),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "Can't decide what to eat?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Shake your phone to get a random recommendation!",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    Button(
                        onClick = {
                            if (allProducts.isNotEmpty()) {
                                recommendedProduct = allProducts.random()
                                showDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Or tap here to decide")
                    }
                }
            }
        }

        // Recommendation Dialog
        if (showDialog && recommendedProduct != null) {
            val product = recommendedProduct!!

            AlertDialog(
                onDismissRequest = {
                    showDialog = false
                    recommendedProduct = null
                },
                icon = { Icon(Icons.Default.RestaurantMenu, null) },
                title = {
                    Text(text = "We Recommend...", fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = product.productName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Only RM${"%.2f".format(product.productPrice)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = product.description.take(100),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDialog = false
                            navController.navigate("foodMenu/${product.vendorId}")
                        }
                    ) {
                        Text("Check it out")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            showDialog = false
                            recommendedProduct = null
                        }
                    ) {
                        Text("Nah, Shake Again")
                    }
                }
            )
        }
    }
}