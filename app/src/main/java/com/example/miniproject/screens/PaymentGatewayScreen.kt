package com.example.miniproject.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.R
import com.example.miniproject.PayPalWebViewActivity
import com.example.miniproject.model.Cart
import com.example.miniproject.model.CustomerAccount
import com.example.miniproject.model.OrderRequest
import com.example.miniproject.service.AuthService
import com.example.miniproject.service.DatabaseService
import com.example.miniproject.service.PayPalService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.util.Calendar

// Image Converter for Payment Screen
class PaymentImageConverter(private val context: android.content.Context) {
    fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            // Remove data URL prefix if present
            val pureBase64 = if (base64String.contains(",")) {
                base64String.substringAfter(",")
            } else {
                base64String
            }

            Log.d("PaymentImageConverter", "Base64 string length: ${pureBase64.length}")

            val decodedBytes = Base64.decode(pureBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e("PaymentImageConverter", "Error decoding base64: ${e.message}")
            null
        }
    }
}

// Malaysian banks data
data class Bank(
    val id: String,
    val name: String,
    val code: String
)

val malaysianBanks = listOf(
    Bank("maybank", "Maybank", "MB"),
    Bank("public", "Public Bank", "PB"),
    Bank("cimb", "CIMB Bank", "CIMB"),
    Bank("rhb", "RHB Bank", "RHB"),
    Bank("hongleong", "Hong Leong Bank", "HLB"),
    Bank("ambank", "AmBank", "AMB"),
    Bank("uob", "UOB Malaysia", "UOB"),
    Bank("ocbc", "OCBC Bank Malaysia", "OCBC"),
    Bank("standard", "Standard Chartered Malaysia", "SC"),
    Bank("bankislam", "Bank Islam", "BIMB"),
    Bank("muamalat", "Bank Muamalat", "BMMB"),
    Bank("affin", "Affin Bank", "AFFIN"),
    Bank("alliance", "Alliance Bank", "ABMB"),
    Bank("bankrakyat", "Bank Rakyat", "BR"),
    Bank("bsn", "Bank Simpanan Nasional", "BSN")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentGatewayScreen(navController: NavController, vendorId: String?, cartJson: String?) {

    println("DEBUG: vendorId = $vendorId")
    println("DEBUG: cartJson = $cartJson")
    println("DEBUG: cartJson isNullOrEmpty = ${cartJson.isNullOrEmpty()}")

    val authService = AuthService()
    val databaseService = DatabaseService()
    val payPalService = PayPalService()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val imageConverter = remember { PaymentImageConverter(context) }

    var customer by remember { mutableStateOf<com.example.miniproject.model.Customer?>(null) }
    var customerAccount by remember { mutableStateOf<CustomerAccount?>(null) }
    var selectedPaymentMethod by remember { mutableStateOf("wallet") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Vendor data state
    var vendor by remember { mutableStateOf<com.example.miniproject.model.Vendor?>(null) }
    var isFetchingVendor by remember { mutableStateOf(false) }
    var vendorBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Card details state
    var selectedBank by remember { mutableStateOf<Bank?>(null) }
    var cardHolderName by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var cardExpiryMonth by remember { mutableStateOf("") }
    var cardExpiryYear by remember { mutableStateOf("") }
    var cardCVV by remember { mutableStateOf("") }
    var showCardForm by remember { mutableStateOf(false) }

    // Dropdown states
    var isBankDropdownExpanded by remember { mutableStateOf(false) }

    // Validation states
    var isExpiryMonthValid by remember { mutableStateOf(true) }
    var isExpiryYearValid by remember { mutableStateOf(true) }
    var isExpiryDateValid by remember { mutableStateOf(true) } // For future date validation
    var isCVVValid by remember { mutableStateOf(true) }

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

    // Fetch vendor data dynamically when vendorId is available
    LaunchedEffect(vendorId) {
        if (!vendorId.isNullOrEmpty()) {
            isFetchingVendor = true
            try {
                Log.d("PaymentScreen", "Fetching vendor data for ID: $vendorId")
                vendor = databaseService.getVendorById(vendorId)

                // Process vendor image
                vendor?.let { vendorData ->
                    Log.d("PaymentScreen", "Fetched vendor: ${vendorData.vendorName}")
                    Log.d("PaymentScreen", "Vendor has profile image: ${vendorData.profileImageBase64?.isNotEmpty()}")

                    if (!vendorData.profileImageBase64.isNullOrEmpty()) {
                        val bitmap = imageConverter.base64ToBitmap(vendorData.profileImageBase64)
                        vendorBitmap = bitmap
                        Log.d("PaymentScreen", "Vendor bitmap created from database: ${bitmap != null}")
                    } else {
                        Log.d("PaymentScreen", "No vendor profile image in database")
                    }
                }
            } catch (e: Exception) {
                Log.e("PaymentScreen", "Error fetching vendor: ${e.message}")
                e.printStackTrace()
            } finally {
                isFetchingVendor = false
            }
        }
    }

    // Fetch customer data
    LaunchedEffect(Unit) {
        val currentCustomer = authService.getCurrentCustomer()
        customer = currentCustomer
        currentCustomer?.let {
            val account = databaseService.getCustomerAccount(it.customerId)
            customerAccount = account
        }
    }

    // Update card form visibility when payment method changes
    LaunchedEffect(selectedPaymentMethod) {
        showCardForm = selectedPaymentMethod == "card"
    }

    // Helper function to validate expiry month
    val validateExpiryMonth = { month: String ->
        if (month.isNotEmpty()) {
            val monthNum = month.toIntOrNull()
            val isValid = monthNum in 1..12
            isExpiryMonthValid = isValid
            isValid
        } else {
            isExpiryMonthValid = true
            true
        }
    }

    // Helper function to validate expiry year
    val validateExpiryYear = { year: String ->
        if (year.isNotEmpty()) {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR) % 100 // Get last 2 digits (25 for 2025)
            val yearNum = year.toIntOrNull()
            val isValid = yearNum != null && yearNum >= currentYear
            isExpiryYearValid = isValid
            isValid
        } else {
            isExpiryYearValid = true
            true
        }
    }

    // Helper function to validate expiry date not in past
    val validateExpiryDateNotPast = { month: String, year: String ->
        if (month.isNotEmpty() && year.isNotEmpty()) {
            val monthNum = month.toIntOrNull()
            val yearNum = year.toIntOrNull()
            val currentYear = Calendar.getInstance().get(Calendar.YEAR) % 100
            val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1 // January is 0

            val isValid = if (yearNum == currentYear) {
                // If same year, month must be >= current month
                monthNum != null && monthNum >= currentMonth
            } else {
                // If future year, any month is valid
                true
            }
            isExpiryDateValid = isValid
            isValid
        } else {
            isExpiryDateValid = true
            true
        }
    }

    // Helper function to validate CVV
    val validateCVV = { cvv: String ->
        if (cvv.isNotEmpty()) {
            val isValid = cvv.length == 3 && cvv.all { it.isDigit() }
            isCVVValid = isValid
            isValid
        } else {
            isCVVValid = true
            true
        }
    }

    // Helper function to validate all card details
    val isCardDetailsValid = {
        selectedBank != null &&
                cardHolderName.isNotBlank() &&
                cardNumber.length == 16 &&
                cardExpiryMonth.length == 2 &&
                cardExpiryYear.length == 2 &&
                cardCVV.length == 3 &&
                validateExpiryMonth(cardExpiryMonth) &&
                validateExpiryYear(cardExpiryYear) &&
                validateExpiryDateNotPast(cardExpiryMonth, cardExpiryYear) &&
                validateCVV(cardCVV)
    }

    // Helper function to handle PayPal payment
    fun handlePayPalPayment() {
        if (customer == null) {
            errorMessage = "Please log in to complete order"
            return
        }

        if (cart == null) {
            errorMessage = "Cart is empty"
            return
        }

        coroutineScope.launch {
            isLoading = true
            errorMessage = null

            try {
                // First create the order in your database with "pending" status
                val orderRequest = OrderRequest(
                    customerId = customer!!.customerId,
                    vendorId = cart.vendorId,
                    items = cart.items,
                    totalAmount = cart.total,
                    deliveryAddress = "Store Pickup",
                    paymentMethod = "paypal"
                )

                val createOrderResult = databaseService.createOrderWithDetails(orderRequest)

                if (createOrderResult.isSuccess) {
                    val orderId = createOrderResult.getOrThrow()
                    Log.d("PayPalPayment", "Created order in database: $orderId")

                    // Create PayPal order
                    val paypalResult = payPalService.createOrder(
                        amount = cart.total,
                        orderId = orderId,
                        description = "Food Order from ${cart.vendorName}"
                    )

                    if (paypalResult.isSuccess) {
                        val paypalOrder = paypalResult.getOrThrow()
                        Log.d("PayPalPayment", "Created PayPal order: ${paypalOrder.id}")

                        // Find the approval URL
                        val approvalLink = paypalOrder.links.find { it.rel == "approve" }

                        if (approvalLink != null) {
                            Log.d("PayPalPayment", "Approval URL: ${approvalLink.href}")

                            // Save PayPal order ID to your database
                            databaseService.updateOrderWithPayPalId(
                                orderId = orderId,
                                paypalOrderId = paypalOrder.id,
                                paypalPayerId = ""
                            )

                            // Open PayPal in WebView
                            val intent = Intent(context, PayPalWebViewActivity::class.java).apply {
                                putExtra("PAYPAL_URL", approvalLink.href)
                                putExtra("ORDER_ID", orderId)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)

                            // Don't navigate yet - wait for PayPal to return
                            isLoading = false

                        } else {
                            errorMessage = "PayPal approval URL not found"
                            isLoading = false
                            Log.e("PayPalPayment", "No approval link found")
                        }
                    } else {
                        errorMessage = "Failed to create PayPal order: ${paypalResult.exceptionOrNull()?.message}"
                        isLoading = false
                        Log.e("PayPalPayment", "PayPal order creation failed: ${paypalResult.exceptionOrNull()}")
                    }
                } else {
                    errorMessage = "Failed to create order: ${createOrderResult.exceptionOrNull()?.message}"
                    isLoading = false
                    Log.e("PayPalPayment", "Database order creation failed: ${createOrderResult.exceptionOrNull()}")
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
                isLoading = false
                Log.e("PayPalPayment", "Exception: ${e.message}", e)
            }
        }
    }

    // Helper function to handle regular payments
    fun handleRegularPayment() {
        if (customer == null) {
            errorMessage = "Please log in to complete order"
            return
        }

        if (selectedPaymentMethod == "card" && !isCardDetailsValid()) {
            errorMessage = "Please fill all card details correctly"
            return
        }

        if (selectedPaymentMethod == "wallet" && customerAccount != null) {
            if (customerAccount!!.tapNChowCredit < cart!!.total) {
                errorMessage = "Insufficient wallet balance"
                return
            }
        }

        if (cart == null) {
            errorMessage = "Cart is empty"
            return
        }

        coroutineScope.launch {
            isLoading = true
            errorMessage = null

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
                    val orderId = result.getOrThrow()

                    // Update payment status and order status
                    databaseService.updatePaymentStatus(orderId, "completed")
                    databaseService.updateOrderStatus(orderId, "confirmed")

                    if (selectedPaymentMethod == "wallet" && customerAccount != null) {
                        val newBalance = customerAccount!!.tapNChowCredit - cart.total
                        databaseService.updateCustomerCredit(customer!!.customerId, newBalance)
                    }

                    // Navigate to confirmation
                    navController.navigate("orderConfirmation/${orderId}") {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
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
                            // Vendor Profile Picture
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isFetchingVendor) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(30.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    // Use vendor's actual profile image from database
                                    val currentVendorBitmap = vendorBitmap
                                    if (currentVendorBitmap != null) {
                                        Image(
                                            bitmap = currentVendorBitmap.asImageBitmap(),
                                            contentDescription = "Vendor Profile",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        // Fallback to cart image or default
                                        if (!cart.vendorProfileImage.isNullOrEmpty()) {
                                            val cartBitmap = imageConverter.base64ToBitmap(cart.vendorProfileImage)
                                            if (cartBitmap != null) {
                                                Image(
                                                    bitmap = cartBitmap.asImageBitmap(),
                                                    contentDescription = "Vendor Profile",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                // Show default vendor image
                                                Image(
                                                    painter = painterResource(id = R.drawable.logo2),
                                                    contentDescription = "Vendor Logo",
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(12.dp),
                                                    contentScale = ContentScale.Fit
                                                )
                                            }
                                        } else {
                                            // Show default vendor image
                                            Image(
                                                painter = painterResource(id = R.drawable.logo2),
                                                contentDescription = "Vendor Logo",
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(12.dp),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    // Use vendor name from database if available, otherwise from cart
                                    vendor?.vendorName ?: cart.vendorName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Location - Use vendor address from database if available
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = "Location",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        // Use vendor address from database if available
                                        vendor?.address ?: cart.vendorAddress,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Contact - Use vendor contact from database if available
                                val vendorContact = vendor?.vendorContact ?: cart.vendorContact
                                if (vendorContact.isNotEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Phone,
                                            contentDescription = "Contact",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Contact: $vendorContact",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                // Rating - Use vendor rating from database if available
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = "Rating",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        // Use actual vendor rating from database if available
                                        if (vendor?.rating ?: 0.0 > 0) {
                                            "${"%.1f".format(vendor?.rating ?: 0.0)} ★ (${vendor?.reviewCount ?: 0} reviews)"
                                        } else {
                                            "4.5 ★ (128 reviews)" // Fallback rating
                                        },
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Ready for pickup
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                "Ready for pickup in 10-15 mins",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
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
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Order Items
                        cart.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        item.productName,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        "Qty: ${item.quantity}",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    "RM ${"%.2f".format(item.productPrice * item.quantity)}",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Divider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outline
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Total Calculation
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal", fontSize = 14.sp)
                            Text("RM ${"%.2f".format(cart.subtotal)}", fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Service Fee (10%)", fontSize = 14.sp)
                            Text("RM ${"%.2f".format(cart.serviceFee)}", fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tax (8%)", fontSize = 14.sp)
                            Text("RM ${"%.2f".format(cart.tax)}", fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Divider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outline
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Total",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "RM ${"%.2f".format(cart.total)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
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
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // PayPal Option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedPaymentMethod == "paypal",
                                    onClick = { selectedPaymentMethod = "paypal" }
                                )
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPaymentMethod == "paypal",
                                onClick = { selectedPaymentMethod = "paypal" }
                            )
                            Icon(
                                Icons.Default.Payment,
                                contentDescription = "PayPal",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "PayPal",
                                fontSize = 16.sp
                            )
                        }

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
                            Icon(
                                Icons.Default.CreditCard,
                                contentDescription = "Card",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Credit / Debit Card",
                                fontSize = 16.sp
                            )
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
                            Icon(
                                Icons.Default.Wallet,
                                contentDescription = "Wallet",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Tap N Chow Wallet",
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            customerAccount?.let { account ->
                                Text(
                                    "Balance: RM ${"%.2f".format(account.tapNChowCredit)}",
                                    fontSize = 14.sp,
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
                            Icon(
                                Icons.Default.Money,
                                contentDescription = "Cash",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Cash on Pickup",
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                // Card Details Form (shown only when card payment is selected)
                if (showCardForm) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Card Details",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Bank Name - Now a Dropdown Menu
                            Text(
                                "Bank Name",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            ExposedDropdownMenuBox(
                                expanded = isBankDropdownExpanded,
                                onExpandedChange = { isBankDropdownExpanded = !isBankDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedBank?.name ?: "",
                                    onValueChange = {}, // No direct text input
                                    readOnly = true,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                            expanded = isBankDropdownExpanded
                                        )
                                    },
                                    placeholder = { Text("Select your bank") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                )

                                ExposedDropdownMenu(
                                    expanded = isBankDropdownExpanded,
                                    onDismissRequest = { isBankDropdownExpanded = false }
                                ) {
                                    malaysianBanks.forEach { bank ->
                                        DropdownMenuItem(
                                            text = { Text(bank.name) },
                                            onClick = {
                                                selectedBank = bank
                                                isBankDropdownExpanded = false
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Card Holder Name
                            OutlinedTextField(
                                value = cardHolderName,
                                onValueChange = { cardHolderName = it },
                                label = { Text("Card Holder Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Card Number
                            OutlinedTextField(
                                value = cardNumber,
                                onValueChange = {
                                    if (it.all { char -> char.isDigit() } && it.length <= 16) {
                                        cardNumber = it
                                    }
                                },
                                label = { Text("Card Number") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                visualTransformation = CardNumberTransformation(),
                                placeholder = { Text("1234 5678 9012 3456") },
                                shape = RoundedCornerShape(8.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Expiry Date and CVV in one row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Expiry Month
                                OutlinedTextField(
                                    value = cardExpiryMonth,
                                    onValueChange = {
                                        if (it.all { char -> char.isDigit() } && it.length <= 2) {
                                            cardExpiryMonth = it
                                            validateExpiryMonth(it)
                                            validateExpiryDateNotPast(it, cardExpiryYear)
                                        }
                                    },
                                    label = { Text("MM") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    placeholder = { Text("12") },
                                    shape = RoundedCornerShape(8.dp),
                                    isError = !isExpiryMonthValid || !isExpiryDateValid,
                                    supportingText = {
                                        if (!isExpiryMonthValid) {
                                            Text("Month must be 01-12")
                                        } else if (!isExpiryDateValid) {
                                            Text("Card is expired")
                                        }
                                    }
                                )

                                // Expiry Year
                                OutlinedTextField(
                                    value = cardExpiryYear,
                                    onValueChange = {
                                        if (it.all { char -> char.isDigit() } && it.length <= 2) {
                                            cardExpiryYear = it
                                            validateExpiryYear(it)
                                            validateExpiryDateNotPast(cardExpiryMonth, it)
                                        }
                                    },
                                    label = { Text("YY") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    placeholder = {
                                        val currentYear = Calendar.getInstance().get(Calendar.YEAR) % 100
                                        Text(currentYear.toString().padStart(2, '0'))
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    isError = !isExpiryYearValid || !isExpiryDateValid,
                                    supportingText = {
                                        if (!isExpiryYearValid) {
                                            val currentYear = Calendar.getInstance().get(Calendar.YEAR) % 100
                                            Text("Year must be $currentYear or later")
                                        } else if (!isExpiryDateValid) {
                                            Text("Card is expired")
                                        }
                                    }
                                )

                                // CVV - Now only 3 digits
                                OutlinedTextField(
                                    value = cardCVV,
                                    onValueChange = {
                                        if (it.all { char -> char.isDigit() } && it.length <= 3) {
                                            cardCVV = it
                                            validateCVV(it)
                                        }
                                    },
                                    label = { Text("CVV") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    placeholder = { Text("123") },
                                    shape = RoundedCornerShape(8.dp),
                                    isError = !isCVVValid,
                                    supportingText = {
                                        if (!isCVVValid) {
                                            Text("CVV must be 3 digits")
                                        }
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Card validation message
                            if (selectedPaymentMethod == "card" && !isCardDetailsValid()) {
                                Text(
                                    text = "Please fill all card details correctly",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 14.sp
                                )
                            }

                            // Specific validation messages
                            if (selectedPaymentMethod == "card") {
                                if (selectedBank == null) {
                                    Text(
                                        text = "Please select a bank",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 12.sp
                                    )
                                }
                                if (cardHolderName.isBlank()) {
                                    Text(
                                        text = "Please enter card holder name",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 12.sp
                                    )
                                }
                                if (cardNumber.length != 16) {
                                    Text(
                                        text = "Card number must be 16 digits",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 12.sp
                                    )
                                }
                                if (cardExpiryMonth.length != 2 || cardExpiryYear.length != 2) {
                                    Text(
                                        text = "Please enter valid expiry date (MM/YY)",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 12.sp
                                    )
                                }
                                if (cardCVV.length != 3) {
                                    Text(
                                        text = "CVV must be 3 digits",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Error message
                errorMessage?.let { message ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .background(
                                MaterialTheme.colorScheme.errorContainer,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Complete Order Button
                Button(
                    onClick = {
                        when (selectedPaymentMethod) {
                            "paypal" -> handlePayPalPayment()
                            else -> handleRegularPayment()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    enabled = !isLoading && cart.items.isNotEmpty() &&
                            (selectedPaymentMethod != "card" || isCardDetailsValid()),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            "Complete Order - RM ${"%.2f".format(cart.total)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// Fixed CardNumberTransformation implementation
class CardNumberTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 16) text.text.take(16) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i % 4 == 3 && i != 15) out += " "
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return if (offset <= 3) offset
                else if (offset <= 7) offset + 1
                else if (offset <= 11) offset + 2
                else if (offset <= 16) offset + 3
                else offset + 3
            }

            override fun transformedToOriginal(offset: Int): Int {
                return if (offset <= 4) offset
                else if (offset <= 9) offset - 1
                else if (offset <= 14) offset - 2
                else if (offset <= 19) offset - 3
                else offset - 3
            }
        }

        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}