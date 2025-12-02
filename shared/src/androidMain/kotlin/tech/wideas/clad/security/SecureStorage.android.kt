package tech.wideas.clad.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.secureDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "clad_secure_storage"
)

/**
 * Android implementation of [SecureStorage] using DataStore + Tink.
 *
 * Security architecture:
 * 1. Data is encrypted using Tink's AEAD (Authenticated Encryption with Associated Data)
 * 2. Encryption key is stored in Android Keystore (hardware-backed when available)
 * 3. Uses AES256-GCM for encryption
 * 4. DataStore provides type-safe, coroutine-based persistence
 *
 * This replaces the deprecated EncryptedSharedPreferences approach.
 *
 * Note: This storage does NOT require biometric authentication.
 * For biometric-protected key storage, use [KeyStorage] instead.
 */
class AndroidSecureStorage(private val context: Context) : SecureStorage {

    companion object {
        private const val KEYSET_NAME = "clad_secure_storage_keyset"
        private const val KEYSET_PREF_NAME = "clad_secure_storage_keyset_prefs"
        private const val MASTER_KEY_URI = "android-keystore://clad_secure_storage_master_key"

        init {
            AeadConfig.register()
        }
    }

    private val aead: Aead by lazy {
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, KEYSET_PREF_NAME)
            .withKeyTemplate(AeadKeyTemplates.AES256_GCM)
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle

        // Suppressed: getPrimitive(Class) is marked deprecated in Tink 1.19.0 but no
        // replacement API is documented. This remains the recommended pattern per
        // https://developers.google.com/tink/generate-encrypted-keyset
        @Suppress("DEPRECATION")
        keysetHandle.getPrimitive(Aead::class.java)
    }

    override suspend fun save(key: String, value: String): Unit = withContext(Dispatchers.IO) {
        val encrypted = aead.encrypt(value.encodeToByteArray(), key.encodeToByteArray())
        context.secureDataStore.edit { prefs ->
            prefs[byteArrayPreferencesKey(key)] = encrypted
        }
        Unit
    }

    override suspend fun get(key: String): String? = withContext(Dispatchers.IO) {
        context.secureDataStore.data.map { prefs ->
            prefs[byteArrayPreferencesKey(key)]
        }.first()?.let { encrypted ->
            try {
                aead.decrypt(encrypted, key.encodeToByteArray()).decodeToString()
            } catch (e: Exception) {
                // Decryption failed - data corrupted or key changed
                null
            }
        }
    }

    override suspend fun delete(key: String): Unit = withContext(Dispatchers.IO) {
        context.secureDataStore.edit { prefs ->
            prefs.remove(byteArrayPreferencesKey(key))
        }
        Unit
    }

    override suspend fun contains(key: String): Boolean = withContext(Dispatchers.IO) {
        context.secureDataStore.data.map { prefs ->
            prefs.contains(byteArrayPreferencesKey(key))
        }.first()
    }

    override suspend fun clear(): Unit = withContext(Dispatchers.IO) {
        context.secureDataStore.edit { prefs ->
            prefs.clear()
        }
        Unit
    }
}

actual fun createSecureStorage(): SecureStorage {
    throw UnsupportedOperationException(
        "SecureStorage is now managed by Koin dependency injection. " +
        "Use koinInject<SecureStorage>() or constructor injection instead."
    )
}
