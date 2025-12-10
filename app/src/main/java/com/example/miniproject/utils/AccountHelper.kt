package com.example.miniproject.utils

import com.example.miniproject.service.DatabaseService
import com.google.firebase.Timestamp

object AccountHelper {

    // ONLY FOR VENDORS AND CUSTOMERS - NO ADMIN FREEZE FUNCTIONS

    suspend fun freezeVendorAccount(vendorId: String): Boolean {
        val dbService = DatabaseService()
        return try {
            dbService.freezeVendorAccount(vendorId).isSuccess
        } catch (e: Exception) {
            false
        }
    }

    suspend fun unfreezeVendorAccount(vendorId: String): Boolean {
        val dbService = DatabaseService()
        return try {
            dbService.unfreezeVendorAccount(vendorId).isSuccess
        } catch (e: Exception) {
            false
        }
    }

    suspend fun freezeCustomerAccount(customerId: String): Boolean {
        val dbService = DatabaseService()
        return try {
            dbService.freezeCustomerAccount(customerId).isSuccess
        } catch (e: Exception) {
            false
        }
    }

    suspend fun unfreezeCustomerAccount(customerId: String): Boolean {
        val dbService = DatabaseService()
        return try {
            dbService.unfreezeCustomerAccount(customerId).isSuccess
        } catch (e: Exception) {
            false
        }
    }
}
