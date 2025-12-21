
package com.example.miniproject.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable

data class Admin(
    val adminId: String = "ADMIN001", // Fixed ID for single admin
    val name: String = "Platform Administrator",
    val email: String = "admin@admin.com.my", // Fixed email
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val permissions: List<String> = listOf(
        "manage_users",
        "manage_vendors",
        "view_reports",
        "freeze_accounts",
        "view_analytics"
    )
) {
    // No freeze property needed for admin
    companion object {
        const val ADMIN_ID = "ADMIN001"
        const val ADMIN_EMAIL = "admin@admin.com.my"
    }
}

data class Customer(
    val customerId: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val profileImageBase64: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val isFrozen: Boolean = false, // ONLY CUSTOMERS CAN BE FROZEN
    val lastLogin: Timestamp? = null,
    val loginCount: Int = 0,
    val orderCount: Int = 0,
    val totalSpent: Double = 0.0,
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
                val count = db.collection("customers")
                    .get()
                    .await()
                    .size()
                "C${(count + 1).toString().padStart(4, '0')}"
            } catch (e: Exception) {
                "C${System.currentTimeMillis().toString().takeLast(4).padStart(4, '0')}"
            }
        }
    }
}


data class Vendor(
    val vendorId: String = "",
    val vendorName: String = "",
    val email: String = "",
    val vendorContact: String = "",
    val address: String = "",
    val category: String = "",
    val profileImageBase64: String = "",
    val isFrozen: Boolean = false, // ONLY VENDORS CAN BE FROZEN
    val lastLogin: Timestamp? = null,
    val loginCount: Int = 0,
    val orderCount: Int = 0,
    val totalRevenue: Double = 0.0,
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,

    val acceptedPaymentMethods: List<String> = listOf("cash"),
    val paypalLink: String = ""
) {
    companion object {
        suspend fun generateVendorId(db: FirebaseFirestore): String {
            return try {
                val count = db.collection("vendors")
                    .get()
                    .await()
                    .size()

                println("DEBUG: Existing vendor count: $count")

                val newId = "V${(count + 1).toString().padStart(4, '0')}"
                println("DEBUG: Generated new vendor ID: $newId")
                newId
            } catch (e: Exception) {
                val fallbackId =
                    "V${System.currentTimeMillis().toString().takeLast(4).padStart(4, '0')}"
                println("DEBUG: Using fallback vendor ID: $fallbackId")
                fallbackId
            }
        }
    }
}

data class Voucher(
    @DocumentId val voucherId: String = "",
    val vendorId: String = "",
    val vendorName: String = "",
    val vendorProfileImage: String = "", // Stores Base64 string
    val code: String = "",
    val description: String = "",
    val discountType: String = "percentage",
    val discountValue: Double = 0.0,
    val minSpend: Double = 0.0,
    val coinCost: Int = 0,
    val expiryDate: Timestamp = Timestamp.now(),
    val isActive: Boolean = true,
    val usageLimit: Int = 100,
    val usedCount: Int = 0,
    val createdAt: Timestamp = Timestamp.now()
)

// Product Model
data class Product(
    @DocumentId val productId: String = "",
    val vendorId: String = "",
    val productName: String = "",
    val productPrice: Double = 0.0,
    val description: String = "",
    val stock: Int = 0,
    val imageUrl: String = "",
    val category: String = "",
    val customizations: List<ProductCustomization> = emptyList(), // <--- ADD THIS
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

data class CustomizationOption(
    val name: String = "",      // e.g., "Large", "Extra Spicy"
    val price: Double = 0.0     // e.g., 2.0 (adds RM2.00)
)

data class ProductCustomization(
    val title: String = "",       // e.g., "Size", "Spiciness Level"
    val required: Boolean = false, // If true, customer MUST select one
    val singleSelection: Boolean = true, // true = Radio Button, false = Checkbox
    val options: List<CustomizationOption> = emptyList()
)

// Order Model
data class Order(
    @DocumentId val documentId: String = "", // Firestore document ID (0001, 0002, etc.)
    val orderId: String = "", // Custom order ID (O001, O002, etc.) - can be same as documentId or different
    val customerId: String = "",
    val orderDate: Timestamp = Timestamp.now(),
    val status: String = "pending",
    val vendorIds: List<String> = emptyList(),
    val totalPrice: Double = 0.0,
    val shippingAddress: String = "",
    val paymentMethod: String = "",
    val paypalOrderId: String = "",
    val paypalPayerId: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    // Helper function to get display order ID
    fun getDisplayOrderId(): String {
        return if (orderId.isNotEmpty()) orderId else "O${documentId.padStart(3, '0')}"
    }

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
// Feedback Model with enhanced fields
data class Feedback(
    @DocumentId val feedbackId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val vendorId: String = "",
    val vendorName: String = "",
    val orderId: String = "",
    val productId: String = "",
    val productName: String = "",
    val rating: Double = 0.0,
    val comment: String = "",
    val feedbackDate: Timestamp = Timestamp.now(),
    val createdAt: Timestamp = Timestamp.now(),
    val isVisible: Boolean = true,
    // New fields for vendor replies
    val vendorReply: String = "",
    val vendorReplyDate: Timestamp? = null,
    val isReplied: Boolean = false
)

// Add Vendor Analytics Model
data class VendorAnalytics(
    val vendorId: String = "",
    val totalReviews: Int = 0,
    val averageRating: Double = 0.0,
    val ratingDistribution: Map<Int, Int> = mapOf( // 1 to 5 stars count
        1 to 0,
        2 to 0,
        3 to 0,
        4 to 0,
        5 to 0
    ),
    val totalReplies: Int = 0,
    val replyRate: Double = 0.0, // Percentage of reviews with replies
    val lastUpdated: Timestamp = Timestamp.now()
)

// Product Rating Summary Model
data class ProductRating(
    val productId: String = "",
    val averageRating: Double = 0.0,
    val totalRatings: Int = 0,
    val ratingDistribution: Map<Int, Int> = mapOf( // 1 to 5 stars count
        1 to 0,
        2 to 0,
        3 to 0,
        4 to 0,
        5 to 0
    ),
    val lastUpdated: Timestamp = Timestamp.now()
)

// Vendor Rating Summary Model
data class VendorRating(
    val vendorId: String = "",
    val averageRating: Double = 0.0,
    val totalRatings: Int = 0,
    val lastUpdated: Timestamp = Timestamp.now()
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
    val paymentMethod: String = "",
    // ADD THESE TWO FIELDS:
    val discountAmount: Double = 0.0,
    val voucherCode: String? = null
)

data class CustomerAccount(
    val customerId: String = "",
    val tapNChowCoins: Int = 0,
    val lastSpinDate: Timestamp? = null,
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