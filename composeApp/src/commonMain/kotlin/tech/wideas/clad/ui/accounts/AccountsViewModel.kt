package tech.wideas.clad.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    data class AccountDetails(val accountId: String) : AccountsScreenState()
}

/**
 * UI state for accounts screen.
 */
data class AccountsUiState(
    val accounts: List<AccountInfo> = emptyList(),
    val activeAccountId: String? = null,
    val isLoading: Boolean = true,
    val screenState: AccountsScreenState = AccountsScreenState.AccountList,
    val error: String? = null
) {
    val selectedAccount: AccountInfo?
        get() = (screenState as? AccountsScreenState.AccountDetails)?.let { state ->
            accounts.find { it.id == state.accountId }
        }
}

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
            combine(
                accountRepository.observeAll(),
                accountRepository.observeActiveAccountId()
            ) { accounts, activeId ->
                Pair(accounts, activeId)
            }.collect { (accounts, activeId) ->
                _uiState.value = _uiState.value.copy(
                    accounts = accounts,
                    activeAccountId = activeId,
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
                // Clear active account if we're deleting the active one
                if (_uiState.value.activeAccountId == account.id) {
                    accountRepository.setActiveAccount(null)
                }
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

    fun navigateToAccountDetails(accountId: String) {
        _uiState.value = _uiState.value.copy(
            screenState = AccountsScreenState.AccountDetails(accountId)
        )
    }

    fun setActiveAccount(accountId: String) {
        viewModelScope.launch {
            try {
                accountRepository.setActiveAccount(accountId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to set active account: ${e.message}"
                )
            }
        }
    }

    fun updateAccountLabel(accountId: String, newLabel: String) {
        viewModelScope.launch {
            try {
                accountRepository.updateLabel(accountId, newLabel)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update account label: ${e.message}"
                )
            }
        }
    }
}
