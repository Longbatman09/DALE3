# DALE Test Journeys: App Detection & Logging

This document outlines the "Journeys" (test cases) to verify the core protection loop: Detection -> Lock Overlay -> Verification -> Logging.

---

## Journey 1: Standard Protection Flow
**Goal**: Verify that a protected app is successfully intercepted and logged correctly upon unlock.

1.  **Setup**: Ensure DALE protection is ON and a "Test Group" is created with at least one app (e.g., Chrome).
2.  **Action**: Launch Chrome from the Android Home Screen.
3.  **Expected Result**:
    *   The DALE Lock Screen appears immediately, covering Chrome.
    *   Chrome is NOT yet visible to the user.
4.  **Action**: Enter the correct PIN/Pattern on the DALE Lock Screen.
5.  **Expected Result**:
    *   Lock screen dismisses.
    *   Chrome becomes visible.
6.  **Log Verification**:
    *   Open DALE -> Activity Logs for "Test Group".
    *   Check for entry: `Chrome - OPENED - [Current Timestamp]`.

---

## Journey 2: Exit Grace Period & "CLOSED" Logging
**Goal**: Verify the system detects when an app is closed and respects the grace period for re-entry.

1.  **Setup**: Complete Journey 1 so Chrome is currently "unlocked".
2.  **Action**: Press the **Home** button to exit Chrome.
3.  **Action**: Wait at least **3 seconds** (exceeding the 2s `exitGracePeriodMs`).
4.  **Log Verification**:
    *   Open DALE -> Activity Logs.
    *   Check for entry: `Chrome - CLOSED - [Timestamp]`.
5.  **Action**: Re-launch Chrome.
6.  **Expected Result**: The Lock Screen appears again (session expired).

---

## Journey 3: Quick Switch (Within Grace Period)
**Goal**: Verify that users aren't pestered by the lock screen if they briefly leave the app.

1.  **Setup**: Unlock a protected app (e.g., Chrome).
2.  **Action**: Swipe up to go Home, then **immediately** (within 1 second) tap the Chrome icon again.
3.  **Expected Result**:
    *   Chrome opens directly without showing the Lock Screen.
4.  **Log Verification**:
    *   No new `CLOSED` or `OPENED` logs should be generated for this transition.

---

## Journey 4: Cross-App Transition (App Groups)
**Goal**: Verify logging and detection when moving between two apps in the same protected group.

1.  **Setup**: Create a group with **App A** (Chrome) and **App B** (YouTube).
2.  **Action**: Open App A -> Unlock -> Use for 5 seconds.
3.  **Action**: Open **Recents/Multitasking** and switch directly to App B.
4.  **Expected Result**:
    *   DALE Lock Screen appears for App B.
5.  **Action**: Unlock App B.
6.  **Log Verification**:
    *   `App A - CLOSED` (triggered by foreground switch).
    *   `App B - OPENED` (triggered by unlock).

---

## Journey 5: Security & Bypass Prevention
**Goal**: Ensure the lock screen cannot be easily bypassed.

1.  **Action**: While the Lock Screen is displayed, press the **System Back** button.
    *   **Result**: Lock screen should stay (Back is consumed).
2.  **Action**: While the Lock Screen is displayed, press the **Recents** button.
    *   **Result**: System shows recents. Tap the protected app again.
    *   **Result**: Lock screen should immediately reappear (via `onStop`/`onUserLeaveHint` relaunch logic).
3.  **Action**: Enter an **Incorrect PIN**.
    *   **Result**: Error "Incorrect PIN" appears, dots shake or clear, no log is recorded.

---

## Journey 6: Service Persistence
**Goal**: Verify detection survives service restarts.

1.  **Action**: In Android Settings -> Accessibility, toggle DALE OFF then ON.
2.  **Action**: Open a protected app.
3.  **Expected Result**: Lock screen appears immediately (Service re-initialized and resumed monitoring).

---

## Success Criteria for "Correct Activity Logs"
| Event | Trigger Point | Expected Value |
| :--- | :--- | :--- |
| **OPENED** | `unlockApp()` in `DrawOverOtherAppsLockScreen` | Recorded only after valid credentials. |
| **CLOSED** | `onAccessibilityEvent` + 2s delay | Recorded when the app is no longer the foreground window. |
| **UNLOCKED** | Internal Broadcast | Used to sync state between Service and UI. |
