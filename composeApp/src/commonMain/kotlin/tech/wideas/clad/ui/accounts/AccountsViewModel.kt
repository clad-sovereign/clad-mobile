package tech.wideas.clad.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import tech.wideas.clad.substrate.ConnectionState
import tech.wideas.clad.substrate.SubstrateClient

class AccountsViewModel(
    private val substrateClient: SubstrateClient
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = substrateClient.connectionState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = substrateClient.connectionState.value
        )

    val messages: StateFlow<List<SubstrateClient.NodeMessage>> = substrateClient.messages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = substrateClient.messages.value
        )

    fun clearMessages() {
        substrateClient.clearMessages()
    }
}
