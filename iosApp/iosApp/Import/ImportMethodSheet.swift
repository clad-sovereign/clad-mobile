import SwiftUI

/// Sheet for selecting account import method
struct ImportMethodSheet: View {
    var viewModel: AccountImportViewModel
    @Environment(\.colorScheme) var colorScheme
    @Environment(\.dismiss) var dismiss

    var body: some View {
        let colors = CladColors.ColorScheme.forScheme(colorScheme)

        NavigationStack {
            ZStack {
                colors.background
                    .ignoresSafeArea()

                VStack(spacing: 24) {
                    Text("How would you like to import?")
                        .font(CladTypography.titleMedium)
                        .foregroundColor(colors.onBackground)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.top, 8)

                    VStack(spacing: 16) {
                        ForEach([ImportMethod.seedPhrase, .qrCode, .manualAddress]) { method in
                            ImportMethodButton(
                                method: method,
                                colors: colors,
                                action: {
                                    viewModel.selectImportMethod(method)
                                }
                            )
                        }
                    }

                    Spacer()
                }
                .padding(24)
            }
            .navigationTitle("Import Account")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        dismiss()
                    }
                    .foregroundColor(colors.primary)
                }
            }
        }
    }
}

/// Button for selecting an import method
struct ImportMethodButton: View {
    let method: ImportMethod
    let colors: CladColors.ColorScheme
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 16) {
                Image(systemName: method.icon)
                    .font(.title2)
                    .foregroundColor(colors.primary)
                    .frame(width: 40)

                VStack(alignment: .leading, spacing: 4) {
                    Text(method.title)
                        .font(CladTypography.titleMedium)
                        .foregroundColor(colors.onSurface)

                    Text(method.description)
                        .font(CladTypography.bodyMedium)
                        .foregroundColor(colors.onSurfaceVariant)
                        .multilineTextAlignment(.leading)
                }

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.body)
                    .foregroundColor(colors.onSurfaceVariant)
            }
            .padding(16)
            .background(colors.surface)
            .cornerRadius(12)
        }
        .buttonStyle(.plain)
    }
}
