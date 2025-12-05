package tech.wideas.clad.substrate

import tech.wideas.clad.data.AccountMode

/**
 * RPC endpoint configuration for different account modes.
 *
 * Each account mode connects to a different network:
 * - LIVE: Production mainnet with real transactions
 * - DEMO: Testnet for safe experimentation
 *
 * Endpoints are configurable per platform via [RpcEndpointConfig].
 * Default values point to localhost for development.
 *
 * See docs/live-demo-account-modes.md for design rationale.
 */
object RpcEndpoints {

    /**
     * Get the RPC endpoint for an account mode.
     *
     * @param mode The account mode
     * @return WebSocket URL for the appropriate network
     */
    fun forMode(mode: AccountMode): String = when (mode) {
        AccountMode.LIVE -> RpcEndpointConfig.mainnet
        AccountMode.DEMO -> RpcEndpointConfig.testnet
    }
}

/**
 * Platform-specific RPC endpoint configuration.
 *
 * Override these values in platform-specific implementations to configure
 * production endpoints. Defaults to localhost for development.
 *
 * Android: 10.0.2.2 is the emulator's alias for host machine localhost
 * iOS: 127.0.0.1 is localhost on simulator
 *
 * Production values (when testnet/mainnet are ready):
 * - mainnet: wss://rpc.clad.so
 * - testnet: wss://testnet.clad.so
 */
expect object RpcEndpointConfig {
    /**
     * Mainnet RPC endpoint for production (LIVE mode).
     * Default: localhost for development
     */
    val mainnet: String

    /**
     * Testnet RPC endpoint for demo/training (DEMO mode).
     * Default: localhost for development
     */
    val testnet: String
}
