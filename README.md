# Haatix V2 🖐️✨

> **Touch-Free Android Gesture Controller Powered by On-Device AI Vision**

Haatix V2 turns your Android phone's front camera into an invisible touch controller that feels like using a real finger on the screen. Navigate your phone, click buttons, drag sliders, scroll feeds with continuous joystick control or wave flicks, and return home using natural hand gestures in mid-air—completely offline and privately.

---

## ⚡ What's New in Haatix V2

- 🎯 **Anchor-Frozen Accurate Click:** When your fingers approach a pinch, the cursor locks onto your target ~120ms earlier. The tap fires on *release* at the locked anchor, completely eliminating the hand-shift jitter that plagued earlier gesture controls.
- 🖐️ **Continuous Real-Finger Dragging:** Pinch and hold for 350ms (with haptic feedback) to start a virtual finger drag. Swipe home screen pages, drag the app drawer, or scrub video seekbars smoothly.
- ✌️ **Two-Finger (V-Sign) Continuous Scroll:** Raise two fingers like a laptop touchpad. Move your hand up/down (or left/right) for continuous scroll—farther moves faster, center stops, and automatic ratcheting enables infinite scrolling.
- 🌊 **Wave Flick Scroll:** Quick open-palm waves trigger instantaneous page turns with intelligent return-stroke filtering.
- ⚡ **One Euro Filter:** Replaced sluggish exponential moving averages with the adaptive One Euro Filter—rock-steady when resting, lag-free when moving fast.
- 🚀 **High Performance & Battery Saver:**
  - MediaPipe with GPU delegate and automatic CPU fallback
  - CameraX with `ResolutionSelector` and 3-bitmap reusable memory pool (zero per-frame bitmap allocations)
  - Idle FPS throttling (drops to 8 FPS when no hand is present)
  - Auto adaptive FPS adjusting to device load
- 🎓 **Interactive Practice Trainer:** Built-in interactive training mode to master clicking, scrolling, and page swiping in under 2 minutes.

---

## 📱 Supported Gestures

| Gesture | Description | Trigger Condition |
| :--- | :--- | :--- |
| **Move Cursor** | Move open palm in front camera view | Palm center mapped with One Euro Filter |
| **Click / Tap** | Pinch thumb and index together & release | Quick pinch released within 40..350ms |
| **Finger Drag** | Pinch and hold for 350ms, then move | Continuous touch stroke dispatch |
| **Wave Scroll / Flick** | Quick open-palm wave (Up/Down/Left/Right) | Hand displacement with return-stroke rejection |
| **Continuous Scroll** | Raise index & middle finger (V-Sign) | Hand displacement from center controls velocity |
| **Back Action** | Clench hand into a closed fist for 0.5s & open | Fist held 0.45s..1.4s and released |
| **Pause / Resume** | Clench hand into a closed fist for 1.5s | Fist held continuously for 1.5s |

---

## 📋 Permissions & Setup

Haatix requires:
1. **Camera Permission (`android.permission.CAMERA`):** Captures frames from the front camera for on-device hand tracking.
2. **Display Over Other Apps (`SYSTEM_ALERT_WINDOW`):** Renders the holographic cursor and keep-screen-on overlay.
3. **Haatix Accessibility Service (`AirTouchAccessibilityService`):** Dispatches virtual taps, drags, scrolls, and Back actions.
4. **Vibrate Permission (`android.permission.VIBRATE`):** Haptic feedback when clicking, dragging, and pausing.

### ⚠️ Android 13+ Sideloaded Apps Note ("Restricted Settings")
If you install the APK directly and the Accessibility toggle is disabled / greyed out:
1. Open your phone's **Settings > Apps > Haatix**.
2. Tap the **three dots (⋮)** in the top right corner.
3. Tap **"Allow restricted settings"** and verify with your PIN/fingerprint.
4. Return to Haatix and enable the Accessibility Service.

---

## 🔒 Privacy Statement

Haatix does **NOT** declare `android.permission.INTERNET`. All camera frames are processed in volatile RAM and immediately discarded. The accessibility service is strictly configured with `canRetrieveWindowContent="false"`, meaning it cannot read passwords, messages, or screen content.
