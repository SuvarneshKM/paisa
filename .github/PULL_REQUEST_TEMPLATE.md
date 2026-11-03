## Description

Please describe the changes proposed in this pull request and the problem they solve.

Fixes #(issue)

---

## Type of Change

- [ ] Bug fix (non-breaking change which fixes an issue)
- [ ] New feature (non-breaking change which adds functionality)
- [ ] Bank SMS parser improvement (added/updated bank pattern)
- [ ] Refactoring / Performance enhancement
- [ ] Documentation update

---

## Architectural & Privacy Checklist

- [ ] **Zero Network Access**: This change introduces NO network permissions (`INTERNET`), third-party analytics, or cloud dependencies.
- [ ] **Integer Financial Math**: All currency calculations strictly use integer paise (`Long`) and avoid floating-point drift.
- [ ] **SMS Privacy**: Raw SMS text is never saved or logged; OTPs/2FA codes are filtered out immediately.
- [ ] **No Emojis**: Used Material Symbols Rounded vector XML drawables from `CategoryIconRegistry` instead of emoji characters.
- [ ] **No Real Financial Data**: All unit tests use synthetic, sanitized mock data.

---

## Verification & Testing

Please describe the tests you ran to verify your changes:

- [ ] Local unit tests pass: `./gradlew testDebugUnitTest`
- [ ] Android Lint passes: `./gradlew lintDebug`
- [ ] Debug build succeeds: `./gradlew assembleDebug`
- [ ] Tested on a physical device or emulator (Device / OS: ________________)

---

## Screenshots / Video (If UI change)

*Attach before and after screenshots using sanitized/mock data if applicable.*
