# Privacy Policy & Privacy Model

**Last updated**: November 2026

Paisa is built with a singular architectural conviction: **your financial data belongs solely to you, on your device, and nowhere else.**

---

## 1. Zero Network Access (Offline by Design)

Paisa does **not** declare `android.permission.INTERNET` or `android.permission.ACCESS_NETWORK_STATE` in `AndroidManifest.xml`.
- The Android operating system enforces this restriction at the kernel and sandbox level.
- Paisa cannot transmit financial data, account numbers, transactions, logs, telemetry, or analytics to any remote server or third party.
- Paisa contains no third-party tracking SDKs, no ad networks, and no cloud syncing services.

---

## 2. Bank SMS Detection (`RECEIVE_SMS`)

Paisa optionally listens to incoming SMS broadcasts via `android.permission.RECEIVE_SMS` to automate expense tracking from bank alerts.

### Safeguards:
- **Default OFF**: SMS detection is disabled by default during onboarding and requires explicit user opt-in.
- **In-Memory Ephemeral Parsing**: Incoming SMS alerts are read into volatile memory only to extract amount, merchant, and account last 4 digits.
- **No Raw SMS Storage**: The raw text body of incoming SMS messages is never persisted to disk or saved in the database.
- **OTP & Sensitive Message Discard**: Any SMS containing words like `OTP`, `verification code`, `secret code`, `never share`, or `password` is rejected immediately and discarded without further processing.
- **No `READ_SMS` or `SEND_SMS`**: Paisa does not request `READ_SMS` (it cannot read past SMS inbox messages) and does not request `SEND_SMS` (it cannot send text messages).
- **Manual-Only Mode**: Users can toggle "Manual-only Mode" in Settings at any time, completely disabling the SMS broadcast receiver.

---

## 3. Transaction Categorization Notifications (`POST_NOTIFICATIONS`)

On Android 13+ (API level 33 and higher), Paisa optionally requests notification permissions to post interactive categorization action buttons when a transaction is detected from bank SMS.

### Safeguards:
- **Default OFF**: Notifications are disabled by default.
- **Local Notification Processing**: Notification intent actions (e.g. tapping "Food" or "Shopping") are handled locally via an unexported BroadcastReceiver (`TransactionActionReceiver`).
- **No Notification Tracking**: No data regarding notification views, dismissals, or taps leaves your device.

---

## 4. Local Data Storage & Encryption

- **Database**: All accounts, categories, and transaction records are stored in a local SQLite database managed by Android Jetpack Room (`paisa_database.db`).
- **Sandbox Isolation**: The database file resides in Android's private internal storage (`/data/data/com.example.paisa/databases/`), inaccessible to other installed applications unless the device is rooted.
- **Encryption Status**: Data at rest relies on Android's native file-based encryption (FBE) supported on modern Android devices. Database-level application encryption (such as SQLCipher) is on our development roadmap.
- **No Sensitive Account Credentials**: Paisa never asks for or stores full bank account numbers, 16-digit debit/credit card numbers, PINs, passwords, netbanking credentials, or CVVs. Only user-provided account nicknames and the last 4 digits (e.g. `1234`) are retained for transaction matching.

---

## 5. Local Backup and Restore (Storage Access Framework)

Paisa provides local backup export and import:
- Backups are exported as unencrypted, human-readable JSON files (`paisa_backup_<timestamp>.json`).
- Export and import are performed via the Android Storage Access Framework (SAF). The user chooses the exact storage location (e.g. local Downloads folder or an external drive).
- You are solely responsible for securing exported backup files, as they contain transaction histories and account nicknames.

---

## 6. Permissions Summary

| Permission | Android ID | Why Paisa Uses It | What Paisa Never Does |
|---|---|---|---|
| **Receive SMS** | `android.permission.RECEIVE_SMS` | Captures incoming bank debit/credit transaction SMS alerts for automatic expense logging. | Never reads your SMS inbox, never uploads SMS, never stores raw SMS messages or OTPs. |
| **Notifications** | `android.permission.POST_NOTIFICATIONS` | Displays 1-tap category action chips directly in the notification shade when a spend is detected. | Never sends marketing alerts, remote push notifications, or tracking beacons. |

---

## 7. Open Source Transparency

Paisa's entire source code is public and open source. Anyone can inspect, build, and verify our code, Gradle build files, and manifest to confirm our privacy commitments.
