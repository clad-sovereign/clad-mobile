import SwiftUI
import Shared
import Combine

struct ConnectionView: View {
    @ObservedObject var viewModel: ConnectionViewModelWrapper
    @Binding var isConnected: Bool

    var body: some View {
        VStack(spacing: 24) {
            Spacer()

            // App Title
            VStack(spacing: 8) {
                Text("CLAD Signer")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .foregroundColor(.accentColor)

                Text("Sovereign Real-World Asset Issuance")
                    .font(.body)
                    .foregroundColor(.secondary)
            }

            Spacer()

            // Endpoint Input
            VStack(alignment: .leading, spacing: 8) {
                TextField("ws://127.0.0.1:9944", text: $viewModel.endpoint)
                    .textFieldStyle(.roundedBorder)
                    .autocapitalization(.none)
                    .disabled(viewModel.isLoading)
                    .onChange(of: viewModel.endpoint) { newValue in
                        viewModel.onEndpointChanged(newValue)
                    }

                if let error = viewModel.error {
                    Text(error)
                        .font(.caption)
                        .foregroundColor(.red)
                }
            }
            .padding(.horizontal, 24)

            // Connection Button / Status
            if viewModel.isConnecting {
                VStack(spacing: 16) {
                    ProgressView()
                    Text("Connecting...")
                        .font(.body)
                }
            } else if viewModel.isConnected {
                ProgressView()
            } else {
                Button(action: {
                    viewModel.connect()
                }) {
                    Text(viewModel.hasError ? "Retry Connection" : "Connect to Node")
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.accentColor)
                        .foregroundColor(.white)
                        .cornerRadius(10)
                }
                .padding(.horizontal, 24)
                .disabled(viewModel.isLoading)
            }

            Spacer()

            // Footer
            Text("For finance ministries, debt offices and state issuers")
                .font(.caption)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)
        }
        .padding(.vertical, 24)
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
