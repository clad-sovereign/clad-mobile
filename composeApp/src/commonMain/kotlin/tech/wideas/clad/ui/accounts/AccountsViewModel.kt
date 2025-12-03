package tech.wideas.clad.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.wideas.clad.data.AccountInfo
import tech.wideas.clad.data.AccountRepository
import tech.wideas.clad.security.KeyStorage
import tech.wideas.clad.substrate.ConnectionState
import tech.wideas.clad.substrate.SubstrateClient

/**
 * Navigation state for accounts screen.
 */
sealed class AccountsScreenState {
    data object AccountList : AccountsScreenState()
    data object Import : AccountsScreenState()
}

/**
 * UI state for accounts screen.
 */
data class AccountsUiState(
    val accounts: List<AccountInfo> = emptyList(),
    val isLoading: Boolean = true,
    val screenState: AccountsScreenState = AccountsScreenState.AccountList,
    val error: String? = null
)

class AccountsViewModel(
    private val substrateClient: SubstrateClient,
    private val accountRepository: AccountRepository,
    private val keyStorage: KeyStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()

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

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            accountRepository.observeAll().collect { accounts ->
                _uiState.value = _uiState.value.copy(
                    accounts = accounts,
                    isLoading = false
                )
            }
        }
    }

    fun navigateToImport() {
        _uiState.value = _uiState.value.copy(
            screenState = AccountsScreenState.Import
        )
    }

    fun navigateToAccountList() {
        _uiState.value = _uiState.value.copy(
            screenState = AccountsScreenState.AccountList
        )
    }

    fun deleteAccount(account: AccountInfo) {
        viewModelScope.launch {
            try {
                // Delete keypair from secure storage (if exists)
                keyStorage.deleteKeypair(account.id)
                // Delete account metadata from database
                accountRepository.delete(account.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete account: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearMessages() {
        substrateClient.clearMessages()
    }
}
