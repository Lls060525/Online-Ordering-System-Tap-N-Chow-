package com.example.miniproject.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.miniproject.components.CustomTextField
import com.example.miniproject.service.AuthService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorRegisterScreen(navController: NavController) {
    var vendorName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var vendorContact by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Individual field errors
    var vendorNameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var contactError by remember { mutableStateOf<String?>(null) }
    var addressError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    // Scroll state
    val scrollState = rememberScrollState()

    // Clear errors when user starts typing
    LaunchedEffect(vendorName) { if (vendorName.isNotEmpty()) vendorNameError = null }
    LaunchedEffect(email) { if (email.isNotEmpty()) emailError = null }
    LaunchedEffect(vendorContact) { if (vendorContact.isNotEmpty()) contactError = null }
    LaunchedEffect(address) { if (address.isNotEmpty()) addressError = null }
    LaunchedEffect(password) { if (password.isNotEmpty()) passwordError = null }
    LaunchedEffect(confirmPassword) { if (confirmPassword.isNotEmpty()) confirmPasswordError = null }

    fun validateForm(): Boolean {
        var isValid = true

        // Vendor name validation
        if (vendorName.isEmpty()) {
            vendorNameError = "Vendor name is required"
            isValid = false
        } else if (vendorName.length < 2) {
            vendorNameError = "Vendor name must be at least 2 characters"
            isValid = false
        } else {
            vendorNameError = null
        }

        // Email validation
        if (email.isEmpty()) {
            emailError = "Email is required"
            isValid = false
        } else if (!isValidEmail(email)) {
            emailError = "Please enter a valid email address"
            isValid = false
        } else {
            emailError = null
        }

        // Contact validation
        if (vendorContact.isEmpty()) {
            contactError = "Contact number is required"
            isValid = false
        } else if (!isValidPhoneNumber(vendorContact)) {
            contactError = "Please enter a valid phone number"
            isValid = false
        } else {
            contactError = null
        }

        // Address validation
        if (address.isEmpty()) {
            addressError = "Address is required"
            isValid = false
        } else if (address.length < 10) {
            addressError = "Please enter a complete address"
            isValid = false
        } else {
            addressError = null
        }

        // Password validation
        if (password.isEmpty()) {
            passwordError = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            passwordError = "Password must be at least 6 characters"
            isValid = false
        } else if (!containsLetterAndDigit(password)) {
            passwordError = "Password must contain both letters and numbers"
            isValid = false
        } else {
            passwordError = null
        }

        // Confirm password validation
        if (confirmPassword.isEmpty()) {
            confirmPasswordError = "Please confirm your password"
            isValid = false
        } else if (password != confirmPassword) {
            confirmPasswordError = "Passwords do not match"
            isValid = false
        } else {
            confirmPasswordError = null
        }

        return isValid
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Vendor Registration",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // Logo Section
                Image(
                    painter = painterResource(id = R.drawable.logo2),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(100.dp)
                        .padding(bottom = 8.dp)
                )

                Text(
                    text = "TAP N CHOW",
                    style = MaterialTheme.typography.headlineMedium,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Vendor Registration",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Form Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CustomTextField(
                        value = vendorName,
                        onValueChange = { vendorName = it },
                        label = "Vendor Name",
                        placeholder = "Enter your business name",
                        isError = vendorNameError != null,
                        supportingText = vendorNameError
                    )

                    CustomTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email",
                        placeholder = "Enter your business email",
                        isError = emailError != null,
                        supportingText = emailError
                    )

                    CustomTextField(
                        value = vendorContact,
                        onValueChange = { vendorContact = it },
                        label = "Contact Number",
                        placeholder = "Enter your contact number",
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone,
                        isError = contactError != null,
                        supportingText = contactError
                    )

                    CustomTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = "Business Address",
                        placeholder = "Enter your complete business address",
                        isError = addressError != null,
                        supportingText = addressError
                    )

                    CustomTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        placeholder = "Enter password (min. 6 characters)",
                        visualTransformation = PasswordVisualTransformation(),
                        isError = passwordError != null,
                        supportingText = passwordError
                    )

                    CustomTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = "Confirm Password",
                        placeholder = "Confirm your password",
                        visualTransformation = PasswordVisualTransformation(),
                        isError = confirmPasswordError != null,
                        supportingText = confirmPasswordError
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

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
                                    password = password
                                )
                                isLoading = false
                                if (result.isSuccess) {
                                    navController.navigate("vendorHome") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                } else {
                                    errorMessage = when {
                                        result.exceptionOrNull()?.message?.contains("email-already-in-use") == true ->
                                            "An account with this email already exists"
                                        result.exceptionOrNull()?.message?.contains("invalid-email") == true ->
                                            "Invalid email format"
                                        result.exceptionOrNull()?.message?.contains("weak-password") == true ->
                                            "Password is too weak. Please use a stronger password"
                                        else -> "Registration failed. Please try again"
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Register as Vendor")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Already have a vendor account?",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(
                        onClick = { navController.navigate("vendorLogin") }
                    ) {
                        Text("Vendor Login")
                    }
                }

                errorMessage?.let { message ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Add extra space at the bottom for better scrolling
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// Reuse validation functions from RegisterScreen
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