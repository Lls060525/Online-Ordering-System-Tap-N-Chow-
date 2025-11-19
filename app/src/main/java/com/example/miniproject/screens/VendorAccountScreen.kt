package com.example.miniproject.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.asImageBitmap
import com.example.miniproject.R
import com.example.miniproject.model.Vendor
import com.example.miniproject.service.AuthService
import com.example.miniproject.service.DatabaseService
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.regex.Pattern

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
                            // Update local vendor state
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
            onLogout = {
                showLogoutDialog = true
            },
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
            onEditProfilePicture = {
                showImageSourceDialog = true
            },
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
                ) {
                    Text("Logout")
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
                ) {
                    Text("Gallery")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showImageSourceDialog = false }
                ) {
                    Text("Cancel")
                }
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

        // Vendor name validation
        if (editableVendorName.isEmpty()) {
            vendorNameError = "Vendor name is required"
            isValid = false
        } else if (editableVendorName.length < 2) {
            vendorNameError = "Vendor name must be at least 2 characters"
            isValid = false
        } else {
            vendorNameError = ""
        }

        // Email validation
        if (editableEmail.isEmpty()) {
            emailError = "Email is required"
            isValid = false
        } else if (!isValidEmail(editableEmail)) {
            emailError = "Please enter a valid email address"
            isValid = false
        } else {
            emailError = ""
        }

        // Contact validation
        if (editableContact.isEmpty()) {
            contactError = "Contact number is required"
            isValid = false
        } else if (!isValidPhoneNumber(editableContact)) {
            contactError = "Please enter a valid phone number"
            isValid = false
        } else {
            contactError = ""
        }

        // Address validation
        if (editableAddress.isEmpty()) {
            addressError = "Address is required"
            isValid = false
        } else if (editableAddress.length < 10) {
            addressError = "Please enter a complete address"
            isValid = false
        } else {
            addressError = ""
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
        // Header Section with Edit Button
        HeaderSection(
            vendor = vendor,
            isEditing = isEditing,
            onEditToggle = onEditToggle,
            onSave = { saveChanges() },
            onCancel = { cancelEdit() },
            onEditProfilePicture = onEditProfilePicture
        )

        // Vendor Information Card
        VendorInfoCard(
            vendor = vendor,
            isEditing = isEditing,
            editableVendorName = editableVendorName,
            editableEmail = editableEmail,
            editableContact = editableContact,
            editableAddress = editableAddress,
            onVendorNameChange = { editableVendorName = it },
            onEmailChange = { editableEmail = it },
            onContactChange = { editableContact = it },
            onAddressChange = { editableAddress = it },
            vendorNameError = vendorNameError,
            emailError = emailError,
            contactError = contactError,
            addressError = addressError
        )

        if (!isEditing) {
            // Navigation Cards (only show when not editing)
            NavigationCards(navController = navController)

            // Logout Button
            LogoutButton(onLogout = onLogout)
        }
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
            // Vendor Logo/Image with edit overlay
            Box(
                contentAlignment = Alignment.BottomEnd
            ) {
                // Profile Image
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

                // Edit icon overlay (only show when not in editing mode)
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

            // Vendor Name
            Text(
                text = vendor?.vendorName ?: "Vendor Name",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline or Status
            Text(
                text = "Verified Vendor",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Edit/Save/Cancel Buttons
            if (isEditing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = onSave,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save")
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = "Cancel")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel")
                    }
                }
            } else {
                Button(
                    onClick = onEditToggle,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Profile")
                }
            }
        }
    }
}

// Add these utility functions for image handling
private fun uriToBase64(context: Context, uri: Uri): String? {
    return try {
        val bitmap = if (Build.VERSION.SDK_INT < 28) {
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        } else {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        }

        // Resize bitmap to reduce size (optional)
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
    addressError: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Vendor Information",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Vendor ID (Non-editable)
            InfoRow(
                icon = Icons.Default.Business,
                label = "Vendor ID:",
                value = vendor?.vendorId ?: "N/A",
                isEditable = false
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Vendor Name (Editable)
            EditableInfoRow(
                icon = Icons.Default.Business,
                label = "Vendor Name:",
                value = if (isEditing) editableVendorName else vendor?.vendorName ?: "N/A",
                isEditable = isEditing,
                onValueChange = onVendorNameChange,
                errorMessage = vendorNameError
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Email (Editable)
            EditableInfoRow(
                icon = Icons.Default.Email,
                label = "Email:",
                value = if (isEditing) editableEmail else vendor?.email ?: "N/A",
                isEditable = isEditing,
                onValueChange = onEmailChange,
                errorMessage = emailError
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Contact (Editable)
            EditableInfoRow(
                icon = Icons.Default.Phone,
                label = "Contact:",
                value = if (isEditing) editableContact else vendor?.vendorContact ?: "N/A",
                isEditable = isEditing,
                onValueChange = onContactChange,
                errorMessage = contactError
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Address (Editable)
            EditableAddressRow(
                icon = Icons.Default.LocationOn,
                label = "Address:",
                value = if (isEditing) editableAddress else vendor?.address ?: "N/A",
                isEditable = isEditing,
                onValueChange = onAddressChange,
                errorMessage = addressError
            )
        }
    }
}

@Composable
fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    isEditable: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun EditableInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    isEditable: Boolean,
    onValueChange: (String) -> Unit,
    errorMessage: String = ""
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (isEditable) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errorMessage.isNotEmpty(),
                    supportingText = {
                        if (errorMessage.isNotEmpty()) {
                            Text(text = errorMessage)
                        }
                    }
                )
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun EditableAddressRow(
    icon: ImageVector,
    label: String,
    value: String,
    isEditable: Boolean,
    onValueChange: (String) -> Unit,
    errorMessage: String = ""
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (isEditable) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3,
                    isError = errorMessage.isNotEmpty(),
                    supportingText = {
                        if (errorMessage.isNotEmpty()) {
                            Text(text = errorMessage)
                        }
                    }
                )
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun NavigationCards(navController: NavController) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // My Retail Card
        NavigationCard(
            title = "My Retail",
            description = "Manage your products and inventory",
            onClick = {
                // Navigate to retail management screen
                // navController.navigate("vendorRetail")
            }
        )

        // Report Card
        NavigationCard(
            title = "Report",
            description = "View sales reports and analytics",
            onClick = {
                // Navigate to reports screen
                // navController.navigate("vendorReports")
            }
        )

        // Account Card
        NavigationCard(
            title = "Account",
            description = "Manage account settings and profile",
            onClick = {
                // Navigate to account settings
                // navController.navigate("vendorAccountSettings")
            }
        )
    }
}

@Composable
fun NavigationCard(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Chevron icon - using text arrow as fallback
            Text(
                text = "→",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LogoutButton(onLogout: () -> Unit) {
    Button(
        onClick = onLogout,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    ) {
        Icon(
            imageVector = Icons.Default.ExitToApp,
            contentDescription = "Logout",
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "LOG OUT",
            fontWeight = FontWeight.Bold
        )
    }
}

// Validation functions
private fun isValidEmail(email: String): Boolean {
    val emailRegex = "^[A-Za-z](.*)([@]{1})(.{1,})(\\.)(.{1,})".toRegex()
    return emailRegex.matches(email)
}

private fun isValidPhoneNumber(phone: String): Boolean {
    val phoneRegex = "^[+]?[0-9]{10,15}\$".toRegex()
    return phoneRegex.matches(phone.replace("\\s".toRegex(), ""))
}