# Development Setup

This document describes how to run Clad Signer mobile app with local Substrate nodes for development and testing.

## Prerequisites

1. **Clad Studio** node binary built and available at:
   ```
   ../clad-studio/target/release/clad-node
   ```

2. **Android emulator** or device connected and accessible via `adb`

3. **Port forwarding** configured for Android emulator:
   ```bash
   adb reverse tcp:9944 tcp:9944
   adb reverse tcp:9945 tcp:9945
   ```

## Running Substrate Nodes

The mobile app connects to local Substrate nodes via WebSocket. For development, you need to start the nodes with external RPC access enabled.

### Single Node Setup (Alice)

Start Alice node on port 9944:

```bash
cd ../clad-studio
./target/release/clad-node \
  --chain local \
  --alice \
  --tmp \
  --unsafe-force-node-key-generation \
  --port 30333 \
  --rpc-port 9944 \
  --unsafe-rpc-external \
  --rpc-cors=all
```

### Multi-Node Setup (Alice + Bob)

For testing multi-node scenarios, run both Alice and Bob nodes in separate terminals:

**Terminal 1 - Alice:**
```bash
cd ../clad-studio
./target/release/clad-node \
  --chain local \
  --alice \
  --tmp \
  --unsafe-force-node-key-generation \
  --port 30333 \
  --rpc-port 9944 \
  --unsafe-rpc-external \
  --rpc-cors=all
```

**Terminal 2 - Bob:**
```bash
cd ../clad-studio
./target/release/clad-node \
  --chain local \
  --bob \
  --tmp \
  --unsafe-force-node-key-generation \
  --port 30334 \
  --rpc-port 9945 \
  --unsafe-rpc-external \
  --rpc-cors=all
```

### Important Flags

- `--unsafe-rpc-external`: Allows RPC server to accept connections from external sources (required for Android emulator connections via adb reverse)
- `--rpc-cors=all`: Allows cross-origin requests (required for WebSocket upgrade handshake)
- `--tmp`: Uses temporary database (data is cleared on restart)
- `--rpc-port`: Specifies the WebSocket RPC port

**Security Note:** The `--unsafe-rpc-external` flag should only be used in development. Never use this on production nodes or nodes exposed to the internet.

## Running the Mobile App

### Android

```bash
./gradlew :composeApp:installDebug
```

Or run directly from Android Studio.

### iOS

Open `iosApp/iosApp.xcodeproj` in Xcode and run.

## Running Tests

The integration tests require a running Substrate node.

### Android Instrumented Tests

```bash
# Ensure Alice node is running with external RPC access
./gradlew :shared:connectedAndroidTest
```

For multi-node tests (including Bob node test), ensure both Alice and Bob nodes are running.

Test reports are generated at:
```
shared/build/reports/androidTests/connected/debug/index.html
```

## Troubleshooting

### 403 Forbidden Error

If you see "Expected HTTP 101 response but was '403 Forbidden'" error:

**Cause:** Substrate node is not configured to accept external RPC connections.

**Solution:** Restart the node with `--unsafe-rpc-external` and `--rpc-cors=all` flags as shown above.

### Connection Timeout

**Cause:** Port forwarding is not set up or node is not running.

**Solutions:**
1. Verify node is running: `lsof -i :9944`
2. Verify port forwarding: `adb reverse --list`
3. Re-establish port forwarding:
   ```bash
   adb reverse tcp:9944 tcp:9944
   adb reverse tcp:9945 tcp:9945
   ```

### Cannot Connect from Physical Device

The `10.0.2.2` address only works on Android emulator. For physical devices:

1. Find your machine's IP address:
   ```bash
   ifconfig | grep "inet " | grep -v 127.0.0.1
   ```

2. Update the endpoint in the app to use your machine's IP:
   ```kotlin
   ws://<your-ip>:9944
   ```

3. Ensure firewall allows connections on ports 9944/9945.

## Node Management

### Stop All Nodes
```bash
pkill clad-node
```

### Check Running Nodes
```bash
ps aux | grep clad-node | grep -v grep
```

### Verify Ports
```bash
lsof -i :9944 -i :9945
```
