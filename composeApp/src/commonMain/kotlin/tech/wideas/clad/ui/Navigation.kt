package tech.wideas.clad.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel
import tech.wideas.clad.substrate.ConnectionState
import tech.wideas.clad.ui.accounts.AccountsScreen
import tech.wideas.clad.ui.accounts.AccountsViewModel
import tech.wideas.clad.ui.connection.ConnectionScreen
import tech.wideas.clad.ui.connection.ConnectionViewModel

@Composable
fun AppNavigation() {
    // Create ViewModels once at the top level (like iOS ContentView)
    val connectionViewModel = koinViewModel<ConnectionViewModel>()
    val accountsViewModel = koinViewModel<AccountsViewModel>()

    // Observe connection state
    val connectionState by connectionViewModel.uiState.collectAsState()

    // Simple conditional rendering based on connection state (like iOS)
    if (connectionState.connectionState is ConnectionState.Connected) {
        AccountsScreen(viewModel = accountsViewModel)
    } else {
        ConnectionScreen(viewModel = connectionViewModel)
    }
}
