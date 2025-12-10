package com.example.miniproject.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.model.Voucher
import com.example.miniproject.service.DatabaseService
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerVoucherScreen(navController: NavController) {
    val databaseService = DatabaseService()
    var vouchers by remember { mutableStateOf<List<Voucher>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Load Vouchers
    LaunchedEffect(Unit) {
        vouchers = databaseService.getAllActiveVouchers()
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Vouchers", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                // Green color to match your Customer App theme
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF4CAF50))
            )
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
                    Text("No vouchers available at the moment.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(vouchers) { voucher ->
                        CustomerVoucherCard(voucher)
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerVoucherCard(voucher: Voucher) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    // Decode vendor image
    val vendorImageBitmap = remember(voucher.vendorProfileImage) {
        getBitmapFromBase64(voucher.vendorProfileImage)
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Vendor Info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    modifier = Modifier.size(32.dp),
                    color = Color.LightGray
                ) {
                    if (vendorImageBitmap != null) {
                        Image(
                            bitmap = vendorImageBitmap,
                            contentDescription = "Vendor",
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Store, contentDescription = null, tint = Color.White, modifier = Modifier.padding(6.dp))
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = voucher.vendorName.ifBlank { "Vendor Voucher" },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))

            // Content: Discount & Code
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (voucher.discountType == "percentage") "${voucher.discountValue.toInt()}% OFF"
                        else "RM${"%.2f".format(voucher.discountValue)} OFF",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50) // Green text
                    )
                    Text(
                        text = "Min Spend: RM${"%.2f".format(voucher.minSpend)}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "Valid until: ${dateFormat.format(voucher.expiryDate.toDate())}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                // Copy Code Button
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(voucher.code))
                        Toast.makeText(context, "Code copied!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F5E9)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = voucher.code,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}