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

    // ... (Keep your existing admin functions: forceSaveTestCredentials, saveAdminCredentials, etc.) ...

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

    // --- NEW FUNCTION: Generic Payment Authorization ---
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
                                // Fingerprint didn't match, let them try again (don't resume yet)
                            }
                        }
                    )

                    val promptInfo = BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Confirm Payment")
                        .setSubtitle("Touch sensor to pay RM ${String.format("%.2f", amount)}")
                        .setNegativeButtonText("Cancel")
                        .build()

                    // We don't need a CryptoObject for simple verification,
                    // just checking if the user is present is enough for this level of security.
                    biometricPrompt.authenticate(promptInfo)

                } catch (e: Exception) {
                    e.printStackTrace()
                    continuation.resume(false)
                }
            }
        }
    }

    // ... (Keep getOrCreateSecretKey, createSecretKey, getCipher private functions) ...
    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_NAME)
        keyStore.load(null)
        if (!keyStore.containsAlias(KEY_NAME)) createSecretKey()
        return keyStore.getKey(KEY_NAME, null) as SecretKey
    }

    private fun createSecretKey() {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_NAME
        )
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_NAME,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
            .build()
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    private fun getCipher(): Cipher {
        return Cipher.getInstance(CIPHER_TRANSFORMATION)
    }
}