package com.example.miniproject.service

import android.content.Context
import android.content.ContextWrapper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class BiometricAuthService(private val context: Context) {

    companion object {
        private const val KEY_NAME = "admin_fingerprint_key"
        private const val KEYSTORE_NAME = "AndroidKeyStore"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val SHARED_PREFS_NAME = "admin_auth_prefs"
        private const val ENCRYPTED_ADMIN_CREDENTIALS_KEY = "encrypted_admin_credentials"
    }

    enum class BiometricStatus {
        AVAILABLE,
        UNAVAILABLE,
        NO_HARDWARE,
        NONE_ENROLLED
    }

    // --- HELPER TO FIND ACTIVITY ---
    fun findFragmentActivity(): FragmentActivity? {
        var currentContext = context
        while (currentContext is ContextWrapper) {
            if (currentContext is FragmentActivity) {
                return currentContext
            }
            currentContext = currentContext.baseContext
        }
        return null
    }



    fun checkBiometricAvailability(): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NONE_ENROLLED
            else -> BiometricStatus.UNAVAILABLE
        }
    }

    // --- Generic Payment Authorization ---
    suspend fun authorizePayment(amount: Double): Boolean {
        val activity = findFragmentActivity()
        if (activity == null) return false

        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                try {
                    val biometricPrompt = BiometricPrompt(
                        activity,
                        ContextCompat.getMainExecutor(context),
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)
                                // User authenticated successfully
                                continuation.resume(true)
                            }

                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                super.onAuthenticationError(errorCode, errString)
                                // Prevent crashing on cancel/back button
                                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                                    continuation.resume(false)
                                } else {
                                    continuation.resume(false)
                                }
                            }

                            override fun onAuthenticationFailed() {
                                super.onAuthenticationFailed()

                            }
                        }
                    )

                    val promptInfo = BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Confirm Payment")
                        .setSubtitle("Touch sensor to pay RM ${String.format("%.2f", amount)}")
                        .setNegativeButtonText("Cancel")
                        .build()

                    biometricPrompt.authenticate(promptInfo)

                } catch (e: Exception) {
                    e.printStackTrace()
                    continuation.resume(false)
                }
            }
        }
    }
}