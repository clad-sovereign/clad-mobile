package tech.wideas.clad.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.util.Log
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import tech.wideas.clad.crypto.Keypair
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.coroutines.resume

private val Context.keyDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "clad_encrypted_keys"
)

/**
 * Android implementation of [KeyStorage] using Android Keystore and BiometricPrompt.
 *
 * Security architecture:
 * 1. Each account has a dedicated AES-256-GCM key in Android Keystore
 * 2. Keys are configured with `setUserAuthenticationRequired(true)` for biometric binding
 * 3. Keypairs are serialized, encrypted with the account's Keystore key
 * 4. Encrypted data + IV stored in DataStore
 * 5. BiometricPrompt.CryptoObject binds biometric auth to actual crypto operation
 *
 * Hardware security:
 * - Uses StrongBox Keymaster when available (Pixel 3+, Samsung S10+)
 * - Falls back to TEE (Trusted Execution Environment) otherwise
 */
class AndroidKeyStorage(
    private val context: Context,
    private val activityProvider: () -> FragmentActivity
) : KeyStorage {

    companion object {
        private const val TAG = "AndroidKeyStorage"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS_PREFIX = "clad_biometric_key_"
        private const val GCM_TAG_LENGTH = 128

        // DataStore keys
        private val ACCOUNT_IDS_KEY = stringSetPreferencesKey("account_ids")

        private fun encryptedDataKey(accountId: String) =
            byteArrayPreferencesKey("encrypted_$accountId")

        private fun ivKey(accountId: String) =
            byteArrayPreferencesKey("iv_$accountId")
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
    }

    override suspend fun isAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    override suspend fun isHardwareBackedAvailable(): Boolean = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return@withContext false
        }

        try {
            val testAlias = "clad_strongbox_test"
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
            )

            val spec = KeyGenParameterSpec.Builder(
                testAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setIsStrongBoxBacked(true)
                .build()

            keyGenerator.init(spec)
            keyGenerator.generateKey()

            // Clean up test key
            keyStore.deleteEntry(testAlias)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun saveKeypair(
        accountId: String,
        keypair: Keypair,
        promptConfig: BiometricPromptConfig
    ): KeyStorageResult<Unit> {
        return try {
            // Generate or retrieve the encryption key for this account
            val keyAlias = "$KEY_ALIAS_PREFIX$accountId"
            val secretKey = getOrCreateSecretKey(keyAlias)

            // Initialize cipher for encryption
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            // Authenticate with biometric and encrypt
            val authResult = authenticateWithCrypto(cipher, promptConfig)

            when (authResult) {
                is CryptoAuthResult.Success -> {
                    val authenticatedCipher = authResult.cipher
                    val serialized = KeypairSerializer.serialize(keypair)
                    val encrypted = authenticatedCipher.doFinal(serialized)
                    val iv = authenticatedCipher.iv

                    // Store encrypted data and IV
                    saveEncryptedData(accountId, encrypted, iv)
                    KeyStorageResult.Success(Unit)
                }
                is CryptoAuthResult.Cancelled -> KeyStorageResult.BiometricCancelled
                is CryptoAuthResult.NotAvailable -> KeyStorageResult.BiometricNotAvailable
                is CryptoAuthResult.Error -> KeyStorageResult.BiometricError(authResult.message)
            }
        } catch (e: Exception) {
            KeyStorageResult.StorageError(e.message ?: "Failed to save keypair")
        }
    }

    override suspend fun getKeypair(
        accountId: String,
        promptConfig: BiometricPromptConfig
    ): KeyStorageResult<Keypair> {
        return try {
            // Load encrypted data and IV
            val (encrypted, iv) = loadEncryptedData(accountId)
                ?: return KeyStorageResult.KeyNotFound

            // Get the encryption key for this account
            val keyAlias = "$KEY_ALIAS_PREFIX$accountId"
            val secretKey = keyStore.getKey(keyAlias, null) as? SecretKey
                ?: return KeyStorageResult.KeyNotFound

            // Initialize cipher for decryption with the stored IV
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            // Authenticate with biometric and decrypt
            val authResult = authenticateWithCrypto(cipher, promptConfig)

            when (authResult) {
                is CryptoAuthResult.Success -> {
                    val authenticatedCipher = authResult.cipher
                    val decrypted = authenticatedCipher.doFinal(encrypted)
                    val keypair = KeypairSerializer.deserialize(decrypted)
                    KeyStorageResult.Success(keypair)
                }
                is CryptoAuthResult.Cancelled -> KeyStorageResult.BiometricCancelled
                is CryptoAuthResult.NotAvailable -> KeyStorageResult.BiometricNotAvailable
                is CryptoAuthResult.Error -> KeyStorageResult.BiometricError(authResult.message)
            }
        } catch (e: Exception) {
            KeyStorageResult.StorageError(e.message ?: "Failed to retrieve keypair")
        }
    }

    override suspend fun deleteKeypair(accountId: String): KeyStorageResult<Unit> {
        return try {
            // Delete from Keystore
            val keyAlias = "$KEY_ALIAS_PREFIX$accountId"
            if (keyStore.containsAlias(keyAlias)) {
                keyStore.deleteEntry(keyAlias)
            }

            // Delete from DataStore
            context.keyDataStore.edit { prefs ->
                prefs.remove(encryptedDataKey(accountId))
                prefs.remove(ivKey(accountId))

                // Remove from account IDs set
                val currentIds = prefs[ACCOUNT_IDS_KEY] ?: emptySet()
                prefs[ACCOUNT_IDS_KEY] = currentIds - accountId
            }

            KeyStorageResult.Success(Unit)
        } catch (e: Exception) {
            KeyStorageResult.StorageError(e.message ?: "Failed to delete keypair")
        }
    }

    override suspend fun hasKeypair(accountId: String): Boolean {
        val keyAlias = "$KEY_ALIAS_PREFIX$accountId"
        return keyStore.containsAlias(keyAlias)
    }

    override suspend fun listAccountIds(): List<String> {
        return context.keyDataStore.data.map { prefs ->
            prefs[ACCOUNT_IDS_KEY]?.toList() ?: emptyList()
        }.first()
    }

    /**
     * Get or create a biometric-protected encryption key in Android Keystore.
     */
    private suspend fun getOrCreateSecretKey(keyAlias: String): SecretKey =
        withContext(Dispatchers.IO) {
            // Return existing key if present
            keyStore.getKey(keyAlias, null)?.let { return@withContext it as SecretKey }

            // Generate new key with biometric protection
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
            )

            val specBuilder = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true)

            // Set authentication parameters (Android R+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                specBuilder.setUserAuthenticationParameters(
                    0, // Require auth for every use
                    KeyProperties.AUTH_BIOMETRIC_STRONG
                )
            }

            // Try StrongBox first (Android P+), fall back to TEE if unavailable
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    specBuilder.setIsStrongBoxBacked(true)
                    keyGenerator.init(specBuilder.build())
                    val key = keyGenerator.generateKey()
                    Log.d(TAG, "Key generated with StrongBox: $keyAlias")
                    return@withContext key
                } catch (e: android.security.keystore.StrongBoxUnavailableException) {
                    Log.d(TAG, "StrongBox unavailable, falling back to TEE: ${e.message}")
                    specBuilder.setIsStrongBoxBacked(false)
                } catch (e: Exception) {
                    Log.d(TAG, "StrongBox key generation failed, falling back to TEE: ${e.message}")
                    specBuilder.setIsStrongBoxBacked(false)
                }
            }

            // Generate key without StrongBox (TEE-backed)
            keyGenerator.init(specBuilder.build())
            keyGenerator.generateKey()
        }

    /**
     * Perform biometric authentication with a crypto object.
     */
    private suspend fun authenticateWithCrypto(
        cipher: Cipher,
        promptConfig: BiometricPromptConfig
    ): CryptoAuthResult = suspendCancellableCoroutine { continuation ->
        val activity = activityProvider()
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                val authenticatedCipher = result.cryptoObject?.cipher
                if (authenticatedCipher != null) {
                    continuation.resume(CryptoAuthResult.Success(authenticatedCipher))
                } else {
                    continuation.resume(CryptoAuthResult.Error("Cipher not available after authentication"))
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                val result = when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_CANCELED -> CryptoAuthResult.Cancelled

                    BiometricPrompt.ERROR_NO_BIOMETRICS,
                    BiometricPrompt.ERROR_HW_NOT_PRESENT,
                    BiometricPrompt.ERROR_HW_UNAVAILABLE -> CryptoAuthResult.NotAvailable

                    else -> CryptoAuthResult.Error(errString.toString())
                }
                continuation.resume(result)
            }

            override fun onAuthenticationFailed() {
                // Don't resume - user can retry
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(promptConfig.title)
            .setSubtitle(promptConfig.subtitle)
            .setDescription(promptConfig.promptDescription)
            .setNegativeButtonText(promptConfig.negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        val cryptoObject = BiometricPrompt.CryptoObject(cipher)
        biometricPrompt.authenticate(promptInfo, cryptoObject)

        continuation.invokeOnCancellation {
            biometricPrompt.cancelAuthentication()
        }
    }

    /**
     * Save encrypted keypair data to DataStore.
     */
    private suspend fun saveEncryptedData(accountId: String, encrypted: ByteArray, iv: ByteArray) {
        context.keyDataStore.edit { prefs ->
            prefs[encryptedDataKey(accountId)] = encrypted
            prefs[ivKey(accountId)] = iv

            // Add to account IDs set
            val currentIds = prefs[ACCOUNT_IDS_KEY] ?: emptySet()
            prefs[ACCOUNT_IDS_KEY] = currentIds + accountId
        }
    }

    /**
     * Load encrypted keypair data from DataStore.
     */
    private suspend fun loadEncryptedData(accountId: String): Pair<ByteArray, ByteArray>? {
        return context.keyDataStore.data.map { prefs ->
            val encrypted = prefs[encryptedDataKey(accountId)]
            val iv = prefs[ivKey(accountId)]

            if (encrypted != null && iv != null) {
                encrypted to iv
            } else {
                null
            }
        }.first()
    }
}

/**
 * Internal result type for biometric authentication with crypto object.
 */
private sealed class CryptoAuthResult {
    data class Success(val cipher: Cipher) : CryptoAuthResult()
    data object Cancelled : CryptoAuthResult()
    data object NotAvailable : CryptoAuthResult()
    data class Error(val message: String) : CryptoAuthResult()
}
