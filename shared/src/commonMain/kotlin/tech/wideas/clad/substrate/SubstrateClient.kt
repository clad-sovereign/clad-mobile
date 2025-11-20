package tech.wideas.clad.substrate

import co.touchlab.kermit.Logger
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import tech.wideas.clad.currentTimeMillis

/**
 * Connection state of the Substrate client
 */
sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

/**
 * RPC request/response models
 */
@Serializable
data class RpcRequest(
    val jsonrpc: String = "2.0",
    val id: Int,
    val method: String,
    val params: JsonArray = JsonArray(emptyList())
)

@Serializable
data class RpcResponse(
    val jsonrpc: String,
    val id: Int? = null,
    val result: JsonElement? = null,
    val error: JsonElement? = null
)

/**
 * Substrate RPC client using WebSocket
 *
 * @param autoReconnect Enable automatic reconnection on connection loss
 * @param maxReconnectAttempts Maximum number of reconnection attempts
 * @param dispatcher The coroutine dispatcher to use (defaults to Dispatchers.Default)
 *
 * Note: This client should be scoped to a ViewModel's lifecycle to ensure
 * proper coroutine cancellation. The internal scope will be created automatically.
 */
class SubstrateClient(
    private val autoReconnect: Boolean = true,
    private val maxReconnectAttempts: Int = 5,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val logger = Logger.withTag("SubstrateClient")
    private var scope: CoroutineScope = CoroutineScope(dispatcher + SupervisorJob())
    private val client = HttpClient {
        install(WebSockets)
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private var session: DefaultClientWebSocketSession? = null
    private var requestId = 0
    private val pendingRequests = mutableMapOf<Int, CompletableDeferred<RpcResponse>>()
    private val pendingRequestsMutex = Mutex()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _metadata = MutableStateFlow<String?>(null)
    val metadata: StateFlow<String?> = _metadata.asStateFlow()

    data class NodeMessage(
        val timestamp: Long,
        val direction: Direction,
        val content: String
    ) {
        enum class Direction { SENT, RECEIVED }
    }

    private val _messages = MutableStateFlow<List<NodeMessage>>(emptyList())
    val messages: StateFlow<List<NodeMessage>> = _messages.asStateFlow()
    private val maxMessages = 50 // Keep last 50 messages (UI displays last 5)

    private var currentEndpoint: String? = null
    private var reconnectAttempts = 0
    private var reconnectJob: Job? = null

    private fun addMessage(direction: NodeMessage.Direction, content: String) {
        val message = NodeMessage(
            timestamp = currentTimeMillis(),
            direction = direction,
            content = content
        )
        _messages.value = (_messages.value + message).takeLast(maxMessages)
        logger.d { "Message added: ${direction} - ${content.take(50)}... Total messages: ${_messages.value.size}" }
    }

    /**
     * Clear all collected messages
     */
    fun clearMessages() {
        _messages.value = emptyList()
    }

    /**
     * Set a custom coroutine scope (e.g., viewModelScope).
     * This should be called before connect() to tie the client's lifecycle to the scope.
     *
     * @param customScope The coroutine scope to use (typically viewModelScope)
     */
    fun setScope(customScope: CoroutineScope) {
        // Cancel existing scope if any operations are running
        scope.cancel()
        scope = customScope
    }

    /**
     * Connect to Substrate node
     */
    suspend fun connect(endpoint: String) {
        if (_connectionState.value is ConnectionState.Connected) {
            return // Already connected
        }

        currentEndpoint = endpoint
        reconnectAttempts = 0
        reconnectJob?.cancel()

        performConnect()
    }

    private suspend fun performConnect() {
        val endpoint = currentEndpoint ?: return

        logger.d { "Attempting to connect to: $endpoint" }
        _connectionState.value = ConnectionState.Connecting

        try {
            session = client.webSocketSession(endpoint)
            logger.d { "WebSocket session established successfully" }
            _connectionState.value = ConnectionState.Connected
            reconnectAttempts = 0 // Reset on successful connection

            // Start listening for responses
            scope.launch {
                listenForResponses()
            }

            // Fetch metadata and chain info after WebSocket is fully established
            scope.launch {
                // Wait for first incoming frame to confirm connection is ready
                delay(100)
                try {
                    // Fetch chain properties first (lighter request)
                    logger.d { "Fetching chain properties..." }
                    getChainProperties()
                    logger.d { "Chain properties fetched" }
                } catch (e: Exception) {
                    logger.e(e) { "Failed to fetch chain properties: ${e.message}" }
                }
                // Then fetch metadata
                fetchMetadata()

                // Subscribe to new block headers for real-time updates
                try {
                    logger.d { "Subscribing to new block headers..." }
                    subscribeNewHeads()
                    logger.d { "Subscribed to new block headers" }
                } catch (e: Exception) {
                    logger.e(e) { "Failed to subscribe to new heads: ${e.message}" }
                }

                // Subscribe to finalized blocks
                try {
                    logger.d { "Subscribing to finalized blocks..." }
                    subscribeFinalizedHeads()
                    logger.d { "Subscribed to finalized blocks" }
                } catch (e: Exception) {
                    logger.e(e) { "Failed to subscribe to finalized heads: ${e.message}" }
                }
            }
        } catch (e: Exception) {
            logger.e(e) { "Connection failed to $endpoint: ${e.message}" }
            _connectionState.value = ConnectionState.Error(e.message ?: "Connection failed")
            attemptReconnect(e)
        }
    }

    private fun attemptReconnect(error: Exception) {
        if (!autoReconnect || reconnectAttempts >= maxReconnectAttempts) {
            return
        }

        reconnectAttempts++
        val delayMs = calculateBackoffDelay(reconnectAttempts)

        reconnectJob = scope.launch {
            delay(delayMs)
            performConnect()
        }
    }

    private fun calculateBackoffDelay(attempt: Int): Long {
        // Exponential backoff: 1s, 2s, 4s, 8s, 16s
        val baseDelay = 1000L
        val maxDelay = 16000L
        val calculatedDelay = baseDelay * (1 shl (attempt - 1))
        return minOf(calculatedDelay, maxDelay)
    }

    /**
     * Disconnect from Substrate node
     */
    suspend fun disconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
        currentEndpoint = null

        // Cancel all pending requests
        pendingRequestsMutex.withLock {
            pendingRequests.values.forEach {
                it.completeExceptionally(SubstrateException("Client disconnected"))
            }
            pendingRequests.clear()
        }

        session?.close()
        session = null
        _connectionState.value = ConnectionState.Disconnected
        _metadata.value = null
        _messages.value = emptyList()
    }

    /**
     * Make an RPC call with timeout
     */
    suspend fun call(
        method: String,
        params: JsonArray = JsonArray(emptyList()),
        timeoutMs: Long = 30_000
    ): JsonElement? {
        val currentSession = session ?: throw IllegalStateException("Not connected")

        val id = ++requestId
        val request = RpcRequest(id = id, method = method, params = params)
        val requestJson = json.encodeToString(RpcRequest.serializer(), request)

        // Create a deferred result for this specific request
        val deferred = CompletableDeferred<RpcResponse>()
        pendingRequestsMutex.withLock {
            pendingRequests[id] = deferred
        }

        try {
            currentSession.send(Frame.Text(requestJson))
            addMessage(NodeMessage.Direction.SENT, requestJson)

            // Wait for response with timeout
            val response = try {
                withTimeout(timeoutMs) {
                    deferred.await()
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                throw SubstrateException("RPC call timeout after ${timeoutMs}ms")
            }

            // Check for RPC errors
            if (response.error != null) {
                throw SubstrateException("RPC error: ${response.error}")
            }

            return response.result
        } finally {
            // Clean up the pending request
            pendingRequestsMutex.withLock {
                pendingRequests.remove(id)
            }
        }
    }

    /**
     * Fetch runtime metadata from the node
     */
    suspend fun fetchMetadata() {
        try {
            logger.d { "Fetching metadata from node..." }
            val result = call("state_getMetadata", JsonArray(emptyList()))
            _metadata.value = result?.jsonPrimitive?.content
            logger.d { "Metadata fetched successfully" }
        } catch (e: Exception) {
            // Metadata fetch failed, but don't disconnect
            logger.e(e) { "Failed to fetch metadata: ${e.message}" }
        }
    }

    /**
     * Subscribe to new block headers
     * This will send continuous updates as new blocks are produced
     */
    suspend fun subscribeNewHeads() {
        call("chain_subscribeNewHeads", JsonArray(emptyList()))
    }

    /**
     * Subscribe to finalized block headers
     * This will send updates when blocks are finalized
     */
    suspend fun subscribeFinalizedHeads() {
        call("chain_subscribeFinalizedHeads", JsonArray(emptyList()))
    }

    /**
     * Get chain properties (name, token symbol, decimals, etc.)
     */
    suspend fun getChainProperties(): Map<String, JsonElement> {
        val result = call("system_properties")
        return if (result is JsonObject) {
            result.toMap()
        } else {
            emptyMap()
        }
    }

    /**
     * Get account balance
     */
    suspend fun getBalance(address: String): String? {
        val result = call(
            "system_accountNextIndex",
            JsonArray(listOf(JsonPrimitive(address)))
        )
        return result?.jsonPrimitive?.content
    }

    private suspend fun listenForResponses() {
        val currentSession = session ?: return

        logger.d { "Started listening for WebSocket responses" }
        try {
            for (frame in currentSession.incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    addMessage(NodeMessage.Direction.RECEIVED, text)
                    try {
                        val response = json.decodeFromString<RpcResponse>(text)
                        // Complete the deferred corresponding to this response ID
                        val id = response.id
                        if (id != null) {
                            val deferred = pendingRequestsMutex.withLock {
                                pendingRequests[id]
                            }
                            if (deferred != null) {
                                deferred.complete(response)
                            } else {
                                // Response for unknown request ID (might be a subscription or duplicate)
                                logger.w { "Received response for unknown request ID: $id" }
                            }
                        } else {
                            // Response without ID (might be a notification)
                            logger.d { "Received response without ID: $text" }
                        }
                    } catch (e: Exception) {
                        logger.w(e) { "Failed to parse response: $text" }
                    }
                }
            }
        } catch (e: Exception) {
            // Connection lost - attempt reconnection if enabled
            logger.e(e) { "Connection lost while listening for responses: ${e.message}" }
            _connectionState.value = ConnectionState.Error(e.message ?: "Connection lost")
            attemptReconnect(e)
        }
    }

    fun close() {
        scope.cancel()
        client.close()
    }
}

class SubstrateException(message: String) : Exception(message)
