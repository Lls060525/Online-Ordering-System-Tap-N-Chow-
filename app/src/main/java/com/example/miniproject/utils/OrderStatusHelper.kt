package com.example.miniproject.util

import androidx.compose.ui.graphics.Color
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

object OrderStatusHelper {

    fun formatOrderDate(timestamp: Timestamp): String {
        val date = timestamp.toDate()
        val formatter = SimpleDateFormat("EEE MMM dd yyyy 'at' HH:mm", Locale.getDefault())
        return formatter.format(date)
    }

    fun getStatusColor(status: String): Color {
        return when (status.lowercase()) {
            "pending" -> Color(0xFFFF9800) // Orange
            "confirmed" -> Color(0xFF2196F3) // Blue
            "preparing" -> Color(0xFF9C27B0) // Purple
            "ready" -> Color(0xFF673AB7) // Deep Purple
            "completed" -> Color(0xFF4CAF50) // Green
            "cancelled" -> Color(0xFFF44336) // Red
            else -> Color.Gray
        }
    }


    fun getStatusDisplayText(status: String): String {
        return when (status.lowercase()) {
            "pending" -> "Pending"
            "confirmed" -> "Confirmed"
            "preparing" -> "Preparing"
            "ready" -> "Ready for Pickup"
            "completed" -> "Completed"
            "cancelled" -> "Cancelled"
            else -> status.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
            }
        }
    }
}