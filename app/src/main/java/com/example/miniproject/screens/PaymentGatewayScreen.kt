package com.example.miniproject.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.Money
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.R
import com.example.miniproject.model.Cart
import com.example.miniproject.model.CustomerAccount
import com.example.miniproject.model.OrderRequest
import com.example.miniproject.service.AuthService
import com.example.miniproject.service.DatabaseService
import com.example.miniproject.utils.ImageConverter
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.net.URLDecoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentGatewayScreen(navController: NavController, vendorId: String?, cartJson: String?) {

    println("DEBUG: vendorId = $vendorId")
    println("DEBUG: cartJson = $cartJson")
    println("DEBUG: cartJson isNullOrEmpty = ${cartJson.isNullOrEmpty()}")

    val authService = AuthService()
    val databaseService = DatabaseService()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val imageConverter = ImageConverter(context)

    var customer by remember { mutableStateOf<com.example.miniproject.model.Customer?>(null) }
    var customerAccount by remember { mutableStateOf<CustomerAccount?>(null) }
    var selectedPaymentMethod by remember { mutableStateOf("wallet") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Parse cart data from JSON
    val cart = remember(cartJson) {
        if (!cartJson.isNullOrEmpty()) {
            try {
                val decodedJson = URLDecoder.decode(cartJson, "UTF-8")
                Json.decodeFromString(Cart.serializer(), decodedJson)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }

    LaunchedEffect(Unit) {
        val currentCustomer = authService.getCurrentCustomer()
        customer = currentCustomer
        currentCustomer?.let {
            val account = databaseService.getCustomerAccount(it.customerId)
            customerAccount = account
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Payment",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        if (cart == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("No cart data found")
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Go Back")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // Vendor Information with Profile Picture
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Vendor Profile Picture with improved handling
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                // Improved image handling
                                when {
                                    !cart.vendorProfileImage.isNullOrEmpty() -> {
                                        // Try to decode the base64 image
                                        val bitmap = imageConverter.base64ToBitmap(cart.vendorProfileImage)
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = "Vendor Profile",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            // If base64 decoding fails, show default icon
                                            Icon(
                                                painter = painterResource(id = R.drawable.logo2),
                                                contentDescription = "Vendor Logo",
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(8.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    else -> {
                                        // No profile image available
                                        Icon(
                                            painter = painterResource(id = R.drawable.logo2),
                                            contentDescription = "Vendor Logo",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(8.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    cart.vendorName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = "Location",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        cart.vendorAddress,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                }

                                if (cart.vendorContact.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Phone,
                                            contentDescription = "Contact",
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "Contact: ${cart.vendorContact}",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = "Rating",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "4.5 ★ (128 reviews)",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Ready for pickup in 10-15 mins",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Order Summary
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Your Order",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Order Items
                        cart.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        item.productName,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "Qty: ${item.quantity}",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    "RM ${"%.2f".format(item.productPrice * item.quantity)}",
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Divider()
                        Spacer(modifier = Modifier.height(12.dp))

                        // Total Calculation
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal")
                            Text("RM ${"%.2f".format(cart.subtotal)}")
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Service Fee (10%)")
                            Text("RM ${"%.2f".format(cart.serviceFee)}")
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tax (8%)")
                            Text("RM ${"%.2f".format(cart.tax)}")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                "RM ${"%.2f".format(cart.total)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Payment Method
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Payment Method",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Credit/Debit Card Option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedPaymentMethod == "card",
                                    onClick = { selectedPaymentMethod = "card" }
                                )
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPaymentMethod == "card",
                                onClick = { selectedPaymentMethod = "card" }
                            )
                            Icon(Icons.Default.CreditCard, contentDescription = "Card")
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Credit / Debit Card")
                        }

                        // Tap N Chow Wallet Option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedPaymentMethod == "wallet",
                                    onClick = { selectedPaymentMethod = "wallet" }
                                )
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPaymentMethod == "wallet",
                                onClick = { selectedPaymentMethod = "wallet" }
                            )
                            Icon(Icons.Default.Wallet, contentDescription = "Wallet")
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Tap N Chow Wallet")
                            Spacer(modifier = Modifier.weight(1f))
                            customerAccount?.let { account ->
                                Text(
                                    "Balance: RM ${"%.2f".format(account.tapNChowCredit)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Cash on Pickup Option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedPaymentMethod == "cash",
                                    onClick = { selectedPaymentMethod = "cash" }
                                )
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPaymentMethod == "cash",
                                onClick = { selectedPaymentMethod = "cash" }
                            )
                            Icon(Icons.Default.Money, contentDescription = "Cash")
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Cash on Pickup")
                        }
                    }
                }

                // Error message
                errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                // Complete Order Button
                Button(
                    onClick = {
                        if (customer == null) {
                            errorMessage = "Please log in to complete order"
                            return@Button
                        }

                        if (selectedPaymentMethod == "wallet" && customerAccount != null) {
                            if (customerAccount!!.tapNChowCredit < cart.total) {
                                errorMessage = "Insufficient wallet balance"
                                return@Button
                            }
                        }

                        isLoading = true
                        coroutineScope.launch {
                            try {
                                val orderRequest = OrderRequest(
                                    customerId = customer!!.customerId,
                                    vendorId = cart.vendorId,
                                    items = cart.items,
                                    totalAmount = cart.total,
                                    deliveryAddress = "Store Pickup",
                                    paymentMethod = selectedPaymentMethod
                                )

                                val result = databaseService.createOrderWithDetails(orderRequest)

                                if (result.isSuccess) {
                                    // Update wallet balance if paid with wallet
                                    if (selectedPaymentMethod == "wallet" && customerAccount != null) {
                                        val newBalance = customerAccount!!.tapNChowCredit - cart.total
                                        databaseService.updateCustomerCredit(customer!!.customerId, newBalance)
                                    }

                                    // Update payment status
                                    databaseService.updatePaymentStatus(result.getOrThrow(), "completed")

                                    navController.navigate("orderConfirmation/${result.getOrThrow()}") {
                                        popUpTo("userHome") { inclusive = false }
                                    }
                                } else {
                                    errorMessage = "Failed to create order: ${result.exceptionOrNull()?.message}"
                                }
                            } catch (e: Exception) {
                                errorMessage = "Error: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    enabled = !isLoading && cart.items.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            "Complete Order",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}