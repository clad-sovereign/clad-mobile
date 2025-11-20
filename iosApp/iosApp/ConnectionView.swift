import SwiftUI
import Shared
import Combine

struct ConnectionView: View {
    @ObservedObject var viewModel: ConnectionViewModelWrapper
    @Binding var isConnected: Bool
    @Environment(\.colorScheme) var colorScheme

    var body: some View {
        let colors = CladColors.ColorScheme.forScheme(colorScheme)

        ZStack {
            // Background - Adapts to system light/dark mode
            colors.background
                .ignoresSafeArea()

            VStack(spacing: 0) {
                // App Title
                VStack(spacing: 8) {
                    Text("CLAD Signer")
                        .font(CladTypography.headlineLarge)
                        .foregroundColor(colors.primary)

                    Text("Sovereign Real-World Asset Issuance")
                        .font(CladTypography.bodyMedium)
                        .foregroundColor(colors.onSurfaceVariant)
                        .multilineTextAlignment(.center)
                }

                Spacer()
                    .frame(height: 48)

                // Endpoint Input
                VStack(alignment: .leading, spacing: 8) {
                    TextField("ws://127.0.0.1:9944", text: $viewModel.endpoint)
                        .font(CladTypography.bodyLarge)
                        .padding(16)
                        .background(colors.surface)
                        .foregroundColor(colors.onSurface)
                        .cornerRadius(10)
                        .autocapitalization(.none)
                        .autocorrectionDisabled()
                        .disabled(viewModel.isLoading)
                        .onChange(of: viewModel.endpoint) { newValue in
                            viewModel.onEndpointChanged(newValue)
                        }

                    if let error = viewModel.error {
                        Text(error)
                            .font(CladTypography.caption)
                            .foregroundColor(colors.error)
                    }
                }

                Spacer()
                    .frame(height: 24)

                // Single button that shows loading state via text and disabled state
                Button(action: {
                    viewModel.connect()
                }) {
                    Text(viewModel.buttonText)
                        .font(CladTypography.bodyLarge)
                        .fontWeight(.medium)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .padding(.horizontal, 24)
                        .background(viewModel.isButtonEnabled ? colors.primary : colors.primary.opacity(0.5))
                        .foregroundColor(colors.onPrimary)
                        .clipShape(Capsule())
                }
                .disabled(!viewModel.isButtonEnabled)

                Spacer()
                    .frame(height: 32)

                // Footer
                Text("For finance ministries, debt offices and state issuers")
                    .font(CladTypography.caption)
                    .foregroundColor(colors.onSurfaceVariant)
                    .multilineTextAlignment(.center)
            }
            .frame(maxHeight: .infinity)
            .padding(.horizontal)
        }
        .onChange(of: viewModel.isConnected) { connected in
            if connected {
                isConnected = true
            }
        }
    }
}

/// ObservableObject wrapper for ConnectionViewModelIOS
class ConnectionViewModelWrapper: ObservableObject {
    private let viewModel: ConnectionViewModelIOS
    private var observationTask: Task<Void, Never>?

    @Published var endpoint: String = ""
    @Published var connectionState: String = "disconnected"
    @Published var isLoading: Bool = false
    @Published var error: String? = nil

    var isConnecting: Bool {
        connectionState == "connecting"
    }

    var isConnected: Bool {
        connectionState == "connected"
    }

    var hasError: Bool {
        error != nil
    }

    var buttonText: String {
        if connectionState == "connecting" || connectionState == "connected" {
            return "Connecting..."
        } else if hasError {
            return "Retry Connection"
        } else {
            return "Connect to Node"
        }
    }

    var isButtonEnabled: Bool {
        connectionState != "connecting" && connectionState != "connected"
    }

    init(viewModel: ConnectionViewModelIOS) {
        self.viewModel = viewModel
        self.endpoint = viewModel.uiState.value.endpoint
        self.isLoading = viewModel.uiState.value.isLoading
        self.error = viewModel.uiState.value.error

        // Observe StateFlow using SKIE's AsyncSequence conversion
        observationTask = Task { [weak self] in
            guard let self = self else { return }
            for await state in self.viewModel.uiState {
                await MainActor.run {
                    self.endpoint = state.endpoint
                    self.isLoading = state.isLoading
                    self.error = state.error

                    // Map ConnectionState to string for easier handling
                    if state.connectionState is ConnectionState.Connected {
                        self.connectionState = "connected"
                    } else if state.connectionState is ConnectionState.Connecting {
                        self.connectionState = "connecting"
                    } else if state.connectionState is ConnectionState.Error {
                        self.connectionState = "error"
                    } else {
                        self.connectionState = "disconnected"
                    }
                }
            }
        }
    }

    func onEndpointChanged(_ newEndpoint: String) {
        viewModel.onEndpointChanged(endpoint: newEndpoint)
    }

    func connect() {
        viewModel.connect()
    }

    func disconnect() {
        viewModel.disconnect()
    }

    deinit {
        observationTask?.cancel()
    }
}
