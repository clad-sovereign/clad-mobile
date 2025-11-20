import SwiftUI
import Shared
import Combine

struct AccountsView: View {
    @ObservedObject var viewModel: AccountsViewModelWrapper

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Title
                Text("Accounts")
                    .font(.largeTitle)
                    .fontWeight(.bold)

                Text("Account management coming in PR #2")
                    .font(.body)
                    .foregroundColor(.secondary)

                // Connection Status Card
                VStack(alignment: .leading, spacing: 16) {
                    HStack {
                        Text(viewModel.connectionStatusText)
                            .font(.headline)

                        Spacer()

                        // Status indicator
                        Circle()
                            .fill(viewModel.connectionStatusColor)
                            .frame(width: 12, height: 12)
                            .scaleEffect(viewModel.isConnected ? viewModel.pulseScale : 1.0)
                    }

                    if viewModel.isConnected {
                        VStack(alignment: .leading, spacing: 4) {
                            HStack(spacing: 4) {
                                Text("✓")
                                    .foregroundColor(.green)
                                Text("Substrate RPC connection established")
                                    .font(.body)
                                    .foregroundColor(.secondary)
                            }

                            HStack(spacing: 4) {
                                Text("✓")
                                    .foregroundColor(.green)
                                Text("Metadata fetched successfully")
                                    .font(.body)
                                    .foregroundColor(.secondary)
                            }
                        }

                        Divider()
                            .padding(.vertical, 8)

                        // Node Stream Section
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Node Stream")
                                .font(.caption)
                                .foregroundColor(.secondary)

                            // Messages container
                            VStack(alignment: .leading, spacing: 4) {
                                ForEach(viewModel.recentMessages, id: \.id) { message in
                                    NodeMessageRow(message: message)
                                }

                                if viewModel.recentMessages.isEmpty {
                                    Text("Waiting for messages...")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                        .padding(.vertical, 8)
                                }
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(12)
                            .background(Color(UIColor.systemBackground))
                            .cornerRadius(8)
                            .frame(height: 125)
                        }
                    }
                }
                .padding(16)
                .background(Color(UIColor.secondarySystemBackground))
                .cornerRadius(12)
            }
            .padding(24)
        }
        .onAppear {
            viewModel.startPulseAnimation()
        }
    }
}

struct NodeMessageRow: View {
    let message: NodeMessageWrapper

    var body: some View {
        Text(message.displayText)
            .font(.system(size: 13))
            .foregroundColor(message.color)
            .lineLimit(2)
    }
}

/// Wrapper for displaying node messages
struct NodeMessageWrapper: Identifiable {
    let id: String
    let displayText: String
    let color: Color
}

/// ObservableObject wrapper for AccountsViewModelIOS
class AccountsViewModelWrapper: ObservableObject {
    private let viewModel: AccountsViewModelIOS
    private var connectionStateTask: Task<Void, Never>?
    private var messagesTask: Task<Void, Never>?

    @Published var connectionState: String = "disconnected"
    @Published var messages: [SubstrateClient.NodeMessage] = []
    @Published var pulseScale: CGFloat = 1.0

    var isConnected: Bool {
        connectionState == "connected"
    }

    var connectionStatusText: String {
        switch connectionState {
        case "connected": return "Connected to node"
        case "connecting": return "Connecting..."
        case "error": return "Connection error"
        default: return "Disconnected"
        }
    }

    var connectionStatusColor: Color {
        switch connectionState {
        case "connected": return .green
        case "connecting": return .yellow
        case "error": return .red
        default: return .gray
        }
    }

    var recentMessages: [NodeMessageWrapper] {
        Array(messages.suffix(5)).map { message in
            let (text, color) = formatMessage(message)
            return NodeMessageWrapper(
                id: message.id.description,
                displayText: text,
                color: color
            )
        }
    }

    init(viewModel: AccountsViewModelIOS) {
        self.viewModel = viewModel

        // Observe connection state using SKIE's AsyncSequence
        connectionStateTask = Task { [weak self] in
            guard let self = self else { return }
            for await state in self.viewModel.connectionState {
                await MainActor.run {
                    if state is ConnectionState.Connected {
                        self.connectionState = "connected"
                    } else if state is ConnectionState.Connecting {
                        self.connectionState = "connecting"
                    } else if state is ConnectionState.Error {
                        self.connectionState = "error"
                    } else {
                        self.connectionState = "disconnected"
                    }
                }
            }
        }

        // Observe messages using SKIE's AsyncSequence
        messagesTask = Task { [weak self] in
            guard let self = self else { return }
            for await messageList in self.viewModel.messages {
                await MainActor.run {
                    self.messages = messageList
                }
            }
        }
    }

    func startPulseAnimation() {
        withAnimation(Animation.easeInOut(duration: 0.8).repeatForever(autoreverses: true)) {
            pulseScale = 1.2
        }
    }

    private func formatMessage(_ message: SubstrateClient.NodeMessage) -> (String, Color) {
        let content = message.content

        switch message.direction {
        case .sent:
            let text: String
            if content.contains("\"method\":\"system_properties\"") {
                text = "→ Requesting chain properties"
            } else if content.contains("\"method\":\"state_getMetadata\"") {
                text = "→ Requesting chain metadata"
            } else if content.contains("\"method\":\"chain_subscribeNewHeads\"") {
                text = "→ Subscribing to new blocks"
            } else if content.contains("\"method\":\"chain_subscribeFinalizedHeads\"") {
                text = "→ Subscribing to finalized blocks"
            } else {
                text = "→ Sending request"
            }
            return (text, Color.blue)

        case .received:
            let text: String
            if content.contains("\"method\":\"chain_newHead\"") {
                // Extract block number
                if let blockNumRange = content.range(of: "\"number\":\"0x"),
                   let endRange = content[blockNumRange.upperBound...].range(of: "\"") {
                    let blockNumHex = String(content[blockNumRange.upperBound..<endRange.lowerBound])
                    if let decimal = Int64(blockNumHex, radix: 16) {
                        text = "🏆 Imported block #\(decimal)"
                    } else {
                        text = "← New block produced"
                    }
                } else {
                    text = "← New block produced"
                }
            } else if content.contains("\"method\":\"chain_finalizedHead\"") {
                if let blockNumRange = content.range(of: "\"number\":\"0x"),
                   let endRange = content[blockNumRange.upperBound...].range(of: "\"") {
                    let blockNumHex = String(content[blockNumRange.upperBound..<endRange.lowerBound])
                    if let decimal = Int64(blockNumHex, radix: 16) {
                        text = "✨ Block #\(decimal) finalized"
                    } else {
                        text = "← Block finalized"
                    }
                } else {
                    text = "← Block finalized"
                }
            } else if content.contains("\"result\":{}") {
                text = "← Chain properties received"
            } else if content.contains("\"result\":\"0x6d657461") {
                text = "📦 Metadata received"
            } else if content.contains("\"result\":\"") && content.count < 150 {
                text = "✓ Subscription active"
            } else {
                text = "← Response received"
            }
            return (text, Color.green)

        default:
            return ("Unknown message", Color.gray)
        }
    }

    deinit {
        connectionStateTask?.cancel()
        messagesTask?.cancel()
    }
}
