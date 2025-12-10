package com.example.miniproject.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
    var logoClickCount by remember { mutableStateOf(0) }
    var showAdminHint by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Reset click count after 3 seconds of inactivity
    LaunchedEffect(logoClickCount) {
        if (logoClickCount > 0) {
            kotlinx.coroutines.delay(3000)
            if (logoClickCount < 5) {
                logoClickCount = 0
                showAdminHint = false
            }
        }
    }

    // Navigate to admin login when 5 clicks are reached
    LaunchedEffect(logoClickCount) {
        if (logoClickCount >= 5) {
            logoClickCount = 0
            showAdminHint = false
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
                        .padding(32.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Logo with click listener for hidden admin access
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clickable {
                                logoClickCount++
                                if (logoClickCount in 1..4) {
                                    showAdminHint = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "Tap N Chow Logo",
                            modifier = Modifier.size(150.dp)
                        )

                        // Show click count hint (subtle visual feedback)
                        if (logoClickCount in 1..4) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(Color.Black.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Text(
                                    text = "Admin: ${logoClickCount}/5",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }
                    }

                    // Hidden admin hint (appears after first click)
                    AnimatedVisibility(
                        visible = showAdminHint,
                        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                            animationSpec = tween(300),
                            initialOffsetY = { -10 }
                        ),
                        exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(
                            animationSpec = tween(300),
                            targetOffsetY = { -10 }
                        )
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Admin Access",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when (logoClickCount) {
                                        1 -> "Tap 4 more times for admin access"
                                        2 -> "Tap 3 more times"
                                        3 -> "Tap 2 more times"
                                        4 -> "One more tap!"
                                        else -> ""
                                    },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Text(
                        text = "Login",
                        style = MaterialTheme.typography.headlineMedium,
                        fontSize = 32.sp
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    CustomTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "User ID/Email",
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
                        onClick = { navController.navigate("forgotPassword") } // Updated
                    ) {
                        Text("Forgot password?")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (email.isNotEmpty() && password.isNotEmpty()) {
                                isLoading = true
                                CoroutineScope(Dispatchers.Main).launch {
                                    val result = AuthService().login(email, password)
                                    isLoading = false
                                    if (result.isSuccess) {
                                        // Navigate to home screen
                                        navController.navigate("home") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    } else {
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