package tech.wideas.clad.substrate

import kotlinx.coroutines.flow.first

/**
 * Test helper constants and extension functions for SubstrateClient integration tests.
 */

/**
 * Maximum time to wait for a state transition in tests before timing out.
 * This is a safety bound for event-driven waiting, not an arbitrary delay.
 */
const val TEST_STATE_TRANSITION_TIMEOUT_MS = 1000L

/**
 * Wait for the client to reach Connected state.
 * This is an event-driven wait that completes as soon as the state is reached.
 */
suspend fun SubstrateClient.waitForConnection() {
    connectionState.first { it is ConnectionState.Connected }
}

/**
 * Wait for the client to reach Disconnected state.
 * This is an event-driven wait that completes as soon as the state is reached.
 */
suspend fun SubstrateClient.waitForDisconnection() {
    connectionState.first { it is ConnectionState.Disconnected }
}

/**
 * Wait for metadata to be fetched and become available.
 * This is an event-driven wait that completes as soon as metadata is non-null.
 */
suspend fun SubstrateClient.waitForMetadata() {
    metadata.first { it != null }
}
