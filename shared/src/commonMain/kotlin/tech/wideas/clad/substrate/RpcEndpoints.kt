package tech.wideas.clad.substrate

import tech.wideas.clad.data.AccountMode

/**
 * RPC endpoint configuration for different account modes.
 *
 * Each account mode connects to a different network:
 * - LIVE: Production mainnet with real transactions
 * - DEMO: Testnet for safe experimentation
 *
 * See docs/live-demo-account-modes.md for design rationale.
 */
object RpcEndpoints {

    /**
     * Mainnet RPC endpoint for production (LIVE mode).
     */
    const val MAINNET = "wss://rpc.clad.so"

    /**
     * Testnet RPC endpoint for demo/training (DEMO mode).
     */
    const val TESTNET = "wss://testnet.clad.so"

    /**
     * Get the RPC endpoint for an account mode.
     *
     * @param mode The account mode
     * @return WebSocket URL for the appropriate network
     */
    fun forMode(mode: AccountMode): String = when (mode) {
        AccountMode.LIVE -> MAINNET
        AccountMode.DEMO -> TESTNET
    }
}
