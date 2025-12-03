import SwiftUI

/// View for confirming account import details before saving
struct ConfirmImportView: View {
    var viewModel: AccountImportViewModel
    @Environment(\.colorScheme) var colorScheme
    @FocusState private var isLabelFocused: Bool

    var body: some View {
        let colors = CladColors.ColorScheme.forScheme(colorScheme)

        ZStack {
            colors.background
                .ignoresSafeArea()

            ScrollView {
                VStack(spacing: 24) {
                    // Header
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Confirm Import")
                            .font(CladTypography.headlineMedium)
                            .foregroundColor(colors.onBackground)

                        Text(viewModel.importData.isWatchOnly ?
                             "Review the address details before adding this watch-only account." :
                             "Review the derived address and optionally add a label.")
                            .font(CladTypography.bodyMedium)
                            .foregroundColor(colors.onSurfaceVariant)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)

                    // Account type badge
                    HStack {
                        Image(systemName: viewModel.importData.isWatchOnly ? "eye.fill" : "key.fill")
                            .foregroundColor(viewModel.importData.isWatchOnly ? colors.tertiary : colors.primary)

                        Text(viewModel.importData.isWatchOnly ? "Watch-Only Account" : "Full Access Account")
                            .font(CladTypography.labelLarge)
                            .foregroundColor(viewModel.importData.isWatchOnly ? colors.tertiary : colors.primary)

                        Spacer()
                    }
                    .padding(16)
                    .background((viewModel.importData.isWatchOnly ? colors.tertiary : colors.primary).opacity(0.1))
                    .cornerRadius(12)

                    // Address card
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Address")
                            .font(CladTypography.labelLarge)
                            .foregroundColor(colors.onSurfaceVariant)

                        Text(viewModel.derivedAddress)
                            .font(CladTypography.bodyMedium.monospaced())
                            .foregroundColor(colors.onSurface)
                            .lineLimit(nil)
                            .fixedSize(horizontal: false, vertical: true)

                        // Copy button
                        Button(action: copyAddress) {
                            HStack {
                                Image(systemName: "doc.on.doc")
                                Text("Copy Address")
                            }
                            .font(CladTypography.bodyMedium)
                            .foregroundColor(colors.primary)
                        }
                    }
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(colors.surface)
                    .cornerRadius(12)

                    // Key type (only for full access accounts)
                    if !viewModel.importData.isWatchOnly {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Key Type")
                                .font(CladTypography.labelLarge)
                                .foregroundColor(colors.onSurfaceVariant)

                            Text(viewModel.importData.keyType == .sr25519 ? "SR25519 (Schnorrkel)" : "ED25519")
                                .font(CladTypography.bodyMedium)
                                .foregroundColor(colors.onSurface)
                        }
                        .padding(16)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(colors.surface)
                        .cornerRadius(12)
                    }

                    // Account label input
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Account Label (Optional)")
                            .font(CladTypography.labelLarge)
                            .foregroundColor(colors.onSurfaceVariant)

                        TextField("My Account", text: Binding(
                            get: { viewModel.accountLabel },
                            set: { viewModel.accountLabel = $0 }
                        ))
                        .font(CladTypography.bodyMedium)
                        .foregroundColor(colors.onSurface)
                        .focused($isLabelFocused)
                        .padding(16)
                        .background(colors.surface)
                        .cornerRadius(12)
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(isLabelFocused ? colors.primary : Color.clear, lineWidth: 2)
                        )
                        .submitLabel(.done)

                        Text("A friendly name to identify this account")
                            .font(CladTypography.caption)
                            .foregroundColor(colors.onSurfaceVariant)
                    }

                    // Biometric notice for full access accounts
                    if !viewModel.importData.isWatchOnly {
                        HStack(spacing: 12) {
                            Image(systemName: "faceid")
                                .foregroundColor(colors.primary)
                                .font(.title2)

                            VStack(alignment: .leading, spacing: 4) {
                                Text("Biometric Protection")
                                    .font(CladTypography.labelLarge)
                                    .foregroundColor(colors.onSurface)

                                Text("Your recovery key will be protected with Face ID/Touch ID")
                                    .font(CladTypography.caption)
                                    .foregroundColor(colors.onSurfaceVariant)
                            }

                            Spacer()
                        }
                        .padding(16)
                        .background(colors.primary.opacity(0.1))
                        .cornerRadius(12)
                    }

                    Spacer(minLength: 80)
                }
                .padding(24)
            }

            // Import button
            VStack {
                Spacer()

                Button(action: {
                    isLabelFocused = false
                    Task {
                        await viewModel.confirmImport()
                    }
                }) {
                    HStack {
                        if viewModel.flowState == .importing {
                            ProgressView()
                                .tint(colors.onPrimary)
                        } else {
                            Image(systemName: viewModel.importData.isWatchOnly ? "eye.fill" : "checkmark.shield.fill")
                            Text(viewModel.importData.isWatchOnly ? "Add Watch-Only Account" : "Import Account")
                        }
                    }
                    .font(CladTypography.bodyLarge)
                    .fontWeight(.semibold)
                    .frame(maxWidth: .infinity)
                    .padding(16)
                    .background(colors.primary)
                    .foregroundColor(colors.onPrimary)
                    .cornerRadius(12)
                }
                .disabled(viewModel.flowState == .importing)
                .padding(.horizontal, 24)
                .padding(.bottom, 24)
                .background(
                    LinearGradient(
                        colors: [colors.background.opacity(0), colors.background],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    .frame(height: 100)
                    .allowsHitTesting(false)
                )
            }
        }
        .navigationTitle("Confirm")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func copyAddress() {
        UIPasteboard.general.string = viewModel.derivedAddress

        // Simple feedback - in production you might want a toast/snackbar
        let generator = UINotificationFeedbackGenerator()
        generator.notificationOccurred(.success)
    }
}

/// View shown when import succeeds
struct ImportSuccessView: View {
    var viewModel: AccountImportViewModel
    let onDismiss: () -> Void
    @Environment(\.colorScheme) var colorScheme

    var body: some View {
        let colors = CladColors.ColorScheme.forScheme(colorScheme)

        ZStack {
            colors.background
                .ignoresSafeArea()

            VStack(spacing: 32) {
                Spacer()

                // Success icon
                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 80))
                    .foregroundColor(CladColors.statusConnected)

                VStack(spacing: 12) {
                    Text("Account Imported")
                        .font(CladTypography.headlineMedium)
                        .foregroundColor(colors.onBackground)

                    Text(viewModel.importData.isWatchOnly ?
                         "Your watch-only account has been added successfully." :
                         "Your account has been securely imported and protected with biometrics.")
                        .font(CladTypography.bodyMedium)
                        .foregroundColor(colors.onSurfaceVariant)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 24)
                }

                // Address preview
                VStack(spacing: 8) {
                    Text("Address")
                        .font(CladTypography.caption)
                        .foregroundColor(colors.onSurfaceVariant)

                    Text(formatAddress(viewModel.derivedAddress))
                        .font(CladTypography.bodyMedium.monospaced())
                        .foregroundColor(colors.onSurface)
                }
                .padding(16)
                .background(colors.surface)
                .cornerRadius(12)

                Spacer()

                Button(action: onDismiss) {
                    Text("Done")
                        .font(CladTypography.bodyLarge)
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity)
                        .padding(16)
                        .background(colors.primary)
                        .foregroundColor(colors.onPrimary)
                        .cornerRadius(12)
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 24)
            }
        }
        .navigationBarBackButtonHidden(true)
    }

    private func formatAddress(_ address: String) -> String {
        guard address.count > 16 else { return address }
        let prefix = String(address.prefix(8))
        let suffix = String(address.suffix(8))
        return "\(prefix)...\(suffix)"
    }
}

/// View shown when import fails
struct ImportErrorView: View {
    let errorMessage: String
    let onRetry: () -> Void
    let onDismiss: () -> Void
    @Environment(\.colorScheme) var colorScheme

    var body: some View {
        let colors = CladColors.ColorScheme.forScheme(colorScheme)

        ZStack {
            colors.background
                .ignoresSafeArea()

            VStack(spacing: 32) {
                Spacer()

                // Error icon
                Image(systemName: "exclamationmark.triangle.fill")
                    .font(.system(size: 80))
                    .foregroundColor(colors.error)

                VStack(spacing: 12) {
                    Text("Import Failed")
                        .font(CladTypography.headlineMedium)
                        .foregroundColor(colors.onBackground)

                    Text(errorMessage)
                        .font(CladTypography.bodyMedium)
                        .foregroundColor(colors.onSurfaceVariant)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 24)
                }

                Spacer()

                VStack(spacing: 12) {
                    Button(action: onRetry) {
                        Text("Try Again")
                            .font(CladTypography.bodyLarge)
                            .fontWeight(.semibold)
                            .frame(maxWidth: .infinity)
                            .padding(16)
                            .background(colors.primary)
                            .foregroundColor(colors.onPrimary)
                            .cornerRadius(12)
                    }

                    Button(action: onDismiss) {
                        Text("Cancel")
                            .font(CladTypography.bodyLarge)
                            .foregroundColor(colors.onSurfaceVariant)
                            .frame(maxWidth: .infinity)
                            .padding(16)
                    }
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 24)
            }
        }
        .navigationBarBackButtonHidden(true)
    }
}
