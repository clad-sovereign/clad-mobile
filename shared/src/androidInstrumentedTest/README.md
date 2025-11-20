# SubstrateClient Integration Tests

This directory contains comprehensive integration tests for the `SubstrateClient` that validate WebSocket connection behavior with real Substrate nodes.

## Overview

The integration tests are organized into four test suites:

1. **SubstrateClientIntegrationTest** - Connection state management and lifecycle
2. **SubstrateClientRpcTest** - RPC method calls and error handling
3. **SubstrateClientReconnectionTest** - Automatic reconnection with exponential backoff
4. **SubstrateClientConcurrencyTest** - Concurrent RPC calls and response matching

## Prerequisites

### Required: Running Substrate Nodes

These tests require at least one local Substrate node running. The tests are configured to use:

- **Primary endpoint**: `ws://localhost:9944` (alice node)
- **Secondary endpoint**: `ws://localhost:9945` (bob node - optional)

### Starting Nodes

Based on your clad-studio setup, start your nodes:

```bash
cd /path/to/clad-studio

# Start first node (alice)
./target/release/clad-node \
  --chain local \
  --alice \
  --port 30334 \
  --rpc-port 9944 \
  --tmp \
  --unsafe-force-node-key-generation

# Start second node (bob) in another terminal
./target/release/clad-node \
  --chain local \
  --bob \
  --port 30333 \
  --rpc-port 9945 \
  --tmp \
  --unsafe-force-node-key-generation
```

Or if you have them configured differently, just ensure:
- At least one node is accessible at `ws://localhost:9944`
- Nodes are fully synced and producing blocks

## Running the Tests

### All Integration Tests

Run all integration tests (requires connected Android device or emulator):

```bash
./gradlew :shared:connectedAndroidTest
```

### Specific Test Suite

Run a specific test class:

```bash
# Connection tests only
./gradlew :shared:connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=tech.wideas.clad.substrate.SubstrateClientIntegrationTest

# RPC call tests only
./gradlew :shared:connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=tech.wideas.clad.substrate.SubstrateClientRpcTest

# Reconnection tests only
./gradlew :shared:connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=tech.wideas.clad.substrate.SubstrateClientReconnectionTest

# Concurrency tests only
./gradlew :shared:connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=tech.wideas.clad.substrate.SubstrateClientConcurrencyTest
```

### Specific Test Method

Run a single test method:

```bash
./gradlew :shared:connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=tech.wideas.clad.substrate.SubstrateClientIntegrationTest#testConnectToLocalNode
```

## Test Coverage

### Connection Tests (SubstrateClientIntegrationTest)

- ✅ Connect to local node
- ✅ Connection state transitions (Disconnected → Connecting → Connected)
- ✅ Connection timeout scenarios with invalid endpoints
- ✅ Disconnect from connected node
- ✅ Idempotent connection (already connected)
- ✅ Connect to secondary node
- ✅ Metadata fetched after connection
- ✅ Metadata cleared on disconnect

### RPC Call Tests (SubstrateClientRpcTest)

**Basic RPC Methods:**
- ✅ `system_properties` - Chain properties
- ✅ `system_chain` - Chain name
- ✅ `system_name` - Node name
- ✅ `system_version` - Node version
- ✅ `state_getMetadata` - Runtime metadata
- ✅ `chain_getBlockHash` - Block hash (latest and by number)
- ✅ `chain_getHeader` - Block header
- ✅ `chain_getBlock` - Full block data
- ✅ `state_getRuntimeVersion` - Runtime version info
- ✅ `state_getStorage` - Storage queries

**Error Handling:**
- ✅ Invalid RPC method names
- ✅ Invalid method parameters
- ✅ RPC call timeouts

**Advanced:**
- ✅ Custom timeout values
- ✅ RPC calls with parameters
- ✅ Multiple sequential calls
- ✅ Calls after metadata fetch

### Reconnection Tests (SubstrateClientReconnectionTest)

**Auto-Reconnect Disabled:**
- ✅ No reconnection when disabled

**Auto-Reconnect Enabled:**
- ✅ Automatic reconnection on connection failure
- ✅ Max reconnect attempts limit enforced
- ✅ Exponential backoff delay (1s, 2s, 4s, 8s, 16s)
- ✅ Reconnect counter resets on successful connection
- ✅ Connection recovery when node becomes available
- ✅ Cancel reconnection on explicit disconnect

**Edge Cases:**
- ✅ Zero max reconnect attempts
- ✅ High max reconnect attempts
- ✅ Reconnection with valid endpoint

### Concurrent Operations Tests (SubstrateClientConcurrencyTest)

**Concurrent RPC Calls:**
- ✅ Two concurrent calls
- ✅ Multiple (5) concurrent calls
- ✅ Ten concurrent calls (buffer stress test)
- ✅ Concurrent calls with different parameters
- ✅ Fifty concurrent calls (channel buffer limit: 64)

**Response Matching:**
- ✅ Correct response routing to requests
- ✅ Mixed success and failure scenarios

**Channel Buffer Behavior:**
- ✅ Buffer handling under load
- ✅ High-frequency sequential calls

**Stress Tests:**
- ✅ Multiple coroutines with multiple calls each
- ✅ Interleaved sequential and concurrent calls
- ✅ Concurrent calls with individual timeouts
- ✅ Concurrent calls with one timeout
- ✅ Concurrent calls with large responses

## Test Execution Environment

### Android Devices/Emulators

These are **Android instrumented tests** that run on:
- Physical Android devices (API 29+)
- Android emulators (API 29+)

The device/emulator must be able to access `localhost:9944` and `localhost:9945` on your development machine.

### Network Considerations

**For Emulators:**
- `localhost` from the emulator maps to `10.0.2.2` on the host machine
- However, the tests use `ws://localhost:9944` which works because the emulator has special handling for localhost

**For Physical Devices:**
- You may need to modify the test endpoints to use your machine's IP address instead of `localhost`
- Alternatively, use ADB reverse port forwarding:

```bash
adb reverse tcp:9944 tcp:9944
adb reverse tcp:9945 tcp:9945
```

This makes the device's `localhost:9944` forward to your machine's `localhost:9944`.

## Troubleshooting

### Tests Fail with Connection Errors

**Problem**: Tests fail with "Connection failed" or timeout errors

**Solutions**:
1. Verify nodes are running: `curl -H "Content-Type: application/json" -d '{"id":1, "jsonrpc":"2.0", "method": "system_chain"}' http://localhost:9944`
2. Check WebSocket endpoint: Use a WebSocket client to test `ws://localhost:9944`
3. For physical devices, set up ADB port forwarding (see above)
4. Check firewall settings

### Tests Hang or Timeout

**Problem**: Tests start but never complete

**Solutions**:
1. Increase test timeout in individual tests
2. Verify node is producing blocks (not stalled)
3. Check for network connectivity issues
4. Ensure device/emulator has sufficient resources

### Some Tests Pass, Some Fail

**Problem**: Inconsistent test results

**Solutions**:
1. Check node stability - restart nodes if they've been running a long time
2. Verify both nodes are running if secondary endpoint tests fail
3. Look for resource constraints on the device/emulator
4. Check for interference from other running tests

### Channel Buffer Overflow

**Problem**: Tests with many concurrent calls fail

**Solutions**:
1. This shouldn't happen with the current 64-item buffer
2. If it does, there may be a bug in response matching
3. Check SubstrateClient.kt line 76 for buffer size configuration

## CI Integration

To add these tests to CI:

1. Set up Substrate node in CI environment
2. Start node before running tests
3. Run tests with proper timeout:

```yaml
- name: Start Substrate Node
  run: |
    ./target/release/clad-node --dev --tmp --rpc-port 9944 &
    sleep 10 # Wait for node to start

- name: Run Integration Tests
  run: ./gradlew :shared:connectedAndroidTest
```

## Test Performance

Typical execution times on a modern development machine:

- **SubstrateClientIntegrationTest**: ~2-3 minutes
- **SubstrateClientRpcTest**: ~2-3 minutes
- **SubstrateClientReconnectionTest**: ~3-4 minutes (includes backoff delays)
- **SubstrateClientConcurrencyTest**: ~2-3 minutes

**Total**: ~10-15 minutes for full suite

## Contributing

When adding new integration tests:

1. **Group by functionality** - Keep related tests in the same file
2. **Use descriptive names** - Test names should explain what they verify
3. **Set appropriate timeouts** - Default is 30s, increase for slow operations
4. **Clean up resources** - Always disconnect and close client in `@After`
5. **Handle test isolation** - Each test should be independent
6. **Document prerequisites** - Note if test requires specific node configuration

## Related Documentation

- [CLAUDE.md](/CLAUDE.md) - Project overview and build commands
- [SubstrateClient.kt](../commonMain/kotlin/tech/wideas/clad/substrate/SubstrateClient.kt) - Implementation
- [SubstrateClientTest.kt](../commonTest/kotlin/tech/wideas/clad/substrate/SubstrateClientTest.kt) - Unit tests
- [Issue #5](https://github.com/clad-sovereign/clad-mobile/issues/5) - Original issue
