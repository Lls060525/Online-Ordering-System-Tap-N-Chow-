// [file name]: VendorAccountScreen.kt
package com.example.miniproject.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.R
import com.example.miniproject.model.Vendor
import com.example.miniproject.service.AuthService
import com.example.miniproject.service.DatabaseService
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.Base64

@Composable
fun VendorAccountScreen(navController: NavController) {
    val authService = AuthService()
    val databaseService = DatabaseService()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var vendor by remember { mutableStateOf<Vendor?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var showImageSourceDialog by remember { mutableStateOf(false) }

    // Image picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                val base64Image = uriToBase64(context, it)
                base64Image?.let { imageString ->
                    vendor?.vendorId?.let { vendorId ->
                        val result = databaseService.updateVendorProfileImageBase64(vendorId, imageString)
                        if (result.isSuccess) {
                            vendor = vendor?.copy(profileImageBase64 = imageString)
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val currentVendor = authService.getCurrentVendor()
            vendor = currentVendor
            isLoading = false
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        VendorAccountContent(
            vendor = vendor,
            onLogout = { showLogoutDialog = true },
            navController = navController,
            isEditing = isEditing,
            onEditToggle = { isEditing = !isEditing },
            onVendorUpdate = { updatedVendor ->
                coroutineScope.launch {
                    val result = databaseService.updateVendorProfile(updatedVendor)
                    if (result.isSuccess) {
                        vendor = updatedVendor
                        isEditing = false
                    }
                }
            },
            onEditProfilePicture = { showImageSourceDialog = true },
            galleryLauncher = galleryLauncher
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Confirm Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        authService.logout()
                        navController.navigate("login") {
                            popUpTo("vendorHome") { inclusive = true }
                        }
                    }
                ) { Text("Logout") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Image Source Dialog
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Change Profile Picture") },
            text = { Text("Choose image source") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImageSourceDialog = false
                        galleryLauncher.launch("image/*")
                    }
                ) { Text("Gallery") }
            },
            dismissButton = {
                TextButton(onClick = { showImageSourceDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun VendorAccountContent(
    vendor: Vendor?,
    onLogout: () -> Unit,
    navController: NavController,
    isEditing: Boolean,
    onEditToggle: () -> Unit,
    onVendorUpdate: (Vendor) -> Unit,
    onEditProfilePicture: () -> Unit,
    galleryLauncher: ActivityResultLauncher<String>
) {
    // Editable fields state
    var editableVendorName by remember { mutableStateOf(vendor?.vendorName ?: "") }
    var editableEmail by remember { mutableStateOf(vendor?.email ?: "") }
    var editableContact by remember { mutableStateOf(vendor?.vendorContact ?: "") }
    var editableAddress by remember { mutableStateOf(vendor?.address ?: "") }

    // Error states
    var vendorNameError by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }
    var contactError by remember { mutableStateOf("") }
    var addressError by remember { mutableStateOf("") }

    var showPaymentDialog by remember { mutableStateOf(false) }

    // Update fields when vendor changes
    LaunchedEffect(vendor) {
        vendor?.let {
            editableVendorName = it.vendorName
            editableEmail = it.email
            editableContact = it.vendorContact
            editableAddress = it.address
        }
    }

    // Clear errors when user starts typing
    LaunchedEffect(editableVendorName) { if (editableVendorName.isNotEmpty()) vendorNameError = "" }
    LaunchedEffect(editableEmail) { if (editableEmail.isNotEmpty()) emailError = "" }
    LaunchedEffect(editableContact) { if (editableContact.isNotEmpty()) contactError = "" }
    LaunchedEffect(editableAddress) { if (editableAddress.isNotEmpty()) addressError = "" }

    fun validateForm(): Boolean {
        var isValid = true

        if (editableVendorName.isEmpty()) {
            vendorNameError = "Vendor name is required"
            isValid = false
        } else if (editableVendorName.length < 2) {
            vendorNameError = "Vendor name must be at least 2 characters"
            isValid = false
        }

        if (editableEmail.isEmpty()) {
            emailError = "Email is required"
            isValid = false
        } else if (!isValidEmail(editableEmail)) {
            emailError = "Please enter a valid email address"
            isValid = false
        }

        if (editableContact.isEmpty()) {
            contactError = "Contact number is required"
            isValid = false
        } else if (!isValidPhoneNumber(editableContact)) {
            contactError = "Please enter a valid phone number"
            isValid = false
        }

        if (editableAddress.isEmpty()) {
            addressError = "Address is required"
            isValid = false
        } else if (editableAddress.length < 10) {
            addressError = "Please enter a complete address"
            isValid = false
        }

        return isValid
    }

    fun saveChanges() {
        if (validateForm() && vendor != null) {
            val updatedVendor = vendor!!.copy(
                vendorName = editableVendorName.trim(),
                email = editableEmail.trim(),
                vendorContact = editableContact.trim(),
                address = editableAddress.trim()
            )
            onVendorUpdate(updatedVendor)
        }
    }

    fun cancelEdit() {
        vendor?.let {
            editableVendorName = it.vendorName
            editableEmail = it.email
            editableContact = it.vendorContact
            editableAddress = it.address
        }
        vendorNameError = ""
        emailError = ""
        contactError = ""
        addressError = ""
        onEditToggle()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header Section
        HeaderSection(
            vendor = vendor,
            isEditing = isEditing,
            onEditToggle = onEditToggle,
            onSave = { saveChanges() },
            onCancel = { cancelEdit() },
            onEditProfilePicture = onEditProfilePicture
        )

        // Payment Methods Action Card (External)
        PaymentMethodActionCard(
            onClick = { showPaymentDialog = true }
        )

        // Vendor Information Card
        // [FIXED]: Correctly passing all parameters including onManagePaymentClick
        VendorInfoCard(
            vendor = vendor,
            isEditing = isEditing,
            editableVendorName = editableVendorName,
            editableEmail = editableEmail,
            editableContact = editableContact,
            editableAddress = editableAddress,
            onVendorNameChange = { newValue -> editableVendorName = newValue },
            onEmailChange = { newValue -> editableEmail = newValue },
            onContactChange = { newValue -> editableContact = newValue },
            onAddressChange = { newValue -> editableAddress = newValue },
            vendorNameError = vendorNameError,
            emailError = emailError,
            contactError = contactError,
            addressError = addressError,
            onManagePaymentClick = { showPaymentDialog = true } // Passed correctly now
        )

        // Navigation Cards
        NavigationCards(navController)

        // Logout Button
        LogoutButton(onLogout)

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showPaymentDialog && vendor != null) {
        PaymentMethodsDialog(
            currentMethods = vendor.acceptedPaymentMethods,
            currentPayPalLink = vendor.paypalLink,
            onDismiss = { showPaymentDialog = false },
            onConfirm = { methods, link ->
                val updatedVendor = vendor.copy(
                    acceptedPaymentMethods = methods,
                    paypalLink = link
                )
                onVendorUpdate(updatedVendor)
                showPaymentDialog = false
            }
        )
    }
}

@Composable
fun HeaderSection(
    vendor: Vendor?,
    isEditing: Boolean,
    onEditToggle: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onEditProfilePicture: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                if (!vendor?.profileImageBase64.isNullOrEmpty()) {
                    val bitmap = base64ToBitmap(vendor?.profileImageBase64 ?: "")
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Vendor Profile",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .clickable { if (!isEditing) onEditProfilePicture() },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.logo2),
                        contentDescription = "Vendor Logo",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .clickable { if (!isEditing) onEditProfilePicture() },
                        contentScale = ContentScale.Crop
                    )
                }

                if (!isEditing) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8F))
                            .clickable { onEditProfilePicture() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile Picture",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = vendor?.vendorName ?: "Vendor Name",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Verified Vendor",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isEditing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = onSave, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Cancel, contentDescription = "Cancel")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel")
                    }
                }
            } else {
                Button(onClick = onEditToggle, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Profile")
                }
            }
        }
    }
}

@Composable
fun VendorInfoCard(
    vendor: Vendor?,
    isEditing: Boolean,
    editableVendorName: String,
    editableEmail: String,
    editableContact: String,
    editableAddress: String,
    onVendorNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onContactChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    vendorNameError: String,
    emailError: String,
    contactError: String,
    addressError: String,
    onManagePaymentClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Vendor Information",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            InfoRow(Icons.Default.Business, "Vendor ID:", vendor?.vendorId ?: "N/A")
            Spacer(modifier = Modifier.height(12.dp))
            EditableInfoRow(
                Icons.Default.Business, "Vendor Name:",
                if (isEditing) editableVendorName else vendor?.vendorName ?: "N/A",
                isEditing, onVendorNameChange, vendorNameError
            )
            Spacer(modifier = Modifier.height(12.dp))
            EditableInfoRow(
                Icons.Default.Email, "Email:",
                if (isEditing) editableEmail else vendor?.email ?: "N/A",
                isEditing, onEmailChange, emailError
            )
            Spacer(modifier = Modifier.height(12.dp))
            EditableInfoRow(
                Icons.Default.Phone, "Contact:",
                if (isEditing) editableContact else vendor?.vendorContact ?: "N/A",
                isEditing, onContactChange, contactError
            )
            Spacer(modifier = Modifier.height(12.dp))
            EditableAddressRow(
                Icons.Default.LocationOn, "Address:",
                if (isEditing) editableAddress else vendor?.address ?: "N/A",
                isEditing, onAddressChange, addressError
            )

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            // --- Payment Methods Section ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Accepted Payment Methods",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (isEditing) {
                    TextButton(onClick = onManagePaymentClick) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add / Edit")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (vendor != null && vendor.acceptedPaymentMethods.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    vendor.acceptedPaymentMethods.forEach { method ->
                        val (label, color) = when (method) {
                            "paypal" -> "PayPal" to Color(0xFF003087)
                            "card" -> "Card" to MaterialTheme.colorScheme.secondary
                            "cash" -> "Cash" to MaterialTheme.colorScheme.tertiary
                            else -> method to Color.Gray
                        }

                        AssistChip(
                            onClick = { },
                            label = { Text(label, fontSize = 12.sp) },
                            leadingIcon = {
                                when (method) {
                                    "paypal" -> Icon(Icons.Default.Payment, null, modifier = Modifier.size(14.dp))
                                    "cash" -> Icon(Icons.Default.Money, null, modifier = Modifier.size(14.dp))
                                    "card" -> Icon(Icons.Default.CreditCard, null, modifier = Modifier.size(14.dp))
                                }
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = color.copy(alpha = 0.1f),
                                labelColor = color,
                                leadingIconContentColor = color
                            ),
                            border = null
                        )
                    }
                }
            } else {
                Text("No payment methods set", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun PaymentMethodActionCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Payment Methods",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Add or manage accepted payments",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(Icons.Default.ChevronRight, "Open", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, label, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun EditableInfoRow(
    icon: ImageVector, label: String, value: String, isEditable: Boolean,
    onValueChange: (String) -> Unit, errorMessage: String = ""
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, label, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            if (isEditable) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errorMessage.isNotEmpty(),
                    supportingText = { if (errorMessage.isNotEmpty()) Text(errorMessage) }
                )
            } else {
                Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun EditableAddressRow(
    icon: ImageVector, label: String, value: String, isEditable: Boolean,
    onValueChange: (String) -> Unit, errorMessage: String = ""
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, label, modifier = Modifier.size(20.dp).padding(top = 2.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            if (isEditable) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3,
                    isError = errorMessage.isNotEmpty(),
                    supportingText = { if (errorMessage.isNotEmpty()) Text(errorMessage) }
                )
            } else {
                Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
fun NavigationCards(navController: NavController) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NavigationCard(
            title = "My Retail",
            description = "Manage your products and inventory",
            onClick = { navController.navigate("vendorProduct") }
        )
        NavigationCard(
            title = "Report",
            description = "View sales reports and analytics",
            onClick = { navController.navigate("vendorAnalytics") }
        )
    }
}

@Composable
fun NavigationCard(title: String, description: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("→", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LogoutButton(onLogout: () -> Unit) {
    Button(
        onClick = onLogout,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
    ) {
        Icon(Icons.Default.ExitToApp, "Logout", modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("LOG OUT", fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PaymentMethodsDialog(
    currentMethods: List<String>,
    currentPayPalLink: String,
    onDismiss: () -> Unit,
    onConfirm: (List<String>, String) -> Unit
) {
    var selectedMethods by remember { mutableStateOf(currentMethods) }
    var payPalLink by remember { mutableStateOf(currentPayPalLink) }

    val options = listOf(
        Triple("paypal", "PayPal", Icons.Default.Payment),
        Triple("card", "Credit / Debit Card", Icons.Default.CreditCard),
        Triple("cash", "Cash on Pickup", Icons.Default.Money)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Payment Methods", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                options.forEach { (id, label, icon) ->
                    val isSelected = selectedMethods.contains(id)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedMethods = if (isSelected) selectedMethods - id else selectedMethods + id
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(icon, null, tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, "Selected", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            if (id == "paypal" && isSelected) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = payPalLink,
                                    onValueChange = { payPalLink = it },
                                    label = { Text("PayPal Business Email") },
                                    placeholder = { Text("e.g. sb-43qb...@business.example.com") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Default.Email, null, modifier = Modifier.size(16.dp)) }
                                )
                                Text(
                                    "Payments will be sent directly to this PayPal account.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedMethods, payPalLink) }) { Text("Save Methods") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// Helpers
private fun isValidEmail(email: String): Boolean {
    val emailRegex = "^[A-Za-z](.*)([@]{1})(.{1,})(\\.)(.{1,})".toRegex()
    return emailRegex.matches(email)
}

private fun isValidPhoneNumber(phone: String): Boolean {
    val phoneRegex = "^[+]?[0-9]{10,15}\$".toRegex()
    return phoneRegex.matches(phone.replace("\\s".toRegex(), ""))
}

private fun uriToBase64(context: Context, uri: Uri): String? {
    return try {
        val bitmap = if (Build.VERSION.SDK_INT < 28) {
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        } else {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        }
        val resizedBitmap = resizeBitmap(bitmap, 400, 400)
        bitmapToBase64(resizedBitmap)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
    var width = bitmap.width
    var height = bitmap.height
    if (width > maxWidth || height > maxHeight) {
        val ratio = width.toFloat() / height.toFloat()
        if (ratio > 1) {
            width = maxWidth
            height = (maxWidth / ratio).toInt()
        } else {
            height = maxHeight
            width = (maxHeight * ratio).toInt()
        }
    }
    return Bitmap.createScaledBitmap(bitmap, width, height, true)
}

private fun bitmapToBase64(bitmap: Bitmap): String {
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
    val byteArray = outputStream.toByteArray()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Base64.getEncoder().encodeToString(byteArray)
    } else {
        android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)
    }
}

private fun base64ToBitmap(base64String: String): Bitmap {
    val imageBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Base64.getDecoder().decode(base64String)
    } else {
        android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
    }
    return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}