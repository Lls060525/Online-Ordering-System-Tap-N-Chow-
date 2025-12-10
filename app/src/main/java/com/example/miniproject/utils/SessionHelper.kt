package com.example.miniproject.utils

import com.example.miniproject.service.AuthService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object SessionHelper {
    suspend fun checkAndEnforceValidSession(): Boolean {
        val authService = AuthService()
        return try {
            // Check if user is logged in
            val currentUser = authService.getCurrentUser()
            if (currentUser == null) {
                return false
            }

            // Check if user is frozen
            val isFrozen = authService.isUserFrozen()
            if (isFrozen) {
                // Force logout if frozen
                authService.logout()
                return false
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}