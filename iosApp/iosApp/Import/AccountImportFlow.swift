import SwiftUI

/// Main container view for the account import flow
/// Handles navigation between different import steps
struct AccountImportFlow: View {
    @State private var viewModel = AccountImportViewModel()
    @Environment(\.dismiss) var dismiss
    @Environment(\.colorScheme) var colorScheme

    var body: some View {
        NavigationStack {
            contentView
                .toolbar {
                    if showsCancelButton {
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Cancel") {
                                dismiss()
                            }
                            .foregroundColor(CladColors.ColorScheme.forScheme(colorScheme).primary)
                        }
                    }

                    if showsBackButton {
                        ToolbarItem(placement: .navigationBarLeading) {
                            Button(action: { viewModel.goBack() }) {
                                Image(systemName: "chevron.left")
                                    .foregroundColor(CladColors.ColorScheme.forScheme(colorScheme).primary)
                            }
                        }
                    }
                }
        }
    }

    @ViewBuilder
    private var contentView: some View {
        switch viewModel.flowState {
        case .selectMethod:
            ImportMethodSheet(viewModel: viewModel)

        case .seedPhraseInput:
            SeedPhraseInputView(viewModel: viewModel)

        case .qrCodeScan:
            QRCodeScannerView(viewModel: viewModel)

        case .manualAddressInput:
            ManualAddressEntryView(viewModel: viewModel)

        case .confirmImport, .importing:
            ConfirmImportView(viewModel: viewModel)

        case .success:
            ImportSuccessView(viewModel: viewModel) {
                dismiss()
            }

        case .error(let message):
            ImportErrorView(
                errorMessage: message,
                onRetry: { viewModel.goBack() },
                onDismiss: { dismiss() }
            )
        }
    }

    private var showsCancelButton: Bool {
        switch viewModel.flowState {
        case .selectMethod:
            return true
        case .success, .error:
            return false
        default:
            return false
        }
    }

    private var showsBackButton: Bool {
        switch viewModel.flowState {
        case .seedPhraseInput, .qrCodeScan, .manualAddressInput, .confirmImport:
            return true
        default:
            return false
        }
    }
}
