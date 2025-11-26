package com.example.miniproject.service

import android.net.Uri
import com.example.miniproject.model.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class DatabaseService {
    private val db = FirebaseFirestore.getInstance()

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
                        "updatedAt" to com.google.firebase.Timestamp.now()
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

            // Create order with custom ID
            val order = Order(
                orderId = orderId, // Set the custom order ID
                customerId = orderRequest.customerId,
                totalPrice = orderRequest.totalAmount,
                shippingAddress = orderRequest.deliveryAddress,
                paymentMethod = orderRequest.paymentMethod,
                status = "pending",
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

            Result.success(orderId)
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
                .update("status", status).await()
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


    suspend fun getAllVendors(): List<Vendor> {
        return try {
            db.collection("vendors")
                .get()
                .await()
                .toObjects(Vendor::class.java)
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
                        "updatedAt" to com.google.firebase.Timestamp.now()
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

    // Replace the upload function with this Base64 version
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
            val feedbackRef = db.collection("feedbacks").add(feedback).await()
            Result.success(feedbackRef.id)
        } catch (e: Exception) {
            Result.failure(e)
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

    suspend fun getFeedbackByProduct(productId: String): List<Feedback> {
        return try {
            // This would require joining with order_details to get product-specific feedback
            // For simplicity, we'll get all feedback and filter by product in the app
            db.collection("feedbacks")
                .get()
                .await()
                .toObjects(Feedback::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Vendor Operations
    suspend fun getVendorById(vendorId: String): Vendor? {
        return try {
            db.collection("vendors").document(vendorId).get().await()
                .toObject(Vendor::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // Customer Operations
    suspend fun getCustomerById(customerId: String): Customer? {
        return try {
            db.collection("customers").document(customerId).get().await()
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