package com.example.miniproject.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable

// Customer Model (previously User)
data class Customer(
    val customerId: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val password: String = "",
    val profileImageBase64: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    fun isValid(): Boolean {
        return customerId.isNotBlank() &&
                name.isNotBlank() &&
                email.isNotBlank() &&
                phoneNumber.isNotBlank()
    }

    companion object {
        suspend fun generateCustomerId(db: FirebaseFirestore): String {
            return try {
                // Get the count of existing customers
                val count = db.collection("customers")
                    .get()
                    .await()
                    .size()

                // Format as C0001, C0002, etc.
                "C${(count + 1).toString().padStart(4, '0')}"
            } catch (e: Exception) {
                // Fallback if counting fails
                "C${System.currentTimeMillis().toString().takeLast(4).padStart(4, '0')}"
            }
        }
    }
}

// Vendor Model with category
data class Vendor(
    val vendorId: String = "",
    val vendorName: String = "",
    val vendorContact: String = "",
    val email: String = "",
    val address: String = "",
    val category: String = "", // New category field: "restaurant", "grocery", etc.
    val profileImageBase64: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    companion object {
        suspend fun generateVendorId(db: FirebaseFirestore): String {
            return try {
                // Get the count of existing vendors
                val count = db.collection("vendors")
                    .get()
                    .await()
                    .size()

                println("DEBUG: Existing vendor count: $count") // Add logging

                // Format as V0001, V0002, etc.
                val newId = "V${(count + 1).toString().padStart(4, '0')}"
                println("DEBUG: Generated new vendor ID: $newId") // Add logging
                newId
            } catch (e: Exception) {
                // Fallback if counting fails
                val fallbackId =
                    "V${System.currentTimeMillis().toString().takeLast(4).padStart(4, '0')}"
                println("DEBUG: Using fallback vendor ID: $fallbackId") // Add logging
                fallbackId
            }
        }
    }
}
// Product Model
data class Product(
    @DocumentId val productId: String = "", // Keep for products as they need auto-generated IDs
    val vendorId: String = "",
    val productName: String = "",
    val productPrice: Double = 0.0,
    val description: String = "",
    val stock: Int = 0,
    val imageUrl: String = "",
    val category: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

// Order Model
data class Order(
    @DocumentId val orderId: String = "", // Keep for orders as they need auto-generated IDs
    val customerId: String = "",
    val orderDate: Timestamp = Timestamp.now(),
    val status: String = "pending",
    val totalPrice: Double = 0.0,
    val shippingAddress: String = "",
    val paymentMethod: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    companion object {
        suspend fun generateOrderId(db: FirebaseFirestore): String {
            return try {
                // Get the count of existing orders
                val count = db.collection("orders")
                    .get()
                    .await()
                    .size()

                // Format as O001, O002, etc.
                "O${(count + 1).toString().padStart(3, '0')}"
            } catch (e: Exception) {
                // Fallback if counting fails
                "O${System.currentTimeMillis().toString().takeLast(3).padStart(3, '0')}"
            }
        }
    }
}
// Order Detail Model
data class OrderDetail(
    @DocumentId val orderDetailsId: String = "", // Keep for order details
    val orderId: String = "",
    val productId: String = "",
    val productName: String = "",
    val productPrice: Double = 0.0,
    val quantity: Int = 0,
    val subtotal: Double = 0.0,
    val createdAt: Timestamp = Timestamp.now()
)

// Payment Model
data class Payment(
    @DocumentId val paymentId: String = "", // Keep for payments
    val orderId: String = "",
    val amount: Double = 0.0,
    val paymentMethod: String = "",
    val paymentStatus: String = "",
    val transactionDate: Timestamp = Timestamp.now(),
    val createdAt: Timestamp = Timestamp.now()
)

// Feedback Model
data class Feedback(
    @DocumentId val feedbackId: String = "", // Keep for feedback
    val customerId: String = "",
    val orderId: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val feedbackDate: Timestamp = Timestamp.now(),
    val createdAt: Timestamp = Timestamp.now()
)

@Serializable
data class CartItem(
    val productId: String = "",
    val productName: String = "",
    val productPrice: Double = 0.0,
    val quantity: Int = 0,
    val vendorId: String = "",
    val imageUrl: String = ""
)

@Serializable
data class Cart(
    val vendorId: String = "",
    val vendorName: String = "",
    val vendorAddress: String = "",
    val vendorContact: String = "",
    val vendorProfileImage: String = "",
    val items: List<CartItem> = emptyList(),
    val subtotal: Double = 0.0,
    val serviceFee: Double = 0.0,
    val tax: Double = 0.0,
    val total: Double = 0.0
)

data class OrderRequest(
    val customerId: String = "",
    val vendorId: String = "",
    val items: List<CartItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val deliveryAddress: String = "",
    val paymentMethod: String = ""
)

data class CustomerAccount(
    val customerId: String = "",
    val tapNChowCredit: Double = 0.0,
    val lastUpdated: Timestamp = Timestamp.now()
)

// Vendor Category Constants for easy reference
object VendorCategory {
    const val RESTAURANT = "restaurant"
    const val GROCERY = "grocery"
    const val CAFE = "cafe"
    const val BAKERY = "bakery"
    const val OTHER = "other"

    fun getAllCategories(): List<String> {
        return listOf(RESTAURANT, GROCERY, CAFE, BAKERY, OTHER)
    }

    fun getDisplayName(category: String): String {
        return when (category) {
            RESTAURANT -> "Restaurant"
            GROCERY -> "Grocery Store"
            CAFE -> "Cafe"
            BAKERY -> "Bakery"
            OTHER -> "Other"
            else -> category
        }
    }
}