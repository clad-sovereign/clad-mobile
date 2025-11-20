import SwiftUI

/// Institutional typography system for CLAD Signer
/// Large, readable fonts designed for government officials (non-technical users)
/// Uses iOS semantic font sizes for better accessibility and dynamic type support
struct CladTypography {
    // MARK: - Display & Headline Styles

    /// Main titles (e.g., "CLAD Signer") - Large Title (34pt default)
    static let headlineLarge = Font.largeTitle.weight(.bold)

    /// Section headers - Title (28pt default)
    static let headlineMedium = Font.title.weight(.semibold)

    // MARK: - Title Styles

    /// Card titles - Title 2 (22pt default)
    static let titleLarge = Font.title2.weight(.semibold)

    /// Subsection titles - Title 3 (20pt default)
    static let titleMedium = Font.title3.weight(.medium)

    // MARK: - Body Text

    /// Primary body text - Body (17pt default)
    /// Large and comfortable for reading government/financial content
    static let bodyLarge = Font.body

    /// Secondary body text - Callout (16pt default)
    /// Minimum recommended size for accessibility
    static let bodyMedium = Font.callout

    // MARK: - Labels & Captions

    /// Labels and captions - Subheadline (15pt default)
    static let labelLarge = Font.subheadline.weight(.medium)

    /// Small captions - Footnote (13pt default)
    /// Used sparingly, only for secondary metadata
    static let caption = Font.footnote

    // MARK: - Special Styles

    /// Monospace for node stream messages - Caption Monospaced (12pt default)
    static let codeSmall = Font.caption.monospaced()
}
