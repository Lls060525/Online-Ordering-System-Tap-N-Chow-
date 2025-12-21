package com.example.miniproject.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RatingDisplay(
    rating: Double,
    totalRatings: Int? = null,
    size: Dp = 16.dp,
    showText: Boolean = true
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Star rating
        StarRatingDisplay(rating = rating, size = size)

        // Rating text
        if (showText) {
            Text(
                text = "%.1f".format(rating),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Total ratings count
            totalRatings?.let { count ->
                Text(
                    text = "($count)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StarRatingDisplay(rating: Double, size: Dp = 16.dp) {
    val totalStars = 5
    val fullStars = rating.toInt()
    val fractionalPart = rating - fullStars

    Row {
        // Full stars
        for (i in 0 until fullStars) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(size),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        // Half star if needed
        if (fractionalPart > 0.25) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(size),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        }

        // Empty stars
        val emptyStars = totalStars - fullStars - if (fractionalPart > 0.25) 1 else 0
        for (i in 0 until emptyStars) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(size),
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}