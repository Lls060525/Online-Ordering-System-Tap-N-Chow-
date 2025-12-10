package com.example.miniproject.util

import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object GeocodingHelper {
    suspend fun getAddressFromCoordinates(context: Context, lat: Double, lng: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocation(lat, lng, 1)
                if (!results.isNullOrEmpty()) {
                    results[0].getAddressLine(0)
                } else "Unknown Location"
            } catch (e: Exception) {
                "Unable to get address"
            }
        }
    }
}