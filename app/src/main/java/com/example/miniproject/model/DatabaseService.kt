package com.example.miniproject.service

import android.net.Uri
import com.example.miniproject.model.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

class DatabaseService {
    private val db = FirebaseFirestore.getInstance()


    private suspend fun createFreezeLog(userId: String, userType: String, reason: String) {
        try {
            val log = mapOf(
                "userId" to userId,
                "userType" to userType,
                "reason" to reason,
                "actionDate" to Timestamp.now(),
                "adminAction" to true,
                "createdAt" to Timestamp.now()
            )
            db.collection("freeze_logs").add(log).await()
        } catch (e: Exception) {
            // Silently fail - logging is not critical
        }
    }

    suspend fun getAdmin(): Admin? {
        return try {
            db.collection("admins").document("ADMIN001")
                .get(Source.SERVER)
                .await()
                .toObject(Admin::class.java)
        } catch (e: Exception) {
            null
        }
    }

// Add to DatabaseService class

    suspend fun adminExists(): Boolean {
        return try {
            val adminDoc = db.collection("admins").document("ADMIN001").get().await()
            adminDoc.exists()
        } catch (e: Exception) {
            false
        }
    }

    // Create admin (one-time setup)
    suspend fun createAdmin(): Result<Boolean> {
        return try {
            val adminExists = adminExists()
            if (!adminExists) {
                val admin = Admin(
                    adminId = "ADMIN001",
                    name = "Platform Administrator",
                    email = "admin@admin.com.my",
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                )
                db.collection("admins").document("ADMIN001").set(admin).await()
                Result.success(true)
            } else {
                Result.success(false) // Already exists
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Calculate actual vendor statistics
    suspend fun calculateVendorActualStats(vendorId: String): Triple<Int, Double, Double> {
        var orderCount = 0
        var totalRevenue = 0.0
        var rating = 0.0

        try {
            // Get all orders and calculate vendor's portion
            val allOrders = getAllOrders()

            for (order in allOrders) {
                val orderDetails = getOrderDetails(order.orderId)
                var vendorOrderTotal = 0.0

                for (detail in orderDetails) {
                    val product = getProductById(detail.productId)
                    if (product?.vendorId == vendorId) {
                        val subtotal = detail.subtotal
                        val tax = subtotal * 0.06 // 6% tax
                        vendorOrderTotal += subtotal + tax
                        orderCount++
                    }
                }
                totalRevenue += vendorOrderTotal
            }

            // Get vendor rating from feedback
            val feedbacks = getFeedbackByVendor(vendorId)
            if (feedbacks.isNotEmpty()) {
                rating = feedbacks.map { it.rating }.average()
            }
        } catch (e: Exception) {
            // Handle error
        }

        return Triple(orderCount, totalRevenue, rating)
    }

    // Calculate customer statistics
    suspend fun calculateCustomerActualStats(customerId: String): Pair<Int, Double> {
        var orderCount = 0
        var totalSpent = 0.0

        try {
            val customerOrders = getCustomerOrders(customerId)
            orderCount = customerOrders.size
            totalSpent = customerOrders.sumOf { it.totalPrice }
        } catch (e: Exception) {
            // Handle error
        }

        return Pair(orderCount, totalSpent)
    }
// DatabaseService.kt

// ... inside DatabaseService class ...

    // REPLACEMENT: Robust Freeze/Unfreeze functions
    // In DatabaseService.kt, update the freeze functions:

    suspend fun freezeVendorAccount(vendorId: String): Result<Boolean> {
        return try {
            println("DEBUG: Freezing vendor account $vendorId")
            val updates = mapOf(
                "isFrozen" to true,
                "updatedAt" to Timestamp.now()
            )
            db.collection("vendors").document(vendorId).update(updates).await()
            println("DEBUG: Vendor account $vendorId frozen successfully")
            createFreezeLog(vendorId, "vendor", "Account frozen by admin")
            Result.success(true)
        } catch (e: Exception) {
            println("DEBUG: Error freezing vendor account $vendorId: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun unfreezeVendorAccount(vendorId: String): Result<Boolean> {
        return try {
            println("DEBUG: Unfreezing vendor account $vendorId")
            val updates = mapOf(
                "isFrozen" to false,
                "updatedAt" to Timestamp.now()
            )
            db.collection("vendors").document(vendorId).update(updates).await()
            println("DEBUG: Vendor account $vendorId unfrozen successfully")
            createFreezeLog(vendorId, "vendor", "Account unfrozen by admin")
            Result.success(true)
        } catch (e: Exception) {
            println("DEBUG: Error unfreezing vendor account $vendorId: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun freezeCustomerAccount(customerId: String): Result<Boolean> {
        return try {
            val updates = mapOf(
                "isFrozen" to true,
                "updatedAt" to Timestamp.now()
            )
            db.collection("customers").document(customerId).update(updates).await()
            createFreezeLog(customerId, "customer", "Account frozen by admin")
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unfreezeCustomerAccount(customerId: String): Result<Boolean> {
        return try {
            val updates = mapOf(
                "isFrozen" to false,
                "updatedAt" to Timestamp.now()
            )
            db.collection("customers").document(customerId).update(updates).await()
            createFreezeLog(customerId, "customer", "Account unfrozen by admin")
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

// ... rest of the file


    suspend fun updateCustomerFrozenStatus(customerId: String, isFrozen: Boolean): Result<Unit> {
        return try {
            db.collection("customers").document(customerId)
                .update(mapOf(
                    "isFrozen" to isFrozen,
                    "updatedAt" to Timestamp.now()
                )).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCustomer(customerId: String): Result<Unit> {
        return try {
            // Get customer first to find Firebase UID
            val customer = db.collection("customers").document(customerId).get().await()
                .toObject(Customer::class.java)

            if (customer != null) {
                // Find the mapping to get Firebase UID
                val mappings = db.collection("user_mappings")
                    .whereEqualTo("customerId", customerId)
                    .get().await()

                // Delete customer document
                db.collection("customers").document(customerId).delete().await()

                // Delete user mapping
                mappings.documents.forEach { doc ->
                    db.collection("user_mappings").document(doc.id).delete().await()
                }

                // Optionally delete auth user (requires admin privileges)
                // This would need Firebase Admin SDK on a server
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateVendorFrozenStatus(vendorId: String, isFrozen: Boolean): Result<Unit> {
        return try {
            db.collection("vendors").document(vendorId)
                .update(mapOf(
                    "isFrozen" to isFrozen,
                    "updatedAt" to FieldValue.serverTimestamp()
                )).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteVendor(vendorId: String): Result<Unit> {
        return try {
            // Get vendor first
            val vendor = db.collection("vendors").document(vendorId).get().await()
                .toObject(Vendor::class.java)

            if (vendor != null) {
                // Find the mapping to get Firebase UID
                val mappings = db.collection("user_mappings")
                    .whereEqualTo("vendorId", vendorId)
                    .get().await()

                // Delete vendor document
                db.collection("vendors").document(vendorId).delete().await()

                // Delete user mapping
                mappings.documents.forEach { doc ->
                    db.collection("user_mappings").document(doc.id).delete().await()
                }

                // Delete vendor's products, orders, etc. (optional)
                db.collection("products")
                    .whereEqualTo("vendorId", vendorId)
                    .get().await()
                    .documents.forEach { doc ->
                        db.collection("products").document(doc.id).delete().await()
                    }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Update user activity on login
    suspend fun updateCustomerLoginActivity(customerId: String): Result<Unit> {
        return try {
            db.collection("customers").document(customerId)
                .update(
                    mapOf(
                        "lastLogin" to Timestamp.now(),
                        "loginCount" to FieldValue.increment(1)
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateVendorLoginActivity(vendorId: String): Result<Unit> {
        return try {
            db.collection("vendors").document(vendorId)
                .update(
                    mapOf(
                        "lastLogin" to Timestamp.now(),
                        "loginCount" to FieldValue.increment(1)
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Update customer order stats when order is placed
    suspend fun updateCustomerOrderStats(customerId: String, orderAmount: Double): Result<Unit> {
        return try {
            val customerDoc = db.collection("customers").document(customerId)

            // Get current values first
            val currentDoc = customerDoc.get().await()
            val currentOrderCount = currentDoc.getLong("orderCount")?.toInt() ?: 0
            val currentTotalSpent = currentDoc.getDouble("totalSpent") ?: 0.0

            customerDoc.update(
                mapOf(
                    "orderCount" to (currentOrderCount + 1),
                    "totalSpent" to (currentTotalSpent + orderAmount),
                    "updatedAt" to Timestamp.now()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Update vendor order stats when order is placed
    suspend fun updateVendorOrderStats(vendorId: String, orderAmount: Double): Result<Unit> {
        return try {
            val vendorDoc = db.collection("vendors").document(vendorId)

            // Get current values first
            val currentDoc = vendorDoc.get().await()
            val currentOrderCount = currentDoc.getLong("orderCount")?.toInt() ?: 0
            val currentTotalRevenue = currentDoc.getDouble("totalRevenue") ?: 0.0

            vendorDoc.update(
                mapOf(
                    "orderCount" to (currentOrderCount + 1),
                    "totalRevenue" to (currentTotalRevenue + orderAmount),
                    "updatedAt" to Timestamp.now()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Product Operations
    suspend fun addProduct(product: Product): Result<String> {
        return try {
            val documentRef = db.collection("products").add(product).await()
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Boolean> {
        return try {
            db.collection("products").document(productId).delete().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProduct(product: Product): Result<Boolean> {
        return try {
            db.collection("products").document(product.productId)
                .update(
                    mapOf(
                        "productName" to product.productName,
                        "productPrice" to product.productPrice,
                        "description" to product.description,
                        "stock" to product.stock,
                        "imageUrl" to product.imageUrl,
                        "category" to product.category,
                        "updatedAt" to Timestamp.now()
                    )
                ).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProductById(productId: String): Product? {
        return try {
            db.collection("products").document(productId).get().await()
                .toObject(Product::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getProductsByVendor(vendorId: String): List<Product> {
        return try {
            db.collection("products")
                .whereEqualTo("vendorId", vendorId)
                .get()
                .await()
                .toObjects(Product::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllProducts(): List<Product> {
        return try {
            db.collection("products")
                .get()
                .await()
                .toObjects(Product::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateProductStock(productId: String, newStock: Int): Result<Boolean> {
        return try {
            db.collection("products").document(productId)
                .update("stock", newStock).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Order Operations
    suspend fun createOrder(order: Order, orderDetails: List<OrderDetail>): Result<String> {
        return try {
            // Generate custom order ID (O001 format)
            val orderId = Order.generateOrderId(db)

            // Create order with custom ID
            val orderWithId = order.copy(orderId = orderId)
            db.collection("orders").document(orderId).set(orderWithId).await()

            // Create order details
            orderDetails.forEach { detail ->
                db.collection("order_details").add(
                    detail.copy(orderId = orderId)
                ).await()
            }

            Result.success(orderId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createOrderWithDetails(orderRequest: OrderRequest): Result<String> {
        return try {
            // Generate custom order ID (O001 format)
            val orderId = Order.generateOrderId(db)

            // Create order with custom ID - SET INITIAL STATUS AS "pending"
            val order = Order(
                orderId = orderId,
                customerId = orderRequest.customerId,
                totalPrice = orderRequest.totalAmount,
                shippingAddress = orderRequest.deliveryAddress,
                paymentMethod = orderRequest.paymentMethod,
                status = "pending", // Start as pending
                orderDate = Timestamp.now()
            )

            // Store order using the custom orderId as document ID
            db.collection("orders").document(orderId).set(order).await()

            // Create order details
            orderRequest.items.forEach { item ->
                val orderDetail = OrderDetail(
                    orderId = orderId,
                    productId = item.productId,
                    productName = item.productName,
                    productPrice = item.productPrice,
                    quantity = item.quantity,
                    subtotal = item.productPrice * item.quantity
                )
                db.collection("order_details").add(orderDetail).await()
            }

            // Create payment record
            val payment = Payment(
                orderId = orderId,
                amount = orderRequest.totalAmount,
                paymentMethod = orderRequest.paymentMethod,
                paymentStatus = "pending",
                transactionDate = Timestamp.now()
            )
            db.collection("payments").add(payment).await()

            // Update customer order stats
            updateCustomerOrderStats(orderRequest.customerId, orderRequest.totalAmount)

            // Update vendor order stats (for each vendor in the order)
            orderRequest.items.groupBy { it.vendorId }.forEach { (vendorId, items) ->
                val vendorTotal = items.sumOf { it.productPrice * it.quantity }
                updateVendorOrderStats(vendorId, vendorTotal)
            }

            Result.success(orderId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markOrderAsDelivered(orderId: String): Result<Boolean> {
        return try {
            db.collection("orders").document(orderId)
                .update(
                    mapOf(
                        "status" to "delivered",
                        "updatedAt" to Timestamp.now()
                    )
                ).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePaymentStatus(orderId: String, status: String): Result<Boolean> {
        return try {
            // Update payment status
            val paymentQuery = db.collection("payments")
                .whereEqualTo("orderId", orderId)
                .get()
                .await()

            if (!paymentQuery.isEmpty) {
                val paymentDoc = paymentQuery.documents.first()
                paymentDoc.reference.update("paymentStatus", status).await()
            }

            // Update order status if payment is completed
            if (status == "completed") {
                db.collection("orders").document(orderId)
                    .update("status", "confirmed").await()
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCustomerOrders(customerId: String): List<Order> {
        return try {
            println("DEBUG: Getting ALL orders and filtering for customer: $customerId")

            val allOrders = db.collection("orders")
                .get()
                .await()

            println("DEBUG: Total orders in database: ${allOrders.documents.size}")

            val orders = mutableListOf<Order>()
            allOrders.documents.forEach { doc ->
                val data = doc.data ?: emptyMap()
                println("DEBUG: Order ${doc.id} fields: ${data.keys}")

                // Check all possible customer ID field names
                val custId = data["customerId"] as? String ?:
                data["CustomerID"] as? String ?:
                data["customerID"] as? String ?: ""

                if (custId == customerId) {
                    println("DEBUG: Found matching order for customer $customerId: ${doc.id}")
                    val order = Order(
                        orderId = data["orderId"] as? String ?: doc.id,
                        documentId = doc.id,
                        customerId = custId,
                        orderDate = data["orderDate"] as? Timestamp ?: Timestamp.now(),
                        status = data["status"] as? String ?: "pending",
                        totalPrice = (data["totalPrice"] as? Double) ?: 0.0,
                        shippingAddress = data["shippingAddress"] as? String ?: "",
                        paymentMethod = data["paymentMethod"] as? String ?: "",
                        createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now(),
                        updatedAt = data["updatedAt"] as? Timestamp ?: Timestamp.now()
                    )
                    orders.add(order)
                }
            }

            println("DEBUG: Found ${orders.size} orders for customer $customerId")
            orders
        } catch (e: Exception) {
            println("DEBUG: Error in getCustomerOrders: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getOrderDetails(orderId: String): List<OrderDetail> {
        return try {
            println("DEBUG: Getting order details for orderId: $orderId")

            val result = db.collection("order_details")
                .whereEqualTo("orderId", orderId)
                .get()
                .await()

            println("DEBUG: Found ${result.documents.size} order details for order: $orderId")

            result.toObjects(OrderDetail::class.java)
        } catch (e: Exception) {
            println("DEBUG: Error getting order details: ${e.message}")
            emptyList()
        }
    }

    suspend fun getAllOrders(): List<Order> {
        return try {
            db.collection("orders")
                .get()
                .await()
                .toObjects(Order::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getOrderById(orderId: String): Order? {
        return try {
            db.collection("orders").document(orderId).get().await()
                .toObject(Order::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateOrderStatus(orderId: String, status: String): Result<Boolean> {
        return try {
            db.collection("orders").document(orderId)
                .update(
                    mapOf(
                        "status" to status,
                        "updatedAt" to Timestamp.now()
                    )
                ).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Payment Operations
    suspend fun createPayment(payment: Payment): Result<String> {
        return try {
            val paymentRef = db.collection("payments").add(payment).await()

            // Update order payment status
            db.collection("orders").document(payment.orderId)
                .update("paymentMethod", payment.paymentMethod).await()

            Result.success(paymentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

// Add import at the top


    // Update getAllVendors function
    suspend fun getAllVendors(): List<Vendor> {
        return try {
            db.collection("vendors")
                .get(Source.SERVER)  // Force server fetch
                .await()
                .toObjects(Vendor::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Update getAllCustomers function
    suspend fun getAllCustomers(): List<Customer> {
        return try {
            db.collection("customers")
                .get(Source.SERVER)  // Force server fetch
                .await()
                .toObjects(Customer::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateVendorProfileImageBase64(vendorId: String, base64Image: String): Result<Boolean> {
        return try {
            db.collection("vendors").document(vendorId)
                .update("profileImageBase64", base64Image).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateVendorProfile(vendor: Vendor): Result<Boolean> {
        return try {
            db.collection("vendors").document(vendor.vendorId)
                .update(
                    mapOf(
                        "vendorName" to vendor.vendorName,
                        "email" to vendor.email,
                        "vendorContact" to vendor.vendorContact,
                        "address" to vendor.address,
                        "updatedAt" to Timestamp.now()
                    )
                ).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCustomerAccount(customerId: String): CustomerAccount? {
        return try {
            db.collection("customer_accounts")
                .document(customerId)
                .get()
                .await()
                .toObject(CustomerAccount::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateCustomerProfile(customer: Customer): Result<Boolean> {
        return try {
            db.collection("customers").document(customer.customerId)
                .update(
                    mapOf(
                        "name" to customer.name,
                        "phoneNumber" to customer.phoneNumber,
                        "email" to customer.email,
                        "updatedAt" to Timestamp.now()
                    )
                ).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCustomerCredit(customerId: String, newBalance: Double): Result<Boolean> {
        return try {
            val account = CustomerAccount(
                customerId = customerId,
                tapNChowCredit = newBalance,
                lastUpdated = Timestamp.now()
            )
            db.collection("customer_accounts")
                .document(customerId)
                .set(account)
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCustomerProfileImageBase64(customerId: String, base64Image: String): Result<Boolean> {
        return try {
            db.collection("customers").document(customerId)
                .update("profileImageBase64", base64Image).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPaymentByOrder(orderId: String): Payment? {
        return try {
            db.collection("payments")
                .whereEqualTo("orderId", orderId)
                .get()
                .await()
                .documents.firstOrNull()
                ?.toObject(Payment::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // Feedback Operations
    suspend fun addFeedback(feedback: Feedback): Result<String> {
        return try {
            println("🟡 DEBUG: Adding feedback to Firestore:")
            println("🟡 DEBUG: - Customer: ${feedback.customerName} (${feedback.customerId})")
            println("🟡 DEBUG: - Vendor: ${feedback.vendorName} (${feedback.vendorId})")
            println("🟡 DEBUG: - Order: ${feedback.orderId}")
            println("🟡 DEBUG: - Rating: ${feedback.rating}")
            println("🟡 DEBUG: - Comment: ${feedback.comment}")

            val feedbackRef = db.collection("feedbacks").add(feedback).await()

            println("🟢 DEBUG: Feedback saved with ID: ${feedbackRef.id}")

            // Update product rating summary
            updateProductRating(feedback.productId, feedback.rating)

            // Update vendor rating summary
            updateVendorRating(feedback.vendorId, feedback.rating)

            Result.success(feedbackRef.id)
        } catch (e: Exception) {
            println("🔴 DEBUG: Failed to save feedback: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun updateProductRating(productId: String, newRating: Double) {
        try {
            val feedbacks = db.collection("feedbacks")
                .whereEqualTo("productId", productId)
                .get()
                .await()
                .toObjects(Feedback::class.java)

            if (feedbacks.isNotEmpty()) {
                val averageRating = feedbacks.map { it.rating }.average()
                val ratingDistribution = mutableMapOf<Int, Int>()

                // Initialize distribution
                for (i in 1..5) ratingDistribution[i] = 0

                // Count ratings
                feedbacks.forEach { feedback ->
                    val starRating = feedback.rating.toInt().coerceIn(1, 5)
                    ratingDistribution[starRating] = ratingDistribution[starRating]!! + 1
                }

                val productRating = ProductRating(
                    productId = productId,
                    averageRating = averageRating,
                    totalRatings = feedbacks.size,
                    ratingDistribution = ratingDistribution,
                    lastUpdated = Timestamp.now()
                )

                db.collection("product_ratings").document(productId).set(productRating).await()
            }
        } catch (e: Exception) {
            // Handle error silently
        }
    }

    // Feedback reply functions
    suspend fun addVendorReply(feedbackId: String, reply: String): Result<Boolean> {
        return try {
            val updateData = hashMapOf<String, Any>(
                "vendorReply" to reply,
                "vendorReplyDate" to Timestamp.now(),
                "isReplied" to true,
                "replied" to true // Update both field names for compatibility
            )

            db.collection("feedbacks").document(feedbackId)
                .update(updateData).await()

            // Update analytics
            updateVendorAnalytics(feedbackId)

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateVendorReply(feedbackId: String, reply: String): Result<Boolean> {
        return try {
            val updateData = hashMapOf<String, Any>(
                "vendorReply" to reply,
                "vendorReplyDate" to Timestamp.now()
            )

            db.collection("feedbacks").document(feedbackId)
                .update(updateData).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteVendorReply(feedbackId: String): Result<Boolean> {
        return try {
            db.collection("feedbacks").document(feedbackId)
                .update(
                    mapOf(
                        "vendorReply" to "",
                        "vendorReplyDate" to null,
                        "isReplied" to false
                    )
                ).await()

            // Update analytics
            updateVendorAnalytics(feedbackId)

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Vendor Analytics functions
    private suspend fun updateVendorAnalytics(feedbackId: String) {
        try {
            val feedback = db.collection("feedbacks").document(feedbackId).get().await()
                .toObject(Feedback::class.java)

            feedback?.vendorId?.let { vendorId ->
                updateVendorAnalyticsData(vendorId)
            }
        } catch (e: Exception) {
            // Handle error silently
        }
    }

    suspend fun updateVendorAnalyticsData(vendorId: String) {
        try {
            val feedbacks = db.collection("feedbacks")
                .whereEqualTo("vendorId", vendorId)
                .whereEqualTo("isVisible", true)
                .get()
                .await()
                .toObjects(Feedback::class.java)

            if (feedbacks.isNotEmpty()) {
                val totalReviews = feedbacks.size
                val averageRating = feedbacks.map { it.rating }.average()
                val totalReplies = feedbacks.count { it.isReplied }
                val replyRate = if (totalReviews > 0) (totalReplies.toDouble() / totalReviews) * 100 else 0.0

                val ratingDistribution = mutableMapOf<Int, Int>()
                for (i in 1..5) ratingDistribution[i] = 0

                // Count ratings
                feedbacks.forEach { feedback ->
                    val starRating = feedback.rating.toInt().coerceIn(1, 5)
                    ratingDistribution[starRating] = ratingDistribution[starRating]!! + 1
                }

                val vendorAnalytics = VendorAnalytics(
                    vendorId = vendorId,
                    totalReviews = totalReviews,
                    averageRating = averageRating,
                    ratingDistribution = ratingDistribution,
                    totalReplies = totalReplies,
                    replyRate = replyRate,
                    lastUpdated = Timestamp.now()
                )

                db.collection("vendor_analytics").document(vendorId).set(vendorAnalytics).await()
            }
        } catch (e: Exception) {
            // Handle error silently
        }
    }

    suspend fun getVendorAnalytics(vendorId: String): VendorAnalytics? {
        return try {
            db.collection("vendor_analytics").document(vendorId).get().await()
                .toObject(VendorAnalytics::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getFeedbackWithReplies(vendorId: String): List<Feedback> {
        return try {
            println("DEBUG: Querying feedbacks for vendor: $vendorId")

            if (vendorId.isBlank()) {
                println("DEBUG: Vendor ID is blank!")
                return emptyList()
            }

            val result = db.collection("feedbacks")
                .whereEqualTo("vendorId", vendorId)
                .get()
                .await()

            println("DEBUG: Query completed. Found ${result.documents.size} documents")

            // Manual conversion to handle field name differences
            val feedbacks = result.documents.mapNotNull { document ->
                try {
                    val data = document.data ?: return@mapNotNull null

                    Feedback(
                        feedbackId = document.id,
                        customerId = data["customerId"] as? String ?: "",
                        customerName = data["customerName"] as? String ?: "",
                        vendorId = data["vendorId"] as? String ?: "",
                        vendorName = data["vendorName"] as? String ?: "",
                        orderId = data["orderId"] as? String ?: "",
                        productId = data["productId"] as? String ?: "",
                        productName = data["productName"] as? String ?: "",
                        rating = (data["rating"] as? Double) ?: (data["rating"] as? Long)?.toDouble() ?: 0.0,
                        comment = data["comment"] as? String ?: "",
                        feedbackDate = data["feedbackDate"] as? Timestamp ?: Timestamp.now(),
                        createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now(),
                        isVisible = data["isVisible"] as? Boolean ?: true,
                        vendorReply = data["vendorReply"] as? String ?: "",
                        vendorReplyDate = data["vendorReplyDate"] as? Timestamp,
                        isReplied = data["isReplied"] as? Boolean ?: (data["replied"] as? Boolean) ?: false
                    )
                } catch (e: Exception) {
                    println("DEBUG: Error converting document ${document.id}: ${e.message}")
                    null
                }
            }

            println("DEBUG: Successfully converted to ${feedbacks.size} Feedback objects")

            // Filter by isVisible
            val visibleFeedbacks = feedbacks.filter { it.isVisible }
            println("DEBUG: After filtering - ${visibleFeedbacks.size} visible feedbacks")

            // Sort by date, newest first
            val sortedFeedbacks = visibleFeedbacks.sortedByDescending { it.feedbackDate?.seconds ?: 0 }

            sortedFeedbacks
        } catch (e: Exception) {
            println("DEBUG: Error in getFeedbackWithReplies: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    private suspend fun updateVendorRating(vendorId: String, newRating: Double) {
        try {
            val feedbacks = db.collection("feedbacks")
                .whereEqualTo("vendorId", vendorId)
                .get()
                .await()
                .toObjects(Feedback::class.java)

            if (feedbacks.isNotEmpty()) {
                val averageRating = feedbacks.map { it.rating }.average()

                val vendorRating = VendorRating(
                    vendorId = vendorId,
                    averageRating = averageRating,
                    totalRatings = feedbacks.size,
                    lastUpdated = Timestamp.now()
                )

                db.collection("vendor_ratings").document(vendorId).set(vendorRating).await()
            }
        } catch (e: Exception) {
            // Handle error silently
        }
    }

    suspend fun getFeedbackByOrder(orderId: String): Feedback? {
        return try {
            db.collection("feedbacks")
                .whereEqualTo("orderId", orderId)
                .get()
                .await()
                .documents.firstOrNull()
                ?.toObject(Feedback::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getFeedbackByCustomer(customerId: String): List<Feedback> {
        return try {
            println("DEBUG: Getting feedback for customer: $customerId")

            val result = db.collection("feedbacks")
                .whereEqualTo("customerId", customerId)
                .get()
                .await()

            println("DEBUG: Query completed. Found ${result.documents.size} documents")

            if (result.documents.isEmpty()) {
                println("DEBUG: No feedback documents found for customer $customerId")
                return emptyList()
            }

            // Manual conversion to handle field name differences
            val feedbacks = result.documents.mapNotNull { document ->
                try {
                    val data = document.data ?: return@mapNotNull null

                    Feedback(
                        feedbackId = document.id,
                        customerId = data["customerId"] as? String ?: "",
                        customerName = data["customerName"] as? String ?: "",
                        vendorId = data["vendorId"] as? String ?: "",
                        vendorName = data["vendorName"] as? String ?: "",
                        orderId = data["orderId"] as? String ?: "",
                        productId = data["productId"] as? String ?: "",
                        productName = data["productName"] as? String ?: "",
                        rating = (data["rating"] as? Double) ?: (data["rating"] as? Long)?.toDouble() ?: 0.0,
                        comment = data["comment"] as? String ?: "",
                        feedbackDate = data["feedbackDate"] as? Timestamp ?: Timestamp.now(),
                        createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now(),
                        isVisible = data["isVisible"] as? Boolean ?: true,
                        vendorReply = data["vendorReply"] as? String ?: "",
                        vendorReplyDate = data["vendorReplyDate"] as? Timestamp,
                        isReplied = data["isReplied"] as? Boolean ?: (data["replied"] as? Boolean) ?: false
                    )
                } catch (e: Exception) {
                    println("DEBUG: Error converting document ${document.id}: ${e.message}")
                    null
                }
            }

            println("DEBUG: Successfully converted to ${feedbacks.size} Feedback objects")

            // Sort by date, newest first
            val sortedFeedbacks = feedbacks.sortedByDescending { it.feedbackDate?.seconds ?: 0 }
            println("DEBUG: Returning ${sortedFeedbacks.size} sorted feedbacks")

            sortedFeedbacks
        } catch (e: Exception) {
            println("DEBUG: Error in getFeedbackByCustomer: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getFeedbackByVendor(vendorId: String): List<Feedback> {
        return try {
            db.collection("feedbacks")
                .whereEqualTo("vendorId", vendorId)
                .whereEqualTo("isVisible", true)
                .orderBy("feedbackDate", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(Feedback::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getFeedbackByProduct(productId: String): List<Feedback> {
        return try {
            db.collection("feedbacks")
                .whereEqualTo("productId", productId)
                .whereEqualTo("isVisible", true)
                .orderBy("feedbackDate", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(Feedback::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getProductRating(productId: String): ProductRating? {
        return try {
            db.collection("product_ratings").document(productId).get().await()
                .toObject(ProductRating::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getVendorRating(vendorId: String): VendorRating? {
        return try {
            db.collection("vendor_ratings").document(vendorId).get().await()
                .toObject(VendorRating::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // Vendor Operations
    suspend fun getVendorById(vendorId: String): Vendor? {
        return try {
            db.collection("vendors").document(vendorId)
                .get(Source.SERVER) // <--- FORCE SERVER FETCH
                .await()
                .toObject(Vendor::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // Customer Operations
    suspend fun getCustomerById(customerId: String): Customer? {
        return try {
            db.collection("customers").document(customerId)
                .get(Source.SERVER) // <--- FORCE SERVER FETCH
                .await()
                .toObject(Customer::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getCustomerByFirebaseUid(firebaseUid: String): Customer? {
        return try {
            val mapping = db.collection("user_mappings").document(firebaseUid).get().await()
            val customerId = mapping.getString("customerId")
            customerId?.let {
                db.collection("customers").document(it).get().await()
                    .toObject(Customer::class.java)
            }
        } catch (e: Exception) {
            null
        }
    }
}