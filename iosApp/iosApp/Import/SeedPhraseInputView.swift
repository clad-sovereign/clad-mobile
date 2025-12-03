import SwiftUI

/// View for entering a 12 or 24 word seed phrase
struct SeedPhraseInputView: View {
    var viewModel: AccountImportViewModel
    @Environment(\.colorScheme) var colorScheme
    @FocusState private var focusedIndex: Int?

    var body: some View {
        let colors = CladColors.ColorScheme.forScheme(colorScheme)

        ZStack {
            colors.background
                .ignoresSafeArea()

            ScrollView {
                VStack(spacing: 24) {
                    // Header
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Enter Recovery Phrase")
                            .font(CladTypography.headlineMedium)
                            .foregroundColor(colors.onBackground)

                        Text("Enter your \(viewModel.wordCount)-word recovery phrase in the correct order.")
                            .font(CladTypography.bodyMedium)
                            .foregroundColor(colors.onSurfaceVariant)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)

                    // Word count selector
                    WordCountSelector(
                        selectedCount: viewModel.wordCount,
                        colors: colors,
                        onSelect: { count in
                            viewModel.setWordCount(count)
                        }
                    )

                    // Word grid
                    WordGrid(
                        words: viewModel.seedWords,
                        colors: colors,
                        focusedIndex: $focusedIndex,
                        onWordChange: { index, word in
                            viewModel.updateWord(at: index, with: word)
                        },
                        onPhrasePasted: { words in
                            viewModel.pasteFullPhrase(words)
                        }
                    )

                    // Validation error
                    if let error = viewModel.validationError {
                        HStack(spacing: 8) {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .foregroundColor(colors.error)
                            Text(error)
                                .font(CladTypography.bodyMedium)
                                .foregroundColor(colors.error)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(12)
                        .background(colors.error.opacity(0.1))
                        .cornerRadius(8)
                    }

                    Spacer(minLength: 80)
                }
                .padding(24)
            }

            // Continue button
            VStack {
                Spacer()

                Button(action: {
                    focusedIndex = nil
                    viewModel.validateAndProceedFromSeedPhrase()
                }) {
                    HStack {
                        if viewModel.isValidating {
                            ProgressView()
                                .tint(colors.onPrimary)
                        } else {
                            Text("Continue")
                                .font(CladTypography.bodyLarge)
                                .fontWeight(.semibold)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding(16)
                    .background(viewModel.canProceedFromSeedPhrase ? colors.primary : colors.onSurfaceVariant.opacity(0.3))
                    .foregroundColor(colors.onPrimary)
                    .cornerRadius(12)
                }
                .disabled(!viewModel.canProceedFromSeedPhrase || viewModel.isValidating)
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
        .navigationTitle("Recovery Phrase")
        .navigationBarTitleDisplayMode(.inline)
    }
}

/// Selector for choosing between 12 and 24 word mnemonics
struct WordCountSelector: View {
    let selectedCount: Int
    let colors: CladColors.ColorScheme
    let onSelect: (Int) -> Void

    var body: some View {
        HStack(spacing: 12) {
            WordCountButton(
                count: 12,
                isSelected: selectedCount == 12,
                colors: colors,
                action: { onSelect(12) }
            )

            WordCountButton(
                count: 24,
                isSelected: selectedCount == 24,
                colors: colors,
                action: { onSelect(24) }
            )
        }
    }
}

struct WordCountButton: View {
    let count: Int
    let isSelected: Bool
    let colors: CladColors.ColorScheme
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text("\(count) words")
                .font(CladTypography.bodyMedium)
                .fontWeight(isSelected ? .semibold : .regular)
                .foregroundColor(isSelected ? colors.onPrimary : colors.onSurface)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background(isSelected ? colors.primary : colors.surface)
                .cornerRadius(8)
        }
        .buttonStyle(.plain)
    }
}

/// Grid of word input fields
struct WordGrid: View {
    let words: [String]
    let colors: CladColors.ColorScheme
    var focusedIndex: FocusState<Int?>.Binding
    let onWordChange: (Int, String) -> Void
    let onPhrasePasted: ([String]) -> Void

    private let columns = [
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12)
    ]

    var body: some View {
        LazyVGrid(columns: columns, spacing: 12) {
            ForEach(0..<words.count, id: \.self) { index in
                WordInputField(
                    index: index,
                    word: words[index],
                    colors: colors,
                    isFocused: focusedIndex.wrappedValue == index,
                    onFocus: { focusedIndex.wrappedValue = index },
                    onChange: { word in
                        onWordChange(index, word)
                    },
                    onPhrasePasted: onPhrasePasted,
                    onSubmit: {
                        // Move to next field
                        if index < words.count - 1 {
                            focusedIndex.wrappedValue = index + 1
                        } else {
                            focusedIndex.wrappedValue = nil
                        }
                    }
                )
            }
        }
    }
}

/// Individual word input field
struct WordInputField: View {
    let index: Int
    let word: String
    let colors: CladColors.ColorScheme
    let isFocused: Bool
    let onFocus: () -> Void
    let onChange: (String) -> Void
    let onPhrasePasted: ([String]) -> Void
    let onSubmit: () -> Void

    @State private var localWord: String = ""
    @FocusState private var isFieldFocused: Bool

    var body: some View {
        HStack(spacing: 4) {
            Text("\(index + 1).")
                .font(CladTypography.caption)
                .foregroundColor(colors.onSurfaceVariant)
                .frame(width: 24, alignment: .trailing)

            TextField("", text: $localWord)
                .font(CladTypography.bodyMedium)
                .foregroundColor(colors.onSurface)
                .autocapitalization(.none)
                .autocorrectionDisabled()
                .focused($isFieldFocused)
                .submitLabel(index < 23 ? .next : .done)
                .onSubmit(onSubmit)
                .onChange(of: localWord) { _, newValue in
                    // Handle pasting multiple words
                    let words = newValue.lowercased()
                        .trimmingCharacters(in: .whitespaces)
                        .components(separatedBy: .whitespaces)
                        .filter { !$0.isEmpty }

                    if words.count == 1 {
                        onChange(words[0])
                    } else if words.count == 12 || words.count == 24 {
                        // User pasted a full recovery phrase - auto-fill all fields
                        onPhrasePasted(words)
                        localWord = words[0]
                    } else if words.count > 1 {
                        // Partial paste - just use the first word
                        localWord = words[0]
                        onChange(words[0])
                    }
                }
                .onChange(of: isFieldFocused) { _, focused in
                    if focused {
                        onFocus()
                    }
                }
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 10)
        .background(colors.surface)
        .cornerRadius(8)
        .overlay(
            RoundedRectangle(cornerRadius: 8)
                .stroke(isFocused ? colors.primary : Color.clear, lineWidth: 2)
        )
        .onAppear {
            localWord = word
        }
        .onChange(of: word) { _, newValue in
            if localWord != newValue {
                localWord = newValue
            }
        }
        .onChange(of: isFocused) { _, focused in
            isFieldFocused = focused
        }
    }
}
