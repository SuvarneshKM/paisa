# Contributing to Paisa

Thank you for your interest in contributing to Paisa! We welcome bug reports, improvements to Indian bank SMS parsing patterns, documentation refinements, and feature pull requests.

Paisa is an offline-first, privacy-focused personal finance manager built exclusively with Kotlin and Jetpack Compose for Android.

---

## Guiding Principles

Before proposing or implementing changes, please keep in mind our core architecture and privacy requirements:

1. **Zero Network Usage**: Paisa does not request `android.permission.INTERNET`. Any pull request introducing network dependencies, analytics, telemetry, or remote crash reporters will be declined.
2. **Offline-First & Local Storage**: All financial data resides on-device in Room SQLite. Backups are exported locally via Android's Storage Access Framework (SAF).
3. **Privacy by Default**: SMS alerts are parsed ephemerally in memory to extract amount, merchant, and last 4 digits. Full SMS text is discarded immediately and never stored or logged. OTPs and 2FA messages must be rejected immediately.
4. **Precision Financial Math**: All currency calculations use 64-bit integer paise (`Long`). Never use floating-point types (`Float` or `Double`) for balance calculations or database storage.

---

## Development Prerequisites

- **JDK**: Java 17 or Java 21 (foojay toolchain supported)
- **Android Studio**: Android Studio Meerkat (2024.3+) or newer
- **Android SDK**: `minSdk = 26`, `compileSdk = 36`, `targetSdk = 36`
- **Gradle**: 9.4.1 (managed via the included `gradlew` wrapper)

---

## Building and Testing Locally

Clone the repository and test the build from your terminal:

```bash
# Check Kotlin code compilation and run all local unit tests
./gradlew testDebugUnitTest

# Run Android Lint on debug variant
./gradlew lintDebug

# Build the debug APK
./gradlew assembleDebug
```

The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## Testing Bank SMS Parsers

When adding or refining bank SMS patterns:

1. Never submit real personal bank SMS messages, account numbers, or real UPI references in code or pull requests.
2. Add synthetic test fixtures to [`BankSmsParserTest.kt`](app/src/test/java/com/example/paisa/parser/BankSmsParserTest.kt).
3. Ensure both debit and credit regex patterns correctly extract:
   - Amount in paise
   - Account last 4 digits
   - Merchant / Beneficiary / VPA name
   - Transaction reference / UPI reference
4. Verify that OTPs, promotional alerts, payment requests, and failed transactions are rejected by the parser.

---

## Code Style & Guidelines

- **Kotlin Official Style**: We follow the official Kotlin coding conventions (`kotlin.code.style=official`).
- **Jetpack Compose**: Follow standard Compose state-hoisting and unidirectional data flow (UDF) patterns. State should flow down, and events should flow up via lambdas.
- **Vector Icons**: Use local Material Symbols Rounded vector XML drawables registered in `CategoryIconRegistry`. Do not use emoji characters in categories or UI controls.
- **Maintain Documentation Integrity**: Preserve existing comments and docstrings. Document non-obvious regex patterns or database migration steps thoroughly.

---

## Pull Request Workflow

1. **Fork & Branch**: Create a feature or bugfix branch off `main`:
   ```bash
   git checkout -b feature/my-improvement
   ```
2. **Run Tests**: Ensure all unit tests and lint checks pass:
   ```bash
   ./gradlew testDebugUnitTest lintDebug
   ```
3. **Commit Messages**: Write clear, descriptive commit messages:
   ```
   feat(parser): add support for Canara Bank UPI debit format
   fix(accounts): prevent primary account switch text wrapping
   ```
4. **Open a Pull Request**: Submit your pull request against the `main` branch. Fill out the provided pull request template completely, including testing details.

---

## Code of Conduct

All contributors and maintainers are expected to adhere to our [Code of Conduct](CODE_OF_CONDUCT.md). Please report unacceptable behavior through our reporting guidelines.
