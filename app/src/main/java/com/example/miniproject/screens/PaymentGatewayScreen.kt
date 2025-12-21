package com.example.miniproject.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.PayPalWebViewActivity
import com.example.miniproject.R
import com.example.miniproject.model.CartRepository
import com.example.miniproject.model.CustomerAccount
import com.example.miniproject.model.OrderRequest
import com.example.miniproject.model.Voucher
import com.example.miniproject.service.AuthService
import com.example.miniproject.service.BiometricAuthService
import com.example.miniproject.service.DatabaseService
import com.example.miniproject.service.PayPalService
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date


class PaymentImageConverter(private val context: android.content.Context) {
    fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            val pureBase64 = if (base64String.contains(",")) {
                base64String.substringAfter(",")
            } else {
                base64String
            }
            val decodedBytes = Base64.decode(pureBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e("PaymentImageConverter", "Error decoding base64: ${e.message}")
            null
        }
    }
}

data class Bank(val id: String, val name: String, val code: String)
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
fun PaymentGatewayScreen(navController: NavController, vendorId: String?) {
    val authService = AuthService()
    val databaseService = DatabaseService()
    val payPalService = PayPalService()
    val context = LocalContext.current
    val biometricAuthService = remember { BiometricAuthService(context) }
    val coroutineScope = rememberCoroutineScope()
    val imageConverter = remember { PaymentImageConverter(context) }

    // User & Vendor State
    var customer by remember { mutableStateOf<com.example.miniproject.model.Customer?>(null) }
    var customerAccount by remember { mutableStateOf<CustomerAccount?>(null) }
    var vendor by remember { mutableStateOf<com.example.miniproject.model.Vendor?>(null) }
    var isFetchingVendor by remember { mutableStateOf(false) }
    var vendorBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Payment State
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedPaymentMethod by remember { mutableStateOf("") }

    // Voucher State
    var availableVouchers by remember { mutableStateOf<List<Voucher>>(emptyList()) }
    var selectedVoucher by remember { mutableStateOf<Voucher?>(null) }
    var showVoucherDialog by remember { mutableStateOf(false) }

    // Card Form State
    var selectedBank by remember { mutableStateOf<Bank?>(null) }
    var cardHolderName by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var cardExpiryMonth by remember { mutableStateOf("") }
    var cardExpiryYear by remember { mutableStateOf("") }
    var cardCVV by remember { mutableStateOf("") }
    var showCardForm by remember { mutableStateOf(false) }
    var isBankDropdownExpanded by remember { mutableStateOf(false) }

    // Validation States
    var isExpiryMonthValid by remember { mutableStateOf(true) }
    var isExpiryYearValid by remember { mutableStateOf(true) }
    var isExpiryDateValid by remember { mutableStateOf(true) }
    var isCVVValid by remember { mutableStateOf(true) }

    // Retrieve Cart from Singleton Repository
    val cart = remember { CartRepository.getCart() }

    // --- FETCH DATA ---
    LaunchedEffect(vendorId) {
        if (!vendorId.isNullOrEmpty()) {
            isFetchingVendor = true
            try {
                vendor = databaseService.getVendorById(vendorId)
                vendor?.let { vendorData ->
                    if (!vendorData.profileImageBase64.isNullOrEmpty()) {
                        vendorBitmap = imageConverter.base64ToBitmap(vendorData.profileImageBase64)
                    }

                    // Logic to auto-select payment method based on vendor settings
                    val acceptedMethods = vendorData.acceptedPaymentMethods
                    if (acceptedMethods.isNotEmpty()) {
                        if (selectedPaymentMethod.isEmpty() || !acceptedMethods.contains(selectedPaymentMethod)) {
                            // Priority: PayPal -> Card -> Cash
                            selectedPaymentMethod = when {
                                acceptedMethods.contains("paypal") -> "paypal"
                                acceptedMethods.contains("card") -> "card"
                                acceptedMethods.contains("cash") -> "cash"
                                else -> acceptedMethods.first()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isFetchingVendor = false
            }
        }
    }

    LaunchedEffect(Unit) {
        val currentCustomer = authService.getCurrentCustomer()
        customer = currentCustomer
        currentCustomer?.let {
            customerAccount = databaseService.getCustomerAccount(it.customerId)
        }
    }

    LaunchedEffect(cart, customer) {
        if (cart != null && customer != null) {
            val allVouchers = databaseService.getVouchersByVendor(cart.vendorId)
            val usedVoucherIds = databaseService.getCustomerUsedVoucherIds(customer!!.customerId)
            val now = Date().time / 1000

            availableVouchers = allVouchers.filter { voucher ->
                val expirySeconds = voucher.expiryDate.seconds
                val isNotUsed = !usedVoucherIds.contains(voucher.voucherId)
                val isNotExpired = expirySeconds > now
                val isMinSpendMet = cart.subtotal >= voucher.minSpend
                val isActive = voucher.isActive
                val isUsageLimitNotReached = voucher.usedCount < voucher.usageLimit
                isActive && isNotExpired && isMinSpendMet && isNotUsed && isUsageLimitNotReached
            }
        }
    }

    // --- CALCULATIONS ---
    val discountAmount = remember(cart, selectedVoucher) {
        if (cart == null || selectedVoucher == null) 0.0
        else {
            if (selectedVoucher!!.discountType == "percentage") {
                (cart.subtotal * (selectedVoucher!!.discountValue / 100)).coerceAtMost(cart.subtotal)
            } else {
                selectedVoucher!!.discountValue.coerceAtMost(cart.subtotal)
            }
        }
    }

    val finalTotal = remember(cart, discountAmount) {
        if (cart == null) 0.0
        else (cart.total - discountAmount).coerceAtLeast(0.0)
    }

    // Validation Helpers
    LaunchedEffect(selectedPaymentMethod) { showCardForm = selectedPaymentMethod == "card" }
    val validateExpiryMonth = { month: String -> if (month.isNotEmpty()) { val m = month.toIntOrNull(); isExpiryMonthValid = m in 1..12; isExpiryMonthValid } else { isExpiryMonthValid = true; true } }
    val validateExpiryYear = { year: String -> if (year.isNotEmpty()) { val cy = Calendar.getInstance().get(Calendar.YEAR) % 100; val y = year.toIntOrNull(); isExpiryYearValid = y != null && y >= cy; isExpiryYearValid } else { isExpiryYearValid = true; true } }
    val validateExpiryDateNotPast = { month: String, year: String -> if (month.isNotEmpty() && year.isNotEmpty()) { val mn = month.toIntOrNull(); val yn = year.toIntOrNull(); val cy = Calendar.getInstance().get(Calendar.YEAR) % 100; val cm = Calendar.getInstance().get(Calendar.MONTH) + 1; isExpiryDateValid = if (yn == cy) (mn != null && mn >= cm) else true; isExpiryDateValid } else { isExpiryDateValid = true; true } }
    val validateCVV = { cvv: String -> if (cvv.isNotEmpty()) { isCVVValid = cvv.length == 3 && cvv.all { it.isDigit() }; isCVVValid } else { isCVVValid = true; true } }
    val isCardDetailsValid = { selectedBank != null && cardHolderName.isNotBlank() && cardNumber.length == 16 && cardExpiryMonth.length == 2 && cardExpiryYear.length == 2 && cardCVV.length == 3 && validateExpiryMonth(cardExpiryMonth) && validateExpiryYear(cardExpiryYear) && validateExpiryDateNotPast(cardExpiryMonth, cardExpiryYear) && validateCVV(cardCVV) }


    // --- PAYMENT LOGIC FUNCTIONS ---
    fun processPayPalPayment() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val orderRequest = OrderRequest(
                    customerId = customer!!.customerId,
                    vendorId = cart!!.vendorId,
                    items = cart.items,
                    totalAmount = finalTotal,
                    deliveryAddress = "Store Pickup",
                    paymentMethod = "paypal",
                    discountAmount = discountAmount,
                    voucherCode = selectedVoucher?.code
                )

                val createOrderResult = databaseService.createOrderWithDetails(orderRequest)
                if (createOrderResult.isSuccess) {
                    val orderId = createOrderResult.getOrThrow()
                    if (selectedVoucher != null) {
                        databaseService.trackVoucherUsage(customer!!.customerId, selectedVoucher!!.voucherId)
                    }

                    // --- KEY CHANGE HERE ---
                    // Get vendor email from the loaded vendor object
                    // We treat the 'paypalLink' field as the storage for the PayPal Business Email
                    val vendorEmail = vendor?.paypalLink

                    val paypalResult = payPalService.createOrder(
                        amount = finalTotal,
                        orderId = orderId,
                        description = "Order from ${cart.vendorName}",
                        vendorPayPalEmail = null
                    )

                    if (paypalResult.isSuccess) {
                        val paypalOrder = paypalResult.getOrThrow()
                        val approvalLink = paypalOrder.links.find { it.rel == "approve" }
                        if (approvalLink != null) {
                            databaseService.updateOrderWithPayPalId(orderId, paypalOrder.id, "")
                            val intent = Intent(context, PayPalWebViewActivity::class.java).apply {
                                putExtra("PAYPAL_URL", approvalLink.href)
                                putExtra("ORDER_ID", orderId)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                            isLoading = false
                        } else { errorMessage = "PayPal URL not found"; isLoading = false }
                    } else { errorMessage = "PayPal failed: ${paypalResult.exceptionOrNull()?.message}"; isLoading = false }
                } else { errorMessage = "Order failed: ${createOrderResult.exceptionOrNull()?.message}"; isLoading = false }
            } catch (e: Exception) { errorMessage = "Error: ${e.message}"; isLoading = false }
        }
    }

    fun processRegularPayment() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val orderRequest = OrderRequest(
                    customerId = customer!!.customerId,
                    vendorId = cart!!.vendorId,
                    items = cart.items,
                    totalAmount = finalTotal,
                    deliveryAddress = "Store Pickup",
                    paymentMethod = selectedPaymentMethod,
                    discountAmount = discountAmount,
                    voucherCode = selectedVoucher?.code
                )

                val result = databaseService.createOrderWithDetails(orderRequest)

                if (result.isSuccess) {
                    val orderId = result.getOrThrow()
                    databaseService.updatePaymentStatus(orderId, "completed")
                    databaseService.updateOrderStatus(orderId, "confirmed")

                    if (selectedVoucher != null) {
                        databaseService.trackVoucherUsage(customer!!.customerId, selectedVoucher!!.voucherId)
                    }

                    // Clear cart after successful payment
                    CartRepository.clear()

                    navController.navigate("orderConfirmation/${orderId}") {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                } else { errorMessage = "Order failed: ${result.exceptionOrNull()?.message}" }
            } catch (e: Exception) { errorMessage = "Error: ${e.message}" }
            finally { isLoading = false }
        }
    }

    fun initiatePayment() {
        if (customer == null) { errorMessage = "Please log in"; return }
        if (cart == null) { errorMessage = "Cart is empty"; return }
        if (selectedPaymentMethod == "card" && !isCardDetailsValid()) { errorMessage = "Check card details"; return }
        isLoading = true
        coroutineScope.launch {
            val status = biometricAuthService.checkBiometricAvailability()

            if (status == BiometricAuthService.BiometricStatus.AVAILABLE) {
                val authenticated = biometricAuthService.authorizePayment(finalTotal)

                if (authenticated) {
                    if (selectedPaymentMethod == "paypal") processPayPalPayment()
                    else processRegularPayment()
                } else {
                    Toast.makeText(context, "Authentication Cancelled", Toast.LENGTH_SHORT).show()
                    isLoading = false
                }
            } else {
                if (selectedPaymentMethod == "paypal") processPayPalPayment()
                else processRegularPayment()
            }
        }
    }

    // --- UI CONTENT ---
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Payment", fontWeight = FontWeight.Bold) },
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
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No cart data found. Please go back to menu.")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // --- Vendor Info Card ---
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            if (vendorBitmap != null) {
                                Image(
                                    bitmap = vendorBitmap!!.asImageBitmap(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.logo2),
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(vendor?.vendorName ?: cart.vendorName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(vendor?.address ?: cart.vendorAddress, fontSize = 14.sp, color = Color.Gray)
                        }
                    }
                }

                // Order Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Order Summary", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        cart.items.forEach { item ->
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text("${item.quantity}x ${item.productName}", fontSize = 14.sp)
                                Text("RM ${"%.2f".format(item.productPrice * item.quantity)}", fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Subtotal", fontSize = 14.sp, color = Color.Gray)
                            Text("RM ${"%.2f".format(cart.subtotal)}", fontSize = 14.sp)
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Service Fee", fontSize = 14.sp, color = Color.Gray)
                            Text("RM ${"%.2f".format(cart.serviceFee)}", fontSize = 14.sp)
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Tax", fontSize = 14.sp, color = Color.Gray)
                            Text("RM ${"%.2f".format(cart.tax)}", fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showVoucherDialog = true }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocalOffer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))

                            if (selectedVoucher != null) {
                                Column {
                                    Text("Voucher Applied: ${selectedVoucher!!.code}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("- RM ${"%.2f".format(discountAmount)}", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Text("Change", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            } else {
                                Text("Apply Voucher", fontSize = 14.sp)
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text("Total", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Column(horizontalAlignment = Alignment.End) {
                                if (discountAmount > 0) {
                                    Text(
                                        "RM ${"%.2f".format(cart.total)}",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        style = androidx.compose.ui.text.TextStyle(textDecoration = TextDecoration.LineThrough)
                                    )
                                }
                                Text(
                                    "RM ${"%.2f".format(finalTotal)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Payment Method Card
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Payment Method", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        val methods = listOf(
                            Triple("paypal", "PayPal", Icons.Default.Payment),
                            Triple("card", "Credit / Debit Card", Icons.Default.CreditCard),
                            Triple("cash", "Cash on Pickup", Icons.Default.Money)
                        )

                        val supportedMethods = vendor?.acceptedPaymentMethods ?: emptyList()

                        methods.forEach { (id, name, icon) ->
                            val isSupported = supportedMethods.contains(id)
                            val alpha = if (isSupported) 1f else 0.4f

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (isSupported) {
                                            Modifier.selectable(
                                                selected = selectedPaymentMethod == id,
                                                onClick = { selectedPaymentMethod = id }
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .padding(vertical = 12.dp)
                                    .graphicsLayer(alpha = alpha),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedPaymentMethod == id,
                                    onClick = {
                                        if (isSupported) selectedPaymentMethod = id
                                    },
                                    enabled = isSupported
                                )
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = if(isSupported) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(name, fontSize = 16.sp, color = if(isSupported) Color.Unspecified else Color.Gray)

                                    if (!isSupported) {
                                        Text(
                                            "Not accepted by vendor",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }

                        if (supportedMethods.isEmpty() && !isFetchingVendor) {
                            Text(
                                "This vendor has not set up payment methods yet.",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                // Card Form
                if (showCardForm && selectedPaymentMethod == "card") {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Card Details", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(16.dp))

                            ExposedDropdownMenuBox(
                                expanded = isBankDropdownExpanded,
                                onExpandedChange = { isBankDropdownExpanded = !isBankDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedBank?.name ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isBankDropdownExpanded) },
                                    placeholder = { Text("Select Bank") },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                )
                                ExposedDropdownMenu(expanded = isBankDropdownExpanded, onDismissRequest = { isBankDropdownExpanded = false }) {
                                    malaysianBanks.forEach { bank ->
                                        DropdownMenuItem(text = { Text(bank.name) }, onClick = { selectedBank = bank; isBankDropdownExpanded = false })
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(value = cardHolderName, onValueChange = { cardHolderName = it }, label = { Text("Holder Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = cardNumber,
                                onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 16) cardNumber = it },
                                label = { Text("Card Number") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                visualTransformation = CardNumberTransformation()
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = cardExpiryMonth,
                                    onValueChange = { if (it.length <= 2) { cardExpiryMonth = it; validateExpiryMonth(it) } },
                                    label = { Text("MM") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    isError = !isExpiryMonthValid
                                )
                                OutlinedTextField(
                                    value = cardExpiryYear,
                                    onValueChange = { if (it.length <= 2) { cardExpiryYear = it; validateExpiryYear(it) } },
                                    label = { Text("YY") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    isError = !isExpiryYearValid
                                )
                                OutlinedTextField(
                                    value = cardCVV,
                                    onValueChange = { if (it.length <= 3) { cardCVV = it; validateCVV(it) } },
                                    label = { Text("CVV") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    isError = !isCVVValid
                                )
                            }
                        }
                    }
                }

                errorMessage?.let { msg ->
                    Box(Modifier.fillMaxWidth().padding(16.dp).background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp)).padding(12.dp)) {
                        Text(msg, color = MaterialTheme.colorScheme.onErrorContainer, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }

                // Payment Button
                Button(
                    onClick = { initiatePayment() },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                    // Check if a valid payment method is selected
                    enabled = !isLoading && selectedPaymentMethod.isNotEmpty() && (selectedPaymentMethod != "card" || isCardDetailsValid())
                ) {
                    if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("Pay RM ${"%.2f".format(finalTotal)}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Voucher Dialog
    if (showVoucherDialog) {
        AlertDialog(
            onDismissRequest = { showVoucherDialog = false },
            title = { Text("Select Voucher") },
            text = {
                if (availableVouchers.isEmpty()) {
                    Text("No vouchers available for this order (Min spend not met or already used).")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(availableVouchers) { voucher ->
                            Card(
                                onClick = { selectedVoucher = voucher; showVoucherDialog = false },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedVoucher?.voucherId == voucher.voucherId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(voucher.code, fontWeight = FontWeight.Bold)
                                        Text(
                                            if (voucher.discountType == "percentage") "${voucher.discountValue.toInt()}% OFF" else "RM${"%.2f".format(voucher.discountValue)} OFF",
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text("Min Spend: RM${"%.2f".format(voucher.minSpend)}", fontSize = 12.sp, color = Color.Gray)
                                    }
                                    if (selectedVoucher?.voucherId == voucher.voucherId) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showVoucherDialog = false }) { Text("Close") } },
            dismissButton = {
                if (selectedVoucher != null) {
                    TextButton(onClick = { selectedVoucher = null; showVoucherDialog = false }) {
                        Text("Remove Voucher", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        )
    }
}

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
                return if (offset <= 3) offset else if (offset <= 7) offset + 1 else if (offset <= 11) offset + 2 else if (offset <= 16) offset + 3 else offset + 3
            }
            override fun transformedToOriginal(offset: Int): Int {
                return if (offset <= 4) offset else if (offset <= 9) offset - 1 else if (offset <= 14) offset - 2 else if (offset <= 19) offset - 3 else offset - 3
            }
        }
        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}