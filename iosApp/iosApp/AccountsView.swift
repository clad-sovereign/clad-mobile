import SwiftUI
import Shared

struct AccountsView: View {
    @ObservedObject var viewModel: AccountsViewModelWrapper
    @Environment(\.colorScheme) var colorScheme

    var body: some View {
        let colors = CladColors.ColorScheme.forScheme(colorScheme)

        ZStack {
            // Background - Adapts to system light/dark mode
            colors.background
                .ignoresSafeArea()

            ScrollView {
                VStack(spacing: 24) {
                    // Title
                    Text("Accounts")
                        .font(CladTypography.headlineLarge)
                        .foregroundColor(colors.onBackground)
                        .frame(maxWidth: .infinity, alignment: .leading)

                    Text("Account management coming in Phase 1B")
                        .font(CladTypography.bodyLarge)
                        .foregroundColor(colors.onSurfaceVariant)
                        .frame(maxWidth: .infinity, alignment: .leading)

                    // Connection Status Card
                    VStack(alignment: .leading, spacing: 16) {
                        HStack {
                            Text(viewModel.connectionStatusText)
                                .font(CladTypography.titleMedium)
                                .foregroundColor(colors.onSurface)

                            Spacer()

                            // Status indicator with pulse animation
                            Circle()
                                .fill(viewModel.connectionStatusColor)
                                .frame(
                                    width: viewModel.isConnected ? viewModel.pulseSize : 12,
                                    height: viewModel.isConnected ? viewModel.pulseSize : 12
                                )
                        }

                        if viewModel.isConnected {
                            VStack(alignment: .leading, spacing: 8) {
                                HStack(spacing: 4) {
                                    Text("✓ Substrate RPC connection established")
                                        .font(CladTypography.bodyMedium)
                                        .foregroundColor(colors.onSurfaceVariant)
                                }

                                HStack(spacing: 4) {
                                    Text("✓ Metadata fetched successfully")
                                        .font(CladTypography.bodyMedium)
                                        .foregroundColor(colors.onSurfaceVariant)
                                }
                            }

                            Divider()
                                .background(colors.tertiary.opacity(0.3))
                                .padding(.vertical, 8)

                            // Node Stream Section
                            VStack(alignment: .leading, spacing: 8) {
                                Text("Node Stream")
                                    .font(CladTypography.labelLarge)
                                    .foregroundColor(colors.onSurfaceVariant)

                                // Messages container with scroll and auto-scroll
                                ScrollViewReader { proxy in
                                    let messages = viewModel.recentMessages(colorScheme: colors)
                                    ScrollView {
                                        VStack(alignment: .leading, spacing: 4) {
                                            ForEach(messages, id: \.id) { message in
                                                NodeMessageRow(message: message)
                                                    .id(message.id)
                                            }

                                            if messages.isEmpty {
                                                Text("Waiting for messages...")
                                                    .font(CladTypography.caption)
                                                    .foregroundColor(colors.tertiary)
                                                    .padding(.vertical, 8)
                                            }
                                        }
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                    }
                                    .padding(12)
                                    .background(colors.background)
                                    .cornerRadius(8)
                                    .frame(height: 125)
                                    .onChange(of: viewModel.messages.count) {
                                        // Auto-scroll to bottom when new messages arrive
                                        if let lastMessage = messages.last {
                                            withAnimation {
                                                proxy.scrollTo(lastMessage.id, anchor: .bottom)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .padding(16)
                    .background(colors.surface)
                    .cornerRadius(12)
                }
                .padding(24)
            }
        }
        .onAppear {
            viewModel.startPulseAnimation()
        }
    }
}

struct NodeMessageRow: View {
    let message: NodeMessageWrapper
    @State private var opacity: Double = 0
    @State private var offsetY: CGFloat = 10

    var body: some View {
        Text(message.displayText)
            .font(CladTypography.codeSmall)
            .foregroundColor(message.color)
            .lineLimit(2)
            .opacity(opacity)
            .offset(y: offsetY)
            .onAppear {
                withAnimation(.easeOut(duration: 0.3)) {
                    opacity = 1
                    offsetY = 0
                }
            }
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
    @Published var pulseSize: CGFloat = 12.0

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
        case "connected": return CladColors.statusConnected
        case "connecting": return CladColors.statusConnecting
        case "error": return CladColors.statusError
        default: return CladColors.statusDisconnected
        }
    }

    func recentMessages(colorScheme: CladColors.ColorScheme) -> [NodeMessageWrapper] {
        Array(messages.suffix(5)).map { message in
            let (text, color) = formatMessage(message, colorScheme: colorScheme)
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
        withAnimation(
            Animation
                .easeInOut(duration: 0.8)
                .repeatForever(autoreverses: true)
        ) {
            pulseSize = 10.0
        }
    }

    private func formatMessage(_ message: SubstrateClient.NodeMessage, colorScheme: CladColors.ColorScheme) -> (String, Color) {
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
            return (text, colorScheme.messageSent)

        case .received:
            let text: String
            if content.contains("\"method\":\"chain_newHead\"") {
                // Extract block number
                if let blockNumRange = content.range(of: "\"number\":\"0x"),
                   let endRange = content[blockNumRange.upperBound...].range(of: "\"") {
                    let blockNumHex = String(content[blockNumRange.upperBound..<endRange.lowerBound])
                    if let decimal = Int64(blockNumHex, radix: 16) {
                        text = "← Imported block #\(decimal)"
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
            return (text, colorScheme.messageReceived)

        default:
            return ("Unknown message", CladColors.statusDisconnected)
        }
    }

    deinit {
        connectionStateTask?.cancel()
        messagesTask?.cancel()
    }
}
