package tech.wideas.clad.data

import tech.wideas.clad.security.SecureStorage

/**
 * Repository for app settings
 */
class SettingsRepository(private val secureStorage: SecureStorage) {

    companion object {
        private const val KEY_RPC_ENDPOINT = "rpc_endpoint"
        const val DEFAULT_RPC_ENDPOINT_ANDROID = "ws://10.0.2.2:9944"
        const val DEFAULT_RPC_ENDPOINT_IOS = "ws://127.0.0.1:9944"
    }

    /**
     * Save RPC endpoint
     */
    suspend fun saveRpcEndpoint(endpoint: String) {
        secureStorage.save(KEY_RPC_ENDPOINT, endpoint)
    }

    /**
     * Get saved RPC endpoint, or default if none saved
     */
    suspend fun getRpcEndpoint(defaultEndpoint: String): String {
        return secureStorage.get(KEY_RPC_ENDPOINT) ?: defaultEndpoint
    }

    /**
     * Check if RPC endpoint is configured
     */
    suspend fun hasRpcEndpoint(): Boolean {
        return secureStorage.contains(KEY_RPC_ENDPOINT)
    }
}
