package com.example.miniproject.screens.vendor

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.R
import com.example.miniproject.utils.CustomTextField
import com.example.miniproject.model.VendorCategory
import com.example.miniproject.service.AuthService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.miniproject.util.GeocodingHelper
import com.example.miniproject.util.LocationHelper


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorRegisterScreen(navController: NavController) {
    // --- Form State ---
    var vendorName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var vendorContact by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(VendorCategory.RESTAURANT) }
    var isCategoryExpanded by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // --- Location State ---
    var latitude by remember { mutableStateOf(0.0) }
    var longitude by remember { mutableStateOf(0.0) }
    var isLocationLoading by remember { mutableStateOf(false) }

    // --- Error States ---
    var vendorNameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var contactError by remember { mutableStateOf<String?>(null) }
    var addressError by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val locationHelper = remember { LocationHelper(context) }
    val scrollState = rememberScrollState()

    // --- Permission Launcher ---
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions.values.all { it }
        if (isGranted) {
            isLocationLoading = true
            locationHelper.getCurrentLocation { loc ->
                latitude = loc.latitude
                longitude = loc.longitude

                // Convert GPS to Address Address
                CoroutineScope(Dispatchers.Main).launch {
                    val addressString = GeocodingHelper.getAddressFromCoordinates(
                        context,
                        loc.latitude,
                        loc.longitude
                    )
                    address = addressString
                    isLocationLoading = false
                }
            }
        } else {
            errorMessage = "Location permission is needed to detect your shop"
        }
    }

    // --- Validation Logic ---
    fun validateForm(): Boolean {
        var isValid = true

        if (vendorName.isEmpty()) { vendorNameError = "Required"; isValid = false }
        else if (vendorName.length < 2) { vendorNameError = "Min 2 chars"; isValid = false }
        else vendorNameError = null

        if (email.isEmpty()) { emailError = "Required"; isValid = false }
        else if (!isValidEmail(email)) { emailError = "Invalid email"; isValid = false }
        else emailError = null

        if (vendorContact.isEmpty()) { contactError = "Required"; isValid = false }
        else contactError = null

        if (address.isEmpty()) { addressError = "Required"; isValid = false }
        else addressError = null

        if (password.isEmpty()) { passwordError = "Required"; isValid = false }
        else if (password.length < 6) { passwordError = "Min 6 chars"; isValid = false }
        else if (!containsLetterAndDigit(password)) { passwordError = "Must contain letters & numbers"; isValid = false }
        else passwordError = null

        if (confirmPassword.isEmpty()) { confirmPasswordError = "Required"; isValid = false }
        else if (password != confirmPassword) { confirmPasswordError = "Passwords do not match"; isValid = false }
        else confirmPasswordError = null

        return isValid
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vendor Registration") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- Logo ---
                Image(
                    painter = painterResource(id = R.drawable.logo2),
                    contentDescription = "Logo",
                    modifier = Modifier.size(80.dp)
                )

                Text(
                    "Create Vendor Account",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                // --- Form Fields ---
                CustomTextField(
                    value = vendorName,
                    onValueChange = { vendorName = it; vendorNameError = null },
                    label = "Vendor Name",
                    placeholder = "Business name",
                    isError = vendorNameError != null,
                    supportingText = vendorNameError
                )

                CustomTextField(
                    value = email,
                    onValueChange = { email = it; emailError = null },
                    label = "Email",
                    placeholder = "Business email",
                    isError = emailError != null,
                    supportingText = emailError
                )

                CustomTextField(
                    value = vendorContact,
                    onValueChange = { vendorContact = it; contactError = null },
                    label = "Contact Number",
                    placeholder = "Phone number",
                    isError = contactError != null,
                    supportingText = contactError
                )

                // --- Address & Location Section ---
                Text(
                    "Shop Location",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Start)
                )

                // Address Field with Detect Button (NO MAP VISUAL)
                CustomTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = "Address",
                    placeholder = "Enter address or click detect",
                    isError = addressError != null,
                    supportingText = addressError,
                    trailingIcon = {
                        IconButton(onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                isLocationLoading = true
                                locationHelper.getCurrentLocation { loc ->
                                    latitude = loc.latitude
                                    longitude = loc.longitude

                                    CoroutineScope(Dispatchers.Main).launch {
                                        val addressString = GeocodingHelper.getAddressFromCoordinates(
                                            context,
                                            loc.latitude,
                                            loc.longitude
                                        )
                                        address = addressString
                                        isLocationLoading = false
                                    }
                                }
                            } else {
                                locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                            }
                        }) {
                            if (isLocationLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            } else {
                                Icon(Icons.Default.MyLocation, "Detect Location", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                )

                if (latitude != 0.0) {
                    Text(
                        text = "Location Captured: $latitude, $longitude",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }

                // --- Category ---
                ExposedDropdownMenuBox(
                    expanded = isCategoryExpanded,
                    onExpandedChange = { isCategoryExpanded = !isCategoryExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = VendorCategory.getDisplayName(category),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        isError = categoryError != null,
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = isCategoryExpanded,
                        onDismissRequest = { isCategoryExpanded = false }
                    ) {
                        VendorCategory.getAllCategories().forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(VendorCategory.getDisplayName(cat)) },
                                onClick = { category = cat; isCategoryExpanded = false }
                            )
                        }
                    }
                }

                // --- Passwords ---
                CustomTextField(
                    value = password,
                    onValueChange = { password = it; passwordError = null },
                    label = "Password",
                    placeholder = "Min 6 characters",
                    visualTransformation = PasswordVisualTransformation(),
                    isError = passwordError != null,
                    supportingText = passwordError
                )

                CustomTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; confirmPasswordError = null },
                    label = "Confirm Password",
                    placeholder = "Re-enter password",
                    visualTransformation = PasswordVisualTransformation(),
                    isError = confirmPasswordError != null,
                    supportingText = confirmPasswordError
                )

                Spacer(modifier = Modifier.height(16.dp))

                // --- Register Button ---
                Button(
                    onClick = {
                        if (validateForm()) {
                            isLoading = true
                            errorMessage = null
                            CoroutineScope(Dispatchers.Main).launch {
                                val result = AuthService().registerVendor(
                                    vendorName = vendorName.trim(),
                                    email = email.trim(),
                                    vendorContact = vendorContact.trim(),
                                    address = address.trim(),
                                    category = category,
                                    password = password,
                                    latitude = latitude,
                                    longitude = longitude
                                )
                                isLoading = false
                                if (result.isSuccess) {
                                    navController.navigate("vendorHome") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                } else {
                                    errorMessage = result.exceptionOrNull()?.message ?: "Registration failed"
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Register Shop")
                }

                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

private fun isValidEmail(email: String): Boolean {
    val emailRegex = "^[A-Za-z](.*)([@]{1})(.{1,})(\\.)(.{1,})".toRegex()
    return emailRegex.matches(email)
}

private fun isValidPhoneNumber(phone: String): Boolean {
    val phoneRegex = "^[+]?[0-9]{10,15}\$".toRegex()
    return phoneRegex.matches(phone.replace("\\s".toRegex(), ""))
}

private fun containsLetterAndDigit(password: String): Boolean {
    val hasLetter = password.any { it.isLetter() }
    val hasDigit = password.any { it.isDigit() }
    return hasLetter && hasDigit
}