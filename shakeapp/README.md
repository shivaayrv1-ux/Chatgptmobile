# Shake & Volume Screen Control

Android native Kotlin app based on the supplied project source.

## Features
- Shake gesture toggles screen state while the foreground sensor service is enabled.
- Volume Up can wake the display when the screen is off (requires Accessibility service and may be restricted by device/OEM behavior).
- Battery optimization status and settings shortcuts.
- Saves toggle settings locally.
- Starts the shake service again after reboot if Shake to toggle screen is enabled.

## Build APK without a PC
1. Upload/push this project to a GitHub repository from your phone.
2. Open the repository's **Actions** tab.
3. Run **Build Android APK** (or push to `main`/`master`).
4. Open the completed workflow run and download the `Shake-Volume-Screen-Control-APK` artifact.
5. Extract the artifact ZIP and install `app-debug.apk` on your Android phone.

Android will ask you to enable permissions from inside the app. Accessibility is a sensitive Android permission; enable it only if you understand the described screen-control behavior. Device/OEM restrictions can affect background sensor and screen-wake behavior.
