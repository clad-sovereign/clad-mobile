package tech.wideas.clad.substrate

/**
 * Android RPC endpoint configuration.
 *
 * Uses 10.0.2.2 which is the Android emulator's alias for host machine localhost.
 * Both LIVE and DEMO point to the same local node during development.
 *
 * TODO: Update to production endpoints when mainnet/testnet are deployed:
 * - mainnet: wss://rpc.clad.so
 * - testnet: wss://testnet.clad.so
 */
actual object RpcEndpointConfig {
    actual val mainnet: String = "ws://10.0.2.2:9944"
    actual val testnet: String = "ws://10.0.2.2:9944"
}
