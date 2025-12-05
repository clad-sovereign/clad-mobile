package tech.wideas.clad.substrate

/**
 * iOS RPC endpoint configuration.
 *
 * Uses 127.0.0.1 localhost for iOS simulator development.
 * Both LIVE and DEMO point to the same local node during development.
 *
 * TODO: Update to production endpoints when mainnet/testnet are deployed:
 * - mainnet: wss://rpc.clad.so
 * - testnet: wss://testnet.clad.so
 */
actual object RpcEndpointConfig {
    actual val mainnet: String = "ws://127.0.0.1:9944"
    actual val testnet: String = "ws://127.0.0.1:9944"
}
