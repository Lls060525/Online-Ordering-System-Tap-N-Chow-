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

    suspend fun forceSaveTestCredentials(): Boolean {
        return try {
            val email = "admin@admin.com.my"
            val password = "administrator"
            val credentials = "$email:$password"
            val encodedData = Base64.encodeToString(credentials.toByteArray(), Base64.DEFAULT)

            val prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(ENCRYPTED_ADMIN_CREDENTIALS_KEY, encodedData)
                .putString("iv", "test_iv")
                .apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun debugSharedPrefs() {
        val prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        println("DEBUG: Shared Prefs Contents: ${prefs.all}")
    }

    fun checkBiometricAvailability(): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NONE_ENROLLED
            else -> BiometricStatus.UNAVAILABLE
        }
    }

    fun hasSavedAdminCredentials(): Boolean {
        val prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(ENCRYPTED_ADMIN_CREDENTIALS_KEY)
    }

    fun clearSavedAdminCredentials() {
        val prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    // Save admin credentials
    suspend fun saveAdminCredentials(email: String, password: String): Boolean {
        // 1. Find the activity internally
        val activity = findFragmentActivity()
        if (activity == null) {
            println("DEBUG: Could not find FragmentActivity from context")
            return false
        }

        // 2. Switch to Main Thread for UI operations
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                try {
                    val secretKey = getOrCreateSecretKey()
                    val cipher = getCipher()
                    cipher.init(Cipher.ENCRYPT_MODE, secretKey)

                    val cryptoObject = BiometricPrompt.CryptoObject(cipher)

                    val biometricPrompt = BiometricPrompt(
                        activity,
                        ContextCompat.getMainExecutor(context),
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)
                                try {
                                    val resultCipher = result.cryptoObject?.cipher
                                    if (resultCipher == null) {
                                        continuation.resume(false)
                                        return
                                    }

                                    val credentials = "$email:$password"
                                    val encryptedData = resultCipher.doFinal(credentials.toByteArray(StandardCharsets.UTF_8))
                                    val iv = resultCipher.iv

                                    val prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
                                    val encodedData = Base64.encodeToString(encryptedData, Base64.DEFAULT)
                                    val encodedIv = Base64.encodeToString(iv, Base64.DEFAULT)

                                    prefs.edit()
                                        .putString(ENCRYPTED_ADMIN_CREDENTIALS_KEY, encodedData)
                                        .putString("iv", encodedIv)
                                        .apply()

                                    continuation.resume(true)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    continuation.resume(false)
                                }
                            }

                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                super.onAuthenticationError(errorCode, errString)
                                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                                ) {
                                    continuation.resume(false)
                                } else {
                                    continuation.cancel()
                                }
                            }
                        }
                    )

                    val promptInfo = BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Save Admin Credentials")
                        .setSubtitle("Touch the sensor to save securely")
                        .setNegativeButtonText("Cancel")
                        .build()

                    biometricPrompt.authenticate(promptInfo, cryptoObject)

                } catch (e: Exception) {
                    e.printStackTrace()
                    continuation.resume(false)
                }
            }
        }
    }

    // Authenticate admin
    suspend fun authenticateWithFingerprint(): Pair<String, String>? {
        val activity = findFragmentActivity()
        if (activity == null) return null

        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                try {
                    val prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
                    val encryptedData = prefs.getString(ENCRYPTED_ADMIN_CREDENTIALS_KEY, null)
                    val ivString = prefs.getString("iv", null)

                    if (encryptedData == null || ivString == null) {
                        continuation.resume(null)
                        return@suspendCancellableCoroutine
                    }

                    val secretKey = getOrCreateSecretKey()
                    val cipher = getCipher()
                    val iv = Base64.decode(ivString, Base64.DEFAULT)
                    val gcmSpec = GCMParameterSpec(128, iv)
                    cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

                    val cryptoObject = BiometricPrompt.CryptoObject(cipher)

                    val biometricPrompt = BiometricPrompt(
                        activity,
                        ContextCompat.getMainExecutor(context),
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                try {
                                    val resultCipher = result.cryptoObject?.cipher
                                    val encryptedBytes = Base64.decode(encryptedData, Base64.DEFAULT)
                                    val decryptedBytes = resultCipher?.doFinal(encryptedBytes)

                                    if (decryptedBytes != null) {
                                        val credentials = String(decryptedBytes, StandardCharsets.UTF_8)
                                        val parts = credentials.split(":")
                                        if (parts.size == 2) {
                                            continuation.resume(Pair(parts[0], parts[1]))
                                        } else {
                                            continuation.resume(null)
                                        }
                                    } else {
                                        continuation.resume(null)
                                    }
                                } catch (e: Exception) {
                                    continuation.resumeWithException(e)
                                }
                            }

                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                                ) {
                                    continuation.cancel()
                                } else {
                                    continuation.resume(null)
                                }
                            }
                        }
                    )

                    val promptInfo = BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Admin Login")
                        .setSubtitle("Touch sensor to login")
                        .setNegativeButtonText("Use Password")
                        .build()

                    biometricPrompt.authenticate(promptInfo, cryptoObject)

                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }
        }
    }

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