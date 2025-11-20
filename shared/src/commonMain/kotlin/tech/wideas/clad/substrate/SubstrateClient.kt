package tech.wideas.clad.substrate

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

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
 */
class SubstrateClient(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
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
    }

    private var session: DefaultClientWebSocketSession? = null
    private var requestId = 0
    private val responseChannel = Channel<RpcResponse>(Channel.UNLIMITED)

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _metadata = MutableStateFlow<String?>(null)
    val metadata: StateFlow<String?> = _metadata.asStateFlow()

    /**
     * Connect to Substrate node
     */
    suspend fun connect(endpoint: String) {
        if (_connectionState.value is ConnectionState.Connected) {
            return // Already connected
        }

        _connectionState.value = ConnectionState.Connecting

        try {
            session = client.webSocketSession(endpoint)
            _connectionState.value = ConnectionState.Connected

            // Start listening for responses
            scope.launch {
                listenForResponses()
            }

            // Fetch metadata immediately after connecting (in background)
            scope.launch {
                delay(500) // Small delay to ensure connection is fully established
                fetchMetadata()
            }
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e.message ?: "Connection failed")
            throw e
        }
    }

    /**
     * Disconnect from Substrate node
     */
    suspend fun disconnect() {
        session?.close()
        session = null
        _connectionState.value = ConnectionState.Disconnected
        _metadata.value = null
    }

    /**
     * Make an RPC call
     */
    suspend fun call(method: String, params: JsonArray = JsonArray(emptyList())): JsonElement? {
        val currentSession = session ?: throw IllegalStateException("Not connected")

        val id = ++requestId
        val request = RpcRequest(id = id, method = method, params = params)
        val requestJson = json.encodeToString(RpcRequest.serializer(), request)

        currentSession.send(Frame.Text(requestJson))

        // Wait for response with matching ID
        while (true) {
            val response = responseChannel.receive()
            if (response.id == id) {
                if (response.error != null) {
                    throw SubstrateException("RPC error: ${response.error}")
                }
                return response.result
            }
        }
    }

    /**
     * Fetch runtime metadata from the node
     */
    suspend fun fetchMetadata() {
        try {
            val result = call("state_getMetadata", JsonArray(emptyList()))
            _metadata.value = result?.jsonPrimitive?.content
        } catch (e: Exception) {
            // Metadata fetch failed, but don't disconnect
            println("Failed to fetch metadata: ${e.message}")
        }
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

        try {
            for (frame in currentSession.incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    try {
                        val response = json.decodeFromString<RpcResponse>(text)
                        responseChannel.send(response)
                    } catch (e: Exception) {
                        println("Failed to parse response: $text")
                    }
                }
            }
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e.message ?: "Connection lost")
        }
    }

    fun close() {
        scope.cancel()
        client.close()
    }
}

class SubstrateException(message: String) : Exception(message)
