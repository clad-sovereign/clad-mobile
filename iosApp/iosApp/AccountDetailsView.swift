import SwiftUI
import Shared

/// Account details screen showing full account information
struct AccountDetailsView: View {
    let account: AccountInfo
    let isActive: Bool
    let onSetActive: () -> Void
    let onDelete: () -> Void
    let onUpdateLabel: (String) -> Void
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) var colorScheme

    @State private var showDeleteConfirmation = false
    @State private var showEditLabelSheet = false
    @State private var editedLabel: String = ""
    @State private var showCopiedToast = false

    var body: some View {
        let colors = CladColors.ColorScheme.forScheme(colorScheme)

        NavigationStack {
            ScrollView(.vertical) {
                VStack(spacing: 24) {
                    // Account Header
                    AccountHeaderSection(
                        account: account,
                        isActive: isActive,
                        colors: colors,
                        onSetActive: onSetActive
                    )

                    Divider()

                    // Address Section
                    AddressSection(
                        address: account.address,
                        colors: colors,
                        onCopy: copyAddress
                    )

                    Divider()

                    // Account Info Section
                    AccountInfoSectionView(account: account, colors: colors)
                }
                .padding(24)
            }
            .background(colors.background)
            .navigationTitle("Account Details")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Back") {
                        dismiss()
                    }
                }

                ToolbarItem(placement: .navigationBarTrailing) {
                    Menu {
                        Button {
                            editedLabel = account.label
                            showEditLabelSheet = true
                        } label: {
                            Label("Rename", systemImage: "pencil")
                        }

                        Button(role: .destructive) {
                            showDeleteConfirmation = true
                        } label: {
                            Label("Delete Account", systemImage: "trash")
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                }
            }
        }
        .alert("Delete Account?", isPresented: $showDeleteConfirmation) {
            Button("Cancel", role: .cancel) {}
            Button("Delete", role: .destructive) {
                onDelete()
                dismiss()
            }
        } message: {
            Text("Are you sure you want to delete \"\(account.label)\"? This action cannot be undone and will remove the keypair from secure storage.")
        }
        .sheet(isPresented: $showEditLabelSheet) {
            EditLabelSheet(
                label: $editedLabel,
                onSave: {
                    if !editedLabel.trimmingCharacters(in: .whitespaces).isEmpty {
                        onUpdateLabel(editedLabel.trimmingCharacters(in: .whitespaces))
                        showEditLabelSheet = false
                    }
                },
                onCancel: {
                    showEditLabelSheet = false
                }
            )
        }
        .overlay {
            if showCopiedToast {
                VStack {
                    Spacer()
                    Text("Address copied!")
                        .font(CladTypography.bodyMedium)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .background(colors.surfaceVariant)
                        .foregroundColor(colors.onSurfaceVariant)
                        .cornerRadius(8)
                        .padding(.bottom, 40)
                }
                .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
    }

    private func copyAddress() {
        UIPasteboard.general.string = account.address
        let generator = UINotificationFeedbackGenerator()
        generator.notificationOccurred(.success)

        withAnimation {
            showCopiedToast = true
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            withAnimation {
                showCopiedToast = false
            }
        }
    }
}

// MARK: - Subviews

private struct AccountHeaderSection: View {
    let account: AccountInfo
    let isActive: Bool
    let colors: CladColors.ColorScheme
    let onSetActive: () -> Void

    var body: some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 8) {
                Text(account.label)
                    .font(CladTypography.headlineSmall)
                    .foregroundColor(colors.onBackground)

                // Active badge
                if isActive {
                    HStack(spacing: 4) {
                        Image(systemName: "checkmark")
                            .font(.caption2)
                        Text("Active")
                            .font(CladTypography.caption)
                    }
                    .foregroundColor(colors.onPrimary)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(colors.primary)
                    .cornerRadius(4)
                }
            }

            Spacer()

            if !isActive {
                Button("Set Active") {
                    onSetActive()
                }
                .buttonStyle(.borderedProminent)
            }
        }
    }
}

private struct AddressSection: View {
    let address: String
    let colors: CladColors.ColorScheme
    let onCopy: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Address")
                .font(CladTypography.titleMedium)
                .foregroundColor(colors.onBackground)

            VStack(alignment: .leading, spacing: 12) {
                Text(address)
                    .font(CladTypography.bodyMedium.monospaced())
                    .foregroundColor(colors.onSurfaceVariant)
                    .textSelection(.enabled)

                HStack {
                    Spacer()
                    Button {
                        onCopy()
                    } label: {
                        HStack {
                            Image(systemName: "doc.on.doc")
                            Text("Copy Address")
                        }
                    }
                    .buttonStyle(.bordered)
                }
            }
            .padding(16)
            .background(colors.surfaceVariant)
            .cornerRadius(12)
        }
    }
}

private struct AccountInfoSectionView: View {
    let account: AccountInfo
    let colors: CladColors.ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Information")
                .font(CladTypography.titleMedium)
                .foregroundColor(colors.onBackground)

            VStack(spacing: 16) {
                InfoRow(label: "Created", value: formatTimestamp(account.createdAt), colors: colors)

                if let lastUsed = account.lastUsedAt {
                    InfoRow(label: "Last Used", value: formatTimestamp(lastUsed.int64Value), colors: colors)
                } else {
                    InfoRow(label: "Last Used", value: "Never", colors: colors)
                }

                InfoRow(label: "Account ID", value: String(account.id.prefix(8)) + "...", colors: colors)
            }
            .padding(16)
            .background(colors.surface)
            .cornerRadius(12)
        }
    }

    private func formatTimestamp(_ epochMillis: Int64) -> String {
        let date = Date(timeIntervalSince1970: Double(epochMillis) / 1000.0)
        let formatter = RelativeDateTimeFormatter()
        formatter.unitsStyle = .abbreviated
        return formatter.localizedString(for: date, relativeTo: Date())
    }
}

private struct InfoRow: View {
    let label: String
    let value: String
    let colors: CladColors.ColorScheme

    var body: some View {
        HStack {
            Text(label)
                .font(CladTypography.bodyMedium)
                .foregroundColor(colors.onSurfaceVariant)
            Spacer()
            Text(value)
                .font(CladTypography.bodyMedium)
                .fontWeight(.medium)
                .foregroundColor(colors.onSurface)
        }
    }
}

private struct EditLabelSheet: View {
    @Binding var label: String
    let onSave: () -> Void
    let onCancel: () -> Void
    @Environment(\.colorScheme) var colorScheme

    var body: some View {
        let colors = CladColors.ColorScheme.forScheme(colorScheme)

        NavigationStack {
            Form {
                Section {
                    TextField("Account Name", text: $label)
                }
            }
            .navigationTitle("Rename Account")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        onCancel()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        onSave()
                    }
                    .disabled(label.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            }
        }
        .presentationDetents([.medium])
    }
}
