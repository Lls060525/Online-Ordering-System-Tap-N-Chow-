package com.example.miniproject.service

import android.util.Log
import com.example.miniproject.model.Customer
import com.example.miniproject.model.Order
import com.example.miniproject.model.OrderDetail
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

object EmailService {

    suspend fun sendReceiptEmail(
        customer: Customer,
        order: Order,
        orderDetails: List<OrderDetail>
    ): Boolean {
        return try {
            val db = FirebaseFirestore.getInstance()


            val emailData = hashMapOf(
                "to" to listOf(customer.email),
                "message" to hashMapOf(
                    "subject" to "Payment Successful: Order #${order.orderId}",
                    "html" to generateHtmlReceipt(customer, order, orderDetails)
                )
            )


            db.collection("mail")
                .add(emailData)
                .await()

            Log.d("EmailService", "Email request added to Firestore for ${customer.email}")
            true
        } catch (e: Exception) {
            Log.e("EmailService", "Failed to queue email: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    // genera HTML
    private fun generateHtmlReceipt(
        customer: Customer,
        order: Order,
        items: List<OrderDetail>
    ): String {
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        val orderDate = dateFormat.format(order.orderDate.toDate())

        val itemsHtml = StringBuilder()
        items.forEach { item ->
            itemsHtml.append("""
                <tr>
                    <td style="padding: 8px; border-bottom: 1px solid #eee;">${item.productName} (x${item.quantity})</td>
                    <td style="padding: 8px; border-bottom: 1px solid #eee; text-align: right;">RM ${"%.2f".format(item.subtotal)}</td>
                </tr>
            """)
        }

        return """
            <html>
            <body style="font-family: Arial, sans-serif; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 8px;">
                    <h2 style="color: #4CAF50; text-align: center;">Payment Successful!</h2>
                    <p>Hi ${customer.name},</p>
                    <p>Thank you for your order. Here is your receipt.</p>
                    
                    <div style="background-color: #f9f9f9; padding: 15px; border-radius: 5px; margin: 20px 0;">
                        <p><strong>Order ID:</strong> ${order.orderId}</p>
                        <p><strong>Date:</strong> $orderDate</p>
                        <p><strong>Payment Method:</strong> ${order.paymentMethod.uppercase()}</p>
                    </div>

                    <table style="width: 100%; border-collapse: collapse;">
                        <thead>
                            <tr style="background-color: #eee;">
                                <th style="padding: 8px; text-align: left;">Item</th>
                                <th style="padding: 8px; text-align: right;">Price</th>
                            </tr>
                        </thead>
                        <tbody>
                            $itemsHtml
                        </tbody>
                        <tfoot>
                            <tr>
                                <td style="padding: 8px; font-weight: bold; text-align: right;">Total</td>
                                <td style="padding: 8px; font-weight: bold; text-align: right; color: #4CAF50;">RM ${"%.2f".format(order.totalPrice)}</td>
                            </tr>
                        </tfoot>
                    </table>

                    <div style="margin-top: 30px; text-align: center; font-size: 12px; color: #888;">
                        <p>This is an automated receipt from Tap N Chow.</p>
                        <p>If you have any questions, please contact support.</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}