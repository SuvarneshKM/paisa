# Screenshot Guidelines for Paisa

This directory holds official screenshots used in the repository documentation and release assets.

## Screenshot Checklist

When contributing or updating screenshots:

1. **Strictly Anonymous Data**:
   - Use mock bank accounts (e.g. `HDFC Bank •••• 1234`, `Salary Account`).
   - Use simulated transactions with round figures or generic merchants (e.g. `Grocery Store`, `Coffee Shop`, `Metro Rail`).
   - **Never** capture screenshots containing real bank account numbers, UPI IDs, real personal names, real transaction amounts, or personal notifications.

2. **Clean Status Bar**:
   - Set the Android emulator to Demo Mode before capturing:
     ```bash
     # Enable demo mode
     adb shell settings put global sysui_demo_allowed 1
     # Display 100% battery, full Wi-Fi, time at 09:00
     adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0900
     adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
     adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4
     adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
     ```
   - To disable demo mode after capturing:
     ```bash
     adb shell am broadcast -a com.android.systemui.demo -e command exit
     ```

3. **Capture Commands**:
   - Capture via ADB:
     ```bash
     adb shell screencap -p /sdcard/screenshot.png
     adb pull /sdcard/screenshot.png docs/screenshots/home_screen.png
     adb shell rm /sdcard/screenshot.png
     ```

4. **Target Screens & Naming**:
   - `01_home_screen.png` — Main dashboard showing balance cards and recent spends.
   - `02_accounts_screen.png` — Bank cards grid with realistic physical card textures.
   - `03_history_screen.png` — Chronological transaction ledger with category icons.
   - `04_add_transaction.png` — Fast expense entry bottom sheet.
   - `05_category_notification.png` — 1-Tap category selection and transaction detail inspector.
   - `06_privacy_masked.png` — Settings & Privacy controls, local backup, and offline security preferences.
