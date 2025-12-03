package tech.wideas.clad.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tech.wideas.clad.data.AccountInfo

/**
 * Account details screen showing full account information.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailsScreen(
    account: AccountInfo,
    isActive: Boolean,
    onBack: () -> Unit,
    onSetActive: () -> Unit,
    onDelete: () -> Unit,
    onUpdateLabel: (String) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditLabelDialog by remember { mutableStateOf(false) }
    var editedLabel by remember { mutableStateOf(account.label) }
    val clipboardManager = LocalClipboardManager.current
    var showCopiedMessage by remember { mutableStateOf(false) }

    // Reset copied message after delay
    LaunchedEffect(showCopiedMessage) {
        if (showCopiedMessage) {
            kotlinx.coroutines.delay(2000)
            showCopiedMessage = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Account Details",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showEditLabelDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit label"
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete account",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Account header with active status
            AccountHeader(
                account = account,
                isActive = isActive,
                onSetActive = onSetActive
            )

            HorizontalDivider()

            // Address section
            AddressSection(
                address = account.address,
                onCopy = {
                    clipboardManager.setText(AnnotatedString(account.address))
                    showCopiedMessage = true
                },
                showCopiedMessage = showCopiedMessage
            )

            HorizontalDivider()

            // Account info section
            AccountInfoSection(account = account)
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account?") },
            text = {
                Text("Are you sure you want to delete \"${account.label}\"? This action cannot be undone and will remove the keypair from secure storage.")
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

    // Edit label dialog
    if (showEditLabelDialog) {
        AlertDialog(
            onDismissRequest = { showEditLabelDialog = false },
            title = { Text("Rename Account") },
            text = {
                OutlinedTextField(
                    value = editedLabel,
                    onValueChange = { editedLabel = it },
                    label = { Text("Account Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editedLabel.isNotBlank()) {
                            onUpdateLabel(editedLabel.trim())
                            showEditLabelDialog = false
                        }
                    },
                    enabled = editedLabel.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    editedLabel = account.label
                    showEditLabelDialog = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AccountHeader(
    account: AccountInfo,
    isActive: Boolean,
    onSetActive: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.label,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            text = account.keyType.name,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    enabled = false
                )

                if (isActive) {
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            leadingIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        if (!isActive) {
            Button(
                onClick = onSetActive,
                modifier = Modifier.padding(start = 16.dp)
            ) {
                Text("Set Active")
            }
        }
    }
}

@Composable
private fun AddressSection(
    address: String,
    onCopy: () -> Unit,
    showCopiedMessage: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Address",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showCopiedMessage) {
                        Text(
                            text = "Copied!",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }

                    FilledTonalButton(
                        onClick = onCopy,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy Address")
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountInfoSection(account: AccountInfo) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Information",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoRow(
                    label = "Key Type",
                    value = account.keyType.name
                )

                InfoRow(
                    label = "Created",
                    value = formatTimestamp(account.createdAt)
                )

                val lastUsed = account.lastUsedAt
                if (lastUsed != null) {
                    InfoRow(
                        label = "Last Used",
                        value = formatTimestamp(lastUsed)
                    )
                } else {
                    InfoRow(
                        label = "Last Used",
                        value = "Never"
                    )
                }

                InfoRow(
                    label = "Account ID",
                    value = account.id.take(8) + "..."
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Format timestamp to human-readable string.
 * Uses a simple relative time format. Each platform's native UI
 * can provide more sophisticated formatting if needed.
 */
private fun formatTimestamp(epochMillis: Long): String {
    val now = tech.wideas.clad.currentTimeMillis()
    val diff = now - epochMillis

    return when {
        diff < 60_000L -> "Just now"
        diff < 3_600_000L -> "${diff / 60_000L} min ago"
        diff < 86_400_000L -> "${diff / 3_600_000L} hours ago"
        diff < 604_800_000L -> "${diff / 86_400_000L} days ago"
        else -> {
            val days = diff / 86_400_000L
            when {
                days < 30 -> "$days days ago"
                days < 365 -> "${days / 30} months ago"
                else -> "${days / 365} years ago"
            }
        }
    }
}
