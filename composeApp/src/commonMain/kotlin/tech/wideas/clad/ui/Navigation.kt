package tech.wideas.clad.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.koin.compose.viewmodel.koinViewModel
import tech.wideas.clad.ui.accounts.AccountsScreen
import tech.wideas.clad.ui.connection.ConnectionScreen
import tech.wideas.clad.ui.connection.ConnectionViewModel

sealed class Screen(val route: String) {
    data object Connection : Screen("connection")
    data object Accounts : Screen("accounts")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Connection.route
    ) {
        composable(Screen.Connection.route) {
            val viewModel = koinViewModel<ConnectionViewModel>()
            ConnectionScreen(
                viewModel = viewModel,
                onConnected = {
                    navController.navigate(Screen.Accounts.route) {
                        popUpTo(Screen.Connection.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Accounts.route) {
            AccountsScreen()
        }
    }
}
