package tech.wideas.clad.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import tech.wideas.clad.substrate.ConnectionState
import tech.wideas.clad.substrate.SubstrateClient

/**
 * iOS-specific AccountsViewModel
 * Mirrors the Android AccountsViewModel but lives in the shared module
 */
class AccountsViewModelIOS(
    private val substrateClient: SubstrateClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val connectionState: StateFlow<ConnectionState> = substrateClient.connectionState
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = substrateClient.connectionState.value
        )

    val messages: StateFlow<List<SubstrateClient.NodeMessage>> = substrateClient.messages
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = substrateClient.messages.value
        )

    fun clearMessages() {
        substrateClient.clearMessages()
    }
}
