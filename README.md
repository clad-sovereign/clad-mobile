![Clad Signer Demo – Native iOS & Android](https://cdn.loom.com/sessions/thumbnails/dd334230db154f9891f46664ae02aec4-9e6c0699711bd8ff-full-play.gif#t=0.1)\
*[Watch 60-second demo – November 2025](https://www.loom.com/share/dd334230db154f9891f46664ae02aec4)*

# Clad Signer

**Native mobile signing application for sovereign and emerging-market RWA tokenization on Polkadot/Substrate**
Kotlin Multiplatform • iOS & Android • Biometric authentication • Offline-capable

Clad Signer eliminates the browser and extension dependency that prevents finance-ministry and debt-office officials from using Substrate-based tokenized assets in practice.

Designed and built as the mobile counterpart to [Clad Studio](https://github.com/clad-sovereign/clad-studio) — the open-source tokenization toolkit targeting sovereign issuers and state-owned enterprises.

### Key features (MVP → Q1 2026)

- Full Substrate extrinsic signing via Polkadot.kt / Rust-based metadata parsing
- Biometric authentication (Face ID / Touch ID / Android BiometricPrompt)
- Offline QR-code signing for low/no-connectivity environments
- Direct RPC / WebSocket connection to any Polkadot parachain or relay chain
- Zero crypto jargon — UI/UX tailored for government officials
- Support for `pallet-clad-token` compliance actions (mint, transfer, freeze, whitelist)

### Target users

- Finance ministry staff
- Debt-management office directors
- Central-bank digital-asset teams
- State-owned enterprise treasury departments

Primary jurisdictions (2026 pilots): Indonesia • Kazakhstan • Nigeria • Egypt • Peru • Vietnam • Côte d'Ivoire • Uzbekistan • Rwanda • Paraguay follow-ons


### Status & Roadmap

| Phase                  | Timeline         | Milestones |
|------------------------|------------------|------------|
| Phase 1 – Core signer  | Nov 2025 – Jan 2026 | Biometric login • Online + QR offline signing • Integration with `pallet-clad-token` |
| Phase 2 – Admin tools  | Feb – Apr 2026   | Full transaction builder • Real-time balance & compliance status • First sovereign pilots |
| Phase 3 – White-label  | H2 2026 onward   | Per-country branding • Localized UI • Central-bank node bundles |

License: Apache-2.0

Part of the Clad Studio sovereign RWA stack — aligned with Polkadot's official RWA tokenization guidelines.

Contact: helloclad@wideas.tech

> **Disclaimer**  
> Clad Sovereign is pre-pilot software. It is not yet intended for production use or real fund issuance. Use only on testnets or local chains.