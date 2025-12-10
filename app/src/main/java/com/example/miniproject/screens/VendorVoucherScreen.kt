package com.example.miniproject.screens

import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.model.Vendor
import com.example.miniproject.model.Voucher
import com.example.miniproject.service.AuthService
import com.example.miniproject.service.DatabaseService
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun getBitmapFromBase64(base64Str: String): androidx.compose.ui.graphics.ImageBitmap? {
    return try {
        if (base64Str.isBlank()) return null
        val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorVoucherScreen(navController: NavController) {
    val databaseService = DatabaseService()
    val authService = AuthService()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var vouchers by remember { mutableStateOf<List<Voucher>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var vendorId by remember { mutableStateOf("") }

    // Store the full vendor object to pass name/image to the voucher
    var currentVendor by remember { mutableStateOf<Vendor?>(null) }

    // Load Vouchers and Vendor Details
    LaunchedEffect(Unit) {
        val vendor = authService.getCurrentVendor()
        if (vendor != null) {
            currentVendor = vendor
            vendorId = vendor.vendorId
            // Load initial list
            vouchers = databaseService.getVouchersByVendor(vendorId)
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Voucher Management",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFFFA500),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Voucher")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (vouchers.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.LocalOffer,
                        contentDescription = "No Vouchers",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No vouchers created yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 18.sp
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(vouchers) { voucher ->
                        VoucherItemCard(
                            voucher = voucher,
                            onToggleStatus = { isActive ->
                                scope.launch {
                                    // Optimistic update
                                    vouchers = vouchers.map {
                                        if (it.voucherId == voucher.voucherId) it.copy(isActive = isActive) else it
                                    }
                                    // Update Firebase
                                    databaseService.toggleVoucherStatus(voucher.voucherId, isActive)
                                }
                            },
                            onDelete = {
                                scope.launch {
                                    databaseService.deleteVoucher(voucher.voucherId)
                                    // Remove from local list
                                    vouchers = vouchers.filter { it.voucherId != voucher.voucherId }
                                    Toast.makeText(context, "Voucher deleted", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Pass vendor details to the dialog
    if (showAddDialog && currentVendor != null) {
        AddVoucherDialog(
            vendorId = vendorId,
            vendorName = currentVendor!!.vendorName,
            vendorImage = currentVendor!!.profileImageBase64,
            onDismiss = { showAddDialog = false },
            onSuccess = {
                showAddDialog = false
                scope.launch {
                    isLoading = true
                    vouchers = databaseService.getVouchersByVendor(vendorId)
                    isLoading = false
                    Toast.makeText(context, "Voucher created successfully", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
fun VoucherItemCard(
    voucher: Voucher,
    onToggleStatus: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    // Decode image for display
    val vendorImageBitmap = remember(voucher.vendorProfileImage) {
        getBitmapFromBase64(voucher.vendorProfileImage)
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // --- NEW HEADER: Vendor Info ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                // Vendor Profile Image
                Surface(
                    shape = CircleShape,
                    modifier = Modifier.size(32.dp),
                    color = Color.LightGray
                ) {
                    if (vendorImageBitmap != null) {
                        Image(
                            bitmap = vendorImageBitmap,
                            contentDescription = "Vendor Logo",
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Fallback Icon
                        Icon(
                            Icons.Default.Store,
                            contentDescription = null,
                            modifier = Modifier.padding(6.dp),
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Vendor Name
                Text(
                    text = voucher.vendorName.ifBlank { "My Voucher" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray
                )
            }

            Divider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))
            // -------------------------------

            // Voucher Main Content (Code & Switch)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = voucher.code,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (voucher.isActive) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (!voucher.isActive) {
                            Surface(
                                color = Color.Gray.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "INACTIVE",
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    Text(
                        text = if (voucher.discountType == "percentage") "${voucher.discountValue.toInt()}% OFF"
                        else "RM${"%.2f".format(voucher.discountValue)} OFF",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                }

                Switch(
                    checked = voucher.isActive,
                    onCheckedChange = onToggleStatus,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // Footer (Min Spend, Expiry, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Min Spend: RM${"%.2f".format(voucher.minSpend)}", fontSize = 13.sp, color = Color.Gray)
                    Text("Expires: ${dateFormat.format(voucher.expiryDate.toDate())}", fontSize = 13.sp, color = Color.Gray)
                    Text("Usage: ${voucher.usedCount} / ${voucher.usageLimit}", fontSize = 13.sp, color = Color.Gray)
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVoucherDialog(
    vendorId: String,
    vendorName: String, // New param
    vendorImage: String, // New param
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val databaseService = DatabaseService()
    val scope = rememberCoroutineScope()

    // Form State
    var code by remember { mutableStateOf("") }
    var discountType by remember { mutableStateOf("percentage") } // "percentage" or "fixed"
    var discountValue by remember { mutableStateOf("") }
    var minSpend by remember { mutableStateOf("") }
    var usageLimit by remember { mutableStateOf("100") }
    var isLoading by remember { mutableStateOf(false) }

    // Date Picker State
    var showDatePicker by remember { mutableStateOf(false) }
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, 7) // Default 7 days
    var selectedDate by remember { mutableStateOf(calendar.timeInMillis) }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)

    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { selectedDate = it }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = dateState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Voucher", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Voucher Code
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Voucher Code") },
                    placeholder = { Text("e.g. SAVE10") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                )

                // Discount Type Selector
                Column {
                    Text("Discount Type", fontSize = 14.sp, color = Color.Gray)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            modifier = Modifier
                                .clickable { discountType = "percentage" }
                                .padding(end = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = discountType == "percentage",
                                onClick = { discountType = "percentage" }
                            )
                            Text("Percentage (%)")
                        }

                        Row(
                            modifier = Modifier.clickable { discountType = "fixed" },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = discountType == "fixed",
                                onClick = { discountType = "fixed" }
                            )
                            Text("Fixed Amount (RM)")
                        }
                    }
                }

                // Discount Value
                OutlinedTextField(
                    value = discountValue,
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) discountValue = it },
                    label = { Text(if (discountType == "percentage") "Percentage Value" else "Amount Value") },
                    placeholder = { Text(if (discountType == "percentage") "e.g. 10" else "e.g. 5.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Min Spend
                OutlinedTextField(
                    value = minSpend,
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) minSpend = it },
                    label = { Text("Minimum Spend (RM)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Usage Limit
                OutlinedTextField(
                    value = usageLimit,
                    onValueChange = { if (it.all { char -> char.isDigit() }) usageLimit = it },
                    label = { Text("Usage Limit (Qty)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Expiry Date
                OutlinedTextField(
                    value = dateFormatter.format(Date(selectedDate)),
                    onValueChange = { },
                    label = { Text("Expiry Date") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Box(
                    modifier = Modifier.clickable { showDatePicker = true }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (code.isBlank() || discountValue.isBlank()) return@Button

                    val value = discountValue.toDoubleOrNull() ?: 0.0

                    // Validation
                    if (discountType == "percentage" && value > 100) return@Button

                    isLoading = true

                    // Creating new voucher with Vendor Name & Image
                    val newVoucher = Voucher(
                        vendorId = vendorId,
                        vendorName = vendorName,
                        vendorProfileImage = vendorImage,
                        code = code,
                        discountType = discountType,
                        discountValue = value,
                        minSpend = minSpend.toDoubleOrNull() ?: 0.0,
                        usageLimit = usageLimit.toIntOrNull() ?: 100,
                        expiryDate = Timestamp(Date(selectedDate))
                    )

                    scope.launch {
                        databaseService.createVoucher(newVoucher)
                        isLoading = false
                        onSuccess()
                    }
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Create")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}