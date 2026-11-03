# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.1.0] - 2026-11-03

### Added
- **Material Symbols Rounded Vector Icon System**: Replaced all category emoji icons with 22 locally bundled, scalable vector drawables mapped via `CategoryIconRegistry`.
- **Smart Category Suggestions**: Deterministic, offline heuristic engine for incoming bank transactions with tiered amount fallbacks, learned merchant preferences, and keyword matching.
- **Offline JSON Backup & Restore**: Storage Access Framework integration in `BackupManager` to export and import complete account, category, transaction, and preference data.
- **Single Primary Account Invariant**: Enforced single primary account guarantee across create and update flows in `BankAccountDao` and `ExpenseTrackerRepository`.
- **Responsive Bank Chip Selection**: Redesigned bank selection in Add/Edit Account sheets using an accessible `FlowRow` grid with immediate realistic bank card preview.

### Changed
- Refactored Add and Edit Bank Account forms for consistent vertical spacing, IME insets padding, and proper scroll behavior.
- Fixed "Set as Primary Account" toggle row layout, eliminating text overlapping the Material 3 Switch and providing a single accessible touch target.
- Enhanced `RealisticBankCard` with authentic embossed typography, metallic chip rendering, and brand-specific gradient palettes.
- Upgraded `RoomDatabase` migration strategy from schema version 1 to 2 with explicit non-destructive column additions.

### Removed
- Removed legacy Glance home-screen widget implementation and associated dependencies to keep the app focused, lightweight, and strictly on-demand.
- Removed bank search input from account sheets in favor of direct 1-tap bank chip selection.

---

## [1.0.0] - 2026-10-26

### Added
- Initial release of Paisa for Android.
- Offline-first personal finance tracking with zero network permissions (`INTERNET` permission absent).
- Room SQLite database with integer paise financial arithmetic.
- Multi-account management supporting Indian bank presets (SBI, HDFC, Axis, ICICI, Kotak, Federal, Canara, SIB, IDFC FIRST, Custom).
- Incoming bank SMS debit and credit parsing via `RECEIVE_SMS` with OTP rejection and deduplication keys.
- Quick categorization notification actions via `POST_NOTIFICATIONS`.
- Transaction search, date-range filtering, and monthly spending analytics.
- "Hide Balances" privacy toggle for discreet usage in public environments.
- Onboarding flow with explicit privacy disclosures and permission opt-ins.
