# Security Policy

## Supported Versions

Only the latest release version on the `main` branch receives active security updates and vulnerability patches.

| Version | Supported          |
| ------- | ------------------ |
| 1.1.x   | :white_check_mark: |
| < 1.1   | :x:                |

---

## Security Model & Privacy Guarantees

Paisa is architected around strict local-first and privacy-first security principles:

1. **No Network Access**: The application does not declare `android.permission.INTERNET` in its manifest. Sockets and remote network calls are hard-blocked by the Android operating system.
2. **Ephemeral SMS Processing**: Incoming SMS messages are inspected strictly in-memory by `SmsReceiver`. Parsed transaction details (amount, last 4 digits, merchant name) are saved to Room, while raw SMS text is discarded immediately.
3. **Strict OTP & 2FA Rejection**: `BankSmsParser` filters out one-time passwords, secret codes, and authentication messages with dedicated rejection regex rules.
4. **Local Database Sandboxing**: All records are stored in an internal SQLite database (`paisa_database.db`) protected by Android's application sandbox and Linux filesystem permissions.
5. **No Full Card or Account Numbers**: Only the last 4 digits are recorded to match transactions with user-defined accounts.

---

## Reporting a Vulnerability

We take the security and privacy of our users very seriously. If you discover a security vulnerability, please report it responsibly:

- **Preferred Method**: Use [GitHub Private Vulnerability Reporting](https://docs.github.com/en/code-security/security-advisories/guidance-on-reporting-and-communicating/privately-reporting-a-security-vulnerability) directly within this repository by navigating to the **Security** tab and clicking **Report a vulnerability**.
- **Alternative Method**: If private reporting is unavailable, please open a draft security advisory on GitHub or contact the repository owner privately via their GitHub profile.

**Please do NOT disclose security vulnerabilities publicly in open GitHub issues or discussions until a patch has been released.**

### When Submitting a Report, Please Include:
1. Description of the vulnerability and its potential impact.
2. Steps to reproduce the issue (including sample non-sensitive payloads or synthetic SMS strings).
3. Android OS version and device model where the issue was reproduced.
4. Any proposed remediations or patches if available.

### Response Timeline
- We will acknowledge receipt of your report within 48 hours.
- We will assess the issue, provide regular progress updates, and release a patch as quickly as possible.
- Once a fix is verified and released, credit will be given in our release notes (unless you request anonymity).
