package tech.wideas.clad.ui.accounts

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.uuid.ExperimentalUuidApi
import org.koin.compose.viewmodel.koinViewModel
import tech.wideas.clad.data.AccountInfo
import tech.wideas.clad.substrate.ConnectionState
import tech.wideas.clad.substrate.SubstrateClient
import tech.wideas.clad.ui.import.ImportScreen

@OptIn(ExperimentalUuidApi::class)
@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val messages by viewModel.messages.collectAsState()

    // Handle navigation between account list and import
    when (uiState.screenState) {
        is AccountsScreenState.AccountList -> {
            AccountListContent(
                accounts = uiState.accounts,
                connectionState = connectionState,
                messages = messages,
                isLoading = uiState.isLoading,
                error = uiState.error,
                onImportClick = { viewModel.navigateToImport() },
                onDeleteAccount = { account -> viewModel.deleteAccount(account) },
                onClearError = { viewModel.clearError() }
            )
        }
        is AccountsScreenState.Import -> {
            ImportScreen(
                onDismiss = { viewModel.navigateToAccountList() },
                onImportComplete = { viewModel.navigateToAccountList() }
            )
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
private fun AccountListContent(
    accounts: List<AccountInfo>,
    connectionState: ConnectionState,
    messages: List<SubstrateClient.NodeMessage>,
    isLoading: Boolean,
    error: String?,
    onImportClick: () -> Unit,
    onDeleteAccount: (AccountInfo) -> Unit,
    onClearError: () -> Unit
) {
    val lazyListState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onImportClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Import Account"
                )
            }
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Text(
                    text = "Accounts",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Connection status
                ConnectionStatusBar(connectionState = connectionState)

                Spacer(modifier = Modifier.height(16.dp))

                // Error snackbar
                if (error != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onClearError) {
                                Text("Dismiss")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Account list or empty state
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (accounts.isEmpty()) {
                    EmptyAccountsState(onImportClick = onImportClick)
                } else {
                    // Account list
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = accounts,
                            key = { it.id }
                        ) { account ->
                            AccountCard(
                                account = account,
                                onDelete = { onDeleteAccount(account) }
                            )
                        }
                    }
                }

                // Node stream (collapsed when accounts exist)
                if (connectionState is ConnectionState.Connected && accounts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    NodeStreamCompact(
                        messages = messages,
                        lazyListState = lazyListState
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusBar(connectionState: ConnectionState) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = when (connectionState) {
                        is ConnectionState.Connected -> MaterialTheme.colorScheme.tertiary.copy(
                            alpha = if (connectionState is ConnectionState.Connected) alpha else 1f
                        )
                        is ConnectionState.Connecting -> Color(0xFFD4A574)
                        is ConnectionState.Disconnected -> MaterialTheme.colorScheme.onSurfaceVariant
                        is ConnectionState.Error -> MaterialTheme.colorScheme.error
                    },
                    shape = MaterialTheme.shapes.small
                )
        )

        Text(
            text = when (connectionState) {
                is ConnectionState.Connected -> "Connected to node"
                is ConnectionState.Connecting -> "Connecting..."
                is ConnectionState.Disconnected -> "Disconnected"
                is ConnectionState.Error -> "Connection error"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyAccountsState(onImportClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No Accounts Yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Import an account to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onImportClick) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Import Account")
        }
    }
}

@Composable
private fun AccountCard(
    account: AccountInfo,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatAddressShort(account.address),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            text = account.keyType.name,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    enabled = false,
                    modifier = Modifier.height(24.dp)
                )
            }

            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete account",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account?") },
            text = {
                Text("Are you sure you want to delete \"${account.label}\"? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
private fun NodeStreamCompact(
    messages: List<SubstrateClient.NodeMessage>,
    lazyListState: androidx.compose.foundation.lazy.LazyListState
) {
    Column {
        Text(
            text = "Node Stream",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                state = lazyListState,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(
                    items = messages.takeLast(3),
                    key = { message -> message.id.toString() }
                ) { message ->
                    NodeMessageItemCompact(message)
                }
            }
        }
    }
}

@Composable
private fun NodeMessageItemCompact(message: SubstrateClient.NodeMessage) {
    val (displayText, color) = formatMessage(message)

    Text(
        text = displayText,
        style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 11.sp
        ),
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

private fun formatMessage(message: SubstrateClient.NodeMessage): Pair<String, Color> {
    val content = message.content

    return when (message.direction) {
        SubstrateClient.NodeMessage.Direction.SENT -> {
            val text = when {
                content.contains("\"method\":\"system_properties\"") ->
                    "→ Requesting chain properties"
                content.contains("\"method\":\"state_getMetadata\"") ->
                    "→ Requesting chain metadata"
                content.contains("\"method\":\"chain_subscribeNewHeads\"") ->
                    "→ Subscribing to new blocks"
                content.contains("\"method\":\"chain_subscribeFinalizedHeads\"") ->
                    "→ Subscribing to finalized blocks"
                else -> "→ Sending request"
            }
            Pair(text, Color(0xFF5B8DBE))
        }
        SubstrateClient.NodeMessage.Direction.RECEIVED -> {
            val text = when {
                content.contains("\"method\":\"chain_newHead\"") -> {
                    val blockNum = content.substringAfter("\"number\":\"0x", "")
                        .substringBefore("\"", "")
                    if (blockNum.isNotEmpty()) {
                        val decimal = blockNum.toLongOrNull(16) ?: 0
                        "← Block #$decimal"
                    } else {
                        "← New block"
                    }
                }
                content.contains("\"method\":\"chain_finalizedHead\"") -> {
                    val blockNum = content.substringAfter("\"number\":\"0x", "")
                        .substringBefore("\"", "")
                    if (blockNum.isNotEmpty()) {
                        val decimal = blockNum.toLongOrNull(16) ?: 0
                        "Finalized #$decimal"
                    } else {
                        "← Finalized"
                    }
                }
                else -> "← Response"
            }
            Pair(text, Color(0xFF0A8C6B))
        }
    }
}

private fun formatAddressShort(address: String): String {
    return if (address.length > 16) {
        "${address.take(8)}...${address.takeLast(8)}"
    } else {
        address
    }
}
