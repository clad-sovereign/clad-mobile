package tech.wideas.clad.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android implementation using EncryptedSharedPreferences
 */
class AndroidSecureStorage(private val context: Context) : SecureStorage {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "clad_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override suspend fun save(key: String, value: String) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    override suspend fun get(key: String): String? = withContext(Dispatchers.IO) {
        sharedPreferences.getString(key, null)
    }

    override suspend fun delete(key: String) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().remove(key).apply()
    }

    override suspend fun contains(key: String): Boolean = withContext(Dispatchers.IO) {
        sharedPreferences.contains(key)
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        sharedPreferences.edit().clear().apply()
    }
}

actual fun createSecureStorage(): SecureStorage {
    throw UnsupportedOperationException(
        "SecureStorage is now managed by Koin dependency injection. " +
        "Use koinInject<SecureStorage>() or constructor injection instead."
    )
}
