import SwiftUI

/// View for manually entering an SS58 address (watch-only import)
struct ManualAddressEntryView: View {
    var viewModel: AccountImportViewModel
    @Environment(\.colorScheme) var colorScheme
    @FocusState private var isAddressFocused: Bool

    var body: some View {
        let colors = CladColors.ColorScheme.forScheme(colorScheme)

        ZStack {
            colors.background
                .ignoresSafeArea()

            ScrollView {
                VStack(spacing: 24) {
                    // Header
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Enter Address")
                            .font(CladTypography.headlineMedium)
                            .foregroundColor(colors.onBackground)

                        Text("Enter an SS58 address to add as a watch-only account. You will not be able to sign transactions with this account.")
                            .font(CladTypography.bodyMedium)
                            .foregroundColor(colors.onSurfaceVariant)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)

                    // Watch-only info banner
                    HStack(spacing: 12) {
                        Image(systemName: "eye.fill")
                            .foregroundColor(colors.tertiary)
                            .font(.title3)

                        VStack(alignment: .leading, spacing: 4) {
                            Text("Watch-Only Account")
                                .font(CladTypography.labelLarge)
                                .foregroundColor(colors.onSurface)

                            Text("View balances and activity without signing capability")
                                .font(CladTypography.caption)
                                .foregroundColor(colors.onSurfaceVariant)
                        }

                        Spacer()
                    }
                    .padding(16)
                    .background(colors.tertiary.opacity(0.1))
                    .cornerRadius(12)

                    // Address input
                    VStack(alignment: .leading, spacing: 8) {
                        Text("SS58 Address")
                            .font(CladTypography.labelLarge)
                            .foregroundColor(colors.onSurfaceVariant)

                        TextField("5...", text: Binding(
                            get: { viewModel.manualAddress },
                            set: { newValue in
                                viewModel.manualAddress = newValue
                                viewModel.validationError = nil
                            }
                        ))
                        .font(CladTypography.bodyMedium.monospaced())
                        .foregroundColor(colors.onSurface)
                        .autocapitalization(.none)
                        .autocorrectionDisabled()
                        .focused($isAddressFocused)
                        .padding(16)
                        .background(colors.surface)
                        .cornerRadius(12)
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(
                                    viewModel.validationError != nil ? colors.error :
                                        (isAddressFocused ? colors.primary : Color.clear),
                                    lineWidth: 2
                                )
                        )
                        .submitLabel(.done)
                        .onSubmit {
                            viewModel.validateManualAddress()
                        }

                        // Character count / hint
                        HStack {
                            if let error = viewModel.validationError {
                                Image(systemName: "exclamationmark.circle.fill")
                                    .foregroundColor(colors.error)
                                Text(error)
                                    .font(CladTypography.caption)
                                    .foregroundColor(colors.error)
                            } else {
                                Text("Substrate addresses typically start with '5' and are 47-48 characters")
                                    .font(CladTypography.caption)
                                    .foregroundColor(colors.onSurfaceVariant)
                            }

                            Spacer()

                            Text("\(viewModel.manualAddress.count)")
                                .font(CladTypography.caption)
                                .foregroundColor(colors.onSurfaceVariant)
                        }
                    }

                    // Paste from clipboard button
                    Button(action: pasteFromClipboard) {
                        HStack {
                            Image(systemName: "doc.on.clipboard")
                            Text("Paste from Clipboard")
                        }
                        .font(CladTypography.bodyMedium)
                        .foregroundColor(colors.primary)
                        .frame(maxWidth: .infinity)
                        .padding(16)
                        .background(colors.primary.opacity(0.1))
                        .cornerRadius(12)
                    }

                    Spacer(minLength: 80)
                }
                .padding(24)
            }

            // Continue button
            VStack {
                Spacer()

                Button(action: {
                    isAddressFocused = false
                    viewModel.proceedFromManualAddress()
                }) {
                    Text("Continue")
                        .font(CladTypography.bodyLarge)
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity)
                        .padding(16)
                        .background(viewModel.canProceedFromManualAddress ? colors.primary : colors.onSurfaceVariant.opacity(0.3))
                        .foregroundColor(colors.onPrimary)
                        .cornerRadius(12)
                }
                .disabled(!viewModel.canProceedFromManualAddress)
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
        .navigationTitle("Manual Entry")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            isAddressFocused = true
        }
    }

    private func pasteFromClipboard() {
        if let string = UIPasteboard.general.string {
            viewModel.manualAddress = string.trimmingCharacters(in: .whitespacesAndNewlines)
            viewModel.validateManualAddress()
        }
    }
}
