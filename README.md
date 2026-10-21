# AirTouch V1 🖐️✨

> **Touch-Free Android Gesture Controller Powered by On-Device AI Vision**

AirTouch turns your Android phone's front camera into an invisible touch controller. Navigate your phone, click buttons, scroll feeds, and return home using natural hand gestures in mid-air—completely offline and privately.

---

## ⚡ Features & Capabilities

- 🦾 **Iron-Man Holographic Overlay Cursor:** Sleek cyan neon reticle with real-time gesture feedback, countdown arcs, and state pill.
- 🎯 **Non-Jumping Palm Center Tracking:** The cursor tracks your **palm center** (average of landmarks 0, 5, 9, 13, and 17) rather than fingertips—meaning pinching to click never causes your cursor to jump.
- 📐 **Dead-Zone & Exponential Moving Average (EMA) Smoothing:** Removes natural hand micro-tremors for rock-steady cursor placement.
- 👆 **Pinch to Tap:** Bring thumb tip (4) and index tip (8) together to tap on the active cursor coordinate. Includes palm-size scaling, hysteresis, and a 150ms debounce.
- 📜 **Open Palm Velocity-Based Scroll:** Quick vertical palm swipes trigger smooth scroll up and down gestures.
- ✊ **Fist Held 0.7s for Back:** Clench your hand into a fist for 0.7 seconds to execute Android's system Back action.
- ⏸️ **Open Palm Held Still 2.0s for Pause/Resume:** Safety switch that toggles tracking on/off without needing to touch the phone.
- 🛡️ **Zero-Network Privacy (No Internet Permission):** Runs 100% on-device with MediaPipe Tasks Vision. No frames or biometric data leave the phone.
- 💡 **Keep Screen On Overlay:** Optional 1x1 transparent overlay maintains phone screen awake during gesture sessions.

---

## 🛠️ Technology Stack

- **OS / Target:** Android (minSdk 26, targetSdk 35)
- **Language & UI:** Kotlin, Jetpack Compose, Material 3
- **Vision Pipeline:** AndroidX CameraX (Front camera, 640x480 resolution, 15-30 FPS cap)
- **Machine Learning:** Google MediaPipe Tasks Vision HandLandmarker (`LIVE_STREAM` mode, offline `hand_landmarker.task` model)
- **Input Dispatch:** Android `AccessibilityService` (`dispatchGesture` for taps & scrolls, `performGlobalAction` for Back)
- **Overlay:** `WindowManager` with `TYPE_APPLICATION_OVERLAY`
- **Build System:** Gradle Kotlin DSL (`.gradle.kts`), Java 17

---

## 📱 Supported Gestures

| Gesture | Description | Trigger Condition |
| :--- | :--- | :--- |
| **Move Cursor** | Move open palm in camera view | Palm center mapped with margin & EMA smoothing |
| **Pinch to Tap** | Touch thumb and index tips together | Thumb-index distance / palm size < threshold (150ms debounce) |
| **Swipe to Scroll** | Fast vertical movement with open palm | Vertical palm velocity > 1.1 units/s |
| **Fist for Back** | Clench hand into a closed fist | All fingertips curled to MCPs held for 0.7s |
| **Pause / Resume** | Hold open palm completely still | Palm speed < threshold held for 2.0s |

*A 600ms cooldown is enforced between all discrete actions to prevent accidental repeated triggers.*

---

## 📋 Permissions & Setup

To operate, AirTouch requires three Android permissions:
1. **Camera Permission (`android.permission.CAMERA`):** Captures frames from the front camera for on-device hand tracking.
2. **Display Over Other Apps (`SYSTEM_ALERT_WINDOW`):** Renders the holographic cursor and keep-screen-on overlay.
3. **AirTouch Accessibility Service:** Dispatches touch taps, scrolls, and the Back action.

### ⚠️ Android 13+ Sideloaded Apps Note ("Restricted Settings")
If you install the APK directly and the Accessibility toggle is disabled / greyed out:
1. Open your phone's **Settings > Apps > AirTouch**.
2. Tap the **three dots (⋮)** in the top right corner.
3. Tap **"Allow restricted settings"** and verify with your PIN/fingerprint.
4. Return to AirTouch and enable the Accessibility Service.

---

## 🏗️ Building from Source

### Prerequisites
- JDK 17 (e.g. Eclipse Temurin 17)
- Android SDK with platform-tools and build-tools for API 35
- Gradle wrapper (included)

### 1. Download Model Asset
Before compiling, download the MediaPipe Hand Landmarker model into `app/src/main/assets/`:
```bash
mkdir -p app/src/main/assets
curl -L -o app/src/main/assets/hand_landmarker.task \
  https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task
```

### 2. Build Debug APK
```bash
./gradlew assembleDebug
```
The output APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🤖 GitHub Actions CI/CD

AirTouch includes an automated GitHub Actions workflow (`.github/workflows/build.yml`) that:
- Automatically triggers on push to `main` and manual `workflow_dispatch`
- Sets up JDK 17
- Fetches `hand_landmarker.task` via curl
- Builds the debug APK with `./gradlew assembleDebug`
- Uploads the signed APK artifact `AirTouch-debug-apk` ready for installation on physical devices.

---

## 🔒 Privacy Statement

AirTouch does **NOT** declare `android.permission.INTERNET`. All camera frames are processed in volatile RAM and immediately discarded. The accessibility service is strictly configured with `canRetrieveWindowContent="false"`, meaning it cannot read passwords, messages, or screen content.
