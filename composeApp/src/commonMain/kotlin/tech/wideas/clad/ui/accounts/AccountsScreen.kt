package tech.wideas.clad.ui.accounts

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.uuid.ExperimentalUuidApi
import org.koin.compose.viewmodel.koinViewModel
import tech.wideas.clad.substrate.ConnectionState
import tech.wideas.clad.substrate.SubstrateClient

@OptIn(ExperimentalUuidApi::class)
@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel = koinViewModel()
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val lazyListState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Accounts",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Account management coming in Phase 1B",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (connectionState) {
                                is ConnectionState.Connected -> "Connected to node"
                                is ConnectionState.Connecting -> "Connecting..."
                                is ConnectionState.Disconnected -> "Disconnected"
                                is ConnectionState.Error -> "Connection error"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Connection status indicator with pulsing size animation for connected state
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val size by infiniteTransition.animateFloat(
                            initialValue = 10f,
                            targetValue = 12f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(800, easing = LinearOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "size"
                        )

                        Box(
                            modifier = Modifier
                                .size(if (connectionState is ConnectionState.Connected) size.dp else 12.dp)
                                .background(
                                    color = when (connectionState) {
                                        is ConnectionState.Connected -> MaterialTheme.colorScheme.tertiary
                                        is ConnectionState.Connecting -> Color(0xFFD4A574) // Muted amber
                                        is ConnectionState.Disconnected -> MaterialTheme.colorScheme.onSurfaceVariant
                                        is ConnectionState.Error -> MaterialTheme.colorScheme.error
                                    },
                                    shape = MaterialTheme.shapes.small
                                )
                        )
                    }

                    if (connectionState is ConnectionState.Connected) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "✓ Substrate RPC connection established",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "✓ Metadata fetched successfully",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Node Stream",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Streaming messages container
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(125.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.background,
                                    shape = MaterialTheme.shapes.small
                                )
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                state = lazyListState,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Show only last 5 messages with slide-up animation
                                items(
                                    items = messages.takeLast(5),
                                    key = { message -> message.id.toString() }
                                ) { message ->
                                    Box(
                                        modifier = Modifier.animateItem(
                                            fadeInSpec = tween(300),
                                            fadeOutSpec = tween(300),
                                            placementSpec = tween(300)
                                        )
                                    ) {
                                        NodeMessageItem(message)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NodeMessageItem(message: SubstrateClient.NodeMessage) {
    val (displayText, color) = formatMessage(message)

    Text(
        text = displayText,
        style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 13.sp
        ),
        color = color,
        maxLines = 2
    )
}

private fun formatMessage(message: SubstrateClient.NodeMessage): Pair<String, Color> {
    val content = message.content

    return when (message.direction) {
        SubstrateClient.NodeMessage.Direction.SENT -> {
            // Parse sent messages
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
            Pair(text, Color(0xFF5B8DBE)) // Muted institutional blue
        }
        SubstrateClient.NodeMessage.Direction.RECEIVED -> {
            // Parse received messages
            val text = when {
                content.contains("\"method\":\"chain_newHead\"") -> {
                    // Extract block number and hash
                    val blockNum = content.substringAfter("\"number\":\"0x", "")
                        .substringBefore("\"", "")
                    val hash = content.substringAfter("\"parentHash\":\"0x", "")
                        .substringBefore("\"", "")
                        .take(8)

                    if (blockNum.isNotEmpty()) {
                        val decimal = blockNum.toLongOrNull(16) ?: 0
                        "← Imported block #$decimal (0x$hash...)"
                    } else {
                        "← New block produced"
                    }
                }
                content.contains("\"method\":\"chain_finalizedHead\"") -> {
                    // Extract block number
                    val blockNum = content.substringAfter("\"number\":\"0x", "")
                        .substringBefore("\"", "")
                    if (blockNum.isNotEmpty()) {
                        val decimal = blockNum.toLongOrNull(16) ?: 0
                        "✨ Block #$decimal finalized"
                    } else {
                        "← Block finalized"
                    }
                }
                content.contains("\"result\":{}") ->
                    "← Chain properties received"
                content.contains("\"result\":\"0x6d657461") ->
                    "📦 Metadata received"
                content.contains("\"result\":\"") && content.length < 150 ->
                    "✓ Subscription active"
                else -> "← Response received"
            }
            Pair(text, Color(0xFF0A8C6B)) // Emerald green (government "secure/verified" standard)
        }
    }
}
