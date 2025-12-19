package com.example.miniproject.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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
fun LoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // --- Hidden Admin Logic ---
    var logoClickCount by remember { mutableIntStateOf(0) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Create AuthService instance once
    val authService = remember { AuthService() }

    // Logic: Reset click count if user stops tapping for 1 second (Requires rapid tapping)
    LaunchedEffect(logoClickCount) {
        if (logoClickCount > 0) {
            kotlinx.coroutines.delay(1000) // 1 second reset timer
            if (logoClickCount < 10) {
                logoClickCount = 0
            }
        }
    }

    // Navigate to admin login when 10 clicks are reached
    LaunchedEffect(logoClickCount) {
        if (logoClickCount >= 10) {
            logoClickCount = 0
            navController.navigate("adminLogin")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF4CAF50))
    ) {
        Scaffold(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 30.dp),
            topBar = {
                TopAppBar(
                    title = { Text("Tap N Chow") }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
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
                        .padding(32.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // --- Logo Section (Hidden Trigger) ---
                    // No visual feedback (ripple removed) to make it look like a static image
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null // Removes the ripple effect to make it "Hidden"
                            ) {
                                logoClickCount++
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "Tap N Chow Logo",
                            modifier = Modifier.size(150.dp)
                        )
                        // Removed the "Admin: X/5" overlay text here
                    }

                    // Removed the AnimatedVisibility Hint Card here

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Login",
                        style = MaterialTheme.typography.headlineMedium,
                        fontSize = 32.sp
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    CustomTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "User Email",
                        placeholder = "Type here"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    CustomTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        placeholder = "Type here",
                        visualTransformation = PasswordVisualTransformation()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { navController.navigate("forgotPassword") }
                    ) {
                        Text("Forgot password?")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (email.isNotEmpty() && password.isNotEmpty()) {
                                isLoading = true
                                CoroutineScope(Dispatchers.Main).launch {
                                    val result = authService.login(email, password)

                                    if (result.isSuccess) {
                                        // LOGIN SUCCESSFUL - Now check Role
                                        val role = authService.getCurrentUserRole()

                                        if (role == "vendor") {
                                            // BLOCKED: Vendor trying to log in as Customer
                                            authService.logout()
                                            isLoading = false
                                            errorMessage = "Invalid account type. Please use Vendor Login."
                                        } else if (role == "admin") {
                                            // BLOCKED: Admin trying to log in as Customer
                                            authService.logout()
                                            isLoading = false
                                            errorMessage = "Invalid account type. Please use Admin Login."
                                        } else {
                                            // SUCCESS: Customer allowed
                                            isLoading = false
                                            navController.navigate("home") {
                                                popUpTo("login") { inclusive = true }
                                            }
                                        }
                                    } else {
                                        isLoading = false
                                        val errorMsg = result.exceptionOrNull()?.message ?: "Login failed"
                                        errorMessage = errorMsg

                                        // Show snackbar if account is frozen
                                        if (errorMsg.contains("frozen", ignoreCase = true)) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message = errorMsg,
                                                    duration = SnackbarDuration.Long
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                errorMessage = "Please fill all fields"
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Enter")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TextButton(
                            onClick = { navController.navigate("register") }
                        ) {
                            Text("Don't have an account? Register")
                        }

                        TextButton(
                            onClick = { navController.navigate("vendorLogin") }
                        ) {
                            Text("Become a Vendor?")
                        }
                    }

                    errorMessage?.let { message ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}