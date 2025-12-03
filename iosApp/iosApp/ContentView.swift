import SwiftUI
import Shared

struct ContentView: View {
    @State private var isConnected = false

    // ViewModels
    @StateObject private var connectionViewModel = ConnectionViewModelWrapper(
        viewModel: ViewModelHelper().getConnectionViewModel()
    )
    @StateObject private var accountsViewModel = AccountsViewModelWrapper(
        viewModel: ViewModelHelper().getAccountsViewModel()
    )

    var body: some View {
        // Main app navigation (no biometric auth for Milestone #1)
        if isConnected {
            AccountsView(connectionViewModel: accountsViewModel)
        } else {
            ConnectionView(
                viewModel: connectionViewModel,
                isConnected: $isConnected
            )
        }
    }
}

struct BiometricAuthView: View {
    @Binding var isAuthenticated: Bool
    @Binding var authenticationError: String?
    @State private var isAuthenticating = false
    @Environment(\.colorScheme) var colorScheme

    private let biometricAuth = BiometricAuth_iosKt.createBiometricAuth()

    var body: some View {
        let colors = CladColors.ColorScheme.forScheme(colorScheme)

        ZStack {
            // Background - Adapts to system light/dark mode
            colors.background
                .ignoresSafeArea()

            VStack(spacing: 24) {
                Spacer()

                Image(systemName: "faceid")
                    .font(.system(size: 80))
                    .foregroundColor(colors.primary)

                Text("CLAD Signer")
                    .font(CladTypography.headlineLarge)
                    .foregroundColor(colors.primary)

                Text("Authenticate to continue")
                    .font(CladTypography.bodyLarge)
                    .foregroundColor(colors.tertiary)

                if let error = authenticationError {
                    Text(error)
                        .font(CladTypography.caption)
                        .foregroundColor(colors.error)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 24)
                }

                if isAuthenticating {
                    ProgressView()
                        .tint(colors.primary)
                } else {
                    Button(action: authenticate) {
                        Text("Authenticate")
                            .font(CladTypography.bodyLarge)
                            .fontWeight(.medium)
                            .frame(maxWidth: 200)
                            .padding(16)
                            .background(colors.primary)
                            .foregroundColor(colors.onPrimary)
                            .cornerRadius(10)
                    }
                }

                Spacer()
            }
        }
        .onAppear {
            // Automatically trigger biometric authentication on appear
            authenticate()
        }
    }

    private func authenticate() {
        isAuthenticating = true
        authenticationError = nil

        Task {
            do {
                let result = try await biometricAuth.authenticate(
                    title: "CLAD Signer",
                    subtitle: "Authenticate to access your wallet",
                    description: ""
                )

                await MainActor.run {
                    isAuthenticating = false

                    if result is BiometricResult.Success {
                        isAuthenticated = true
                    } else if result is BiometricResult.Cancelled {
                        authenticationError = "Authentication cancelled"
                    } else if result is BiometricResult.NotAvailable {
                        authenticationError = "Biometric authentication not available"
                    } else if let error = result as? BiometricResult.Error {
                        authenticationError = error.message
                    }
                }
            } catch {
                await MainActor.run {
                    isAuthenticating = false
                    authenticationError = "Authentication failed: \(error.localizedDescription)"
                }
            }
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
