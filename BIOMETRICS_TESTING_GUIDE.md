# Biometrics in Group Creation - Testing Guide

## Overview
This guide walks through testing the new biometric authentication flow in DALE's group creation process.

## Prerequisites
- Android device or emulator with:
  - Fingerprint sensor (or emulated via developer options)
  - At least one fingerprint enrolled
  - Android API 28+ (BiometricManager requirement)

## Test Cases

### Test Case 1: Biometric Option Visibility

**Objective**: Verify biometrics option only appears on capable devices

**Steps**:
1. Start group creation (AppSelectionActivity → Select 2 apps → Name group)
2. Reach PasswordSetupActivity (Lock Authentication screen)
3. Look for authentication options

**Expected Results**:
- ✓ PIN option is ALWAYS enabled
- ✓ PASSWORD option is ALWAYS enabled  
- ✓ PATTERN option is ALWAYS enabled
- ✓ BIOMETRICS option appears IF:
  - Device has fingerprint sensor (hasSystemFeature FEATURE_FINGERPRINT)
  - At least one fingerprint is enrolled
  - BiometricManager.canAuthenticate() returns BIOMETRIC_SUCCESS
- ✓ BIOMETRICS option is greyed out and disabled if above conditions not met

**Failure Indicators**:
- ✗ Biometrics option appears on device without fingerprint sensor
- ✗ Biometrics option enabled but no fingerprints enrolled
- ✗ Biometrics always disabled even with fingerprints enrolled

---

### Test Case 2: Biometric Apps Selection

**Objective**: Verify user can select which apps to protect with biometrics

**Prerequisites**: 
- Device has biometric capability
- Biometrics option is enabled

**Steps**:
1. Reach Lock Authentication screen
2. Click on BIOMETRICS option
3. BiometricAppsSelectionDialog should appear

**Expected Results**:
- ✓ Dialog shows correct app names (App 1 and App 2)
- ✓ Both apps have toggle switches
- ✓ Toggles start in OFF position
- ✓ Can toggle apps independently
- ✓ "Next" button is disabled when no apps selected
- ✓ "Next" button enables when at least one app selected
- ✓ Can go back with Cancel button

**Valid Selections to Test**:
- [ ] App 1 only
- [ ] App 2 only  
- [ ] Both App 1 and App 2

---

### Test Case 3: Biometric Policy Selection

**Objective**: Verify policy selection (Biometric only vs + Backup)

**Prerequisites**:
- Completed: Apps Selection with at least one app selected

**Steps**:
1. Click Next from BiometricAppsSelectionDialog
2. BiometricPolicyDialog should appear
3. For each selected app, observe toggle

**Expected Results**:
- ✓ Shows only selected apps
- ✓ Each app shows toggle with two states:
  - LEFT (toggle OFF): "Biometric only"
  - RIGHT (toggle ON): "Biometric + Backup"
- ✓ Defaults to "Biometric only" (toggle OFF)
- ✓ Can toggle per-app independently
- ✓ Clear description of each policy

**Test Combinations**:
- [ ] Both apps: Biometric only
- [ ] Both apps: Biometric + Backup
- [ ] App 1: Biometric only, App 2: Biometric + Backup
- [ ] App 1: Biometric + Backup, App 2: Biometric only

---

### Test Case 4: Backup Credential Selection (No Backup Path)

**Objective**: Test flow when "Biometric only" is selected

**Prerequisites**:
- Completed: Policy selection with "Biometric only" for all apps

**Steps**:
1. Click Next from BiometricPolicyDialog
2. Observe next screen

**Expected Results**:
- ✓ Skips backup credential dialogs
- ✓ Shows Overlay Permission dialog
- ✓ No PIN/Password/Pattern entry required
- ✓ Can complete setup by granting overlay permission

**Data Saved**:
- ✓ AppGroup.app1FingerprintEnabled = true (if selected)
- ✓ AppGroup.app2FingerprintEnabled = true (if selected)
- ✓ AppGroup.app1FingerprintBiometricOnly = true
- ✓ AppGroup.app2FingerprintBiometricOnly = true
- ✓ AppGroup.app1LockType = "BIOMETRIC"
- ✓ AppGroup.app2LockType = "BIOMETRIC"
- ✓ AppGroup.app1LockPin = "" (empty)
- ✓ AppGroup.app2LockPin = "" (empty)

---

### Test Case 5: Backup PIN Selection

**Objective**: Test backup credential type selection

**Prerequisites**:
- Completed: Policy selection with "Biometric + Backup" for at least one app

**Steps**:
1. Click Next from BiometricPolicyDialog
2. BiometricBackupCredentialDialog appears for first app needing backup
3. Dialog shows three backup type options:
   - PIN (4 digit PIN)
   - PASSWORD (Alphanumeric password)
   - PATTERN (Draw a pattern)

**Expected Results**:
- ✓ Dialog title shows correct app name
- ✓ All three backup options are visible and clickable
- ✓ Clear descriptions under each
- ✓ "Skip Backup" button available
- ✓ Selecting backup type transitions to credential entry

**Test Each Backup Type**:
- [ ] PIN
- [ ] PASSWORD
- [ ] PATTERN

---

### Test Case 6: Backup Credential Entry - PIN

**Objective**: Test PIN entry for backup authentication

**Prerequisites**:
- Completed: Backup type selection with PIN

**Steps**:
1. CredentialEntryScreen appears for "Enter PIN for [App Name]"
2. See PIN entry with 4 dots display
3. Enter 4-digit PIN (e.g., 1234)
4. Click Next
5. "Confirm PIN" screen appears
6. Enter same PIN
7. Click Confirm

**Expected Results**:
- ✓ Shows correct app name
- ✓ Step indicator shows "Step 1 of 2" → "Step 2 of 2"
- ✓ 4 dots display as you type
- ✓ Can only input digits (no letters)
- ✓ Requires exactly 4 digits
- ✓ Cannot use same PIN as other app
- ✓ PIN mismatch shows error: "PINs do not match"
- ✓ Matching PINs advance to next step (or finish if last app)

**Test Error Cases**:
- [ ] Try entering 3 digits → "Next" disabled
- [ ] Try entering 5 digits → Only accepts 4
- [ ] Try entering same PIN as App 1 → Error shown
- [ ] Enter different PIN on confirm → Error and restart

---

### Test Case 7: Backup Credential Entry - Password

**Objective**: Test password entry for backup authentication

**Prerequisites**:
- Completed: Backup type selection with PASSWORD

**Steps**:
1. CredentialEntryScreen appears for "Enter Password for [App Name]"
2. Text field visible (masked)
3. Enter password (e.g., MyPass123)
4. Click Next
5. "Confirm Password" screen appears
6. Enter same password
7. Click Confirm

**Expected Results**:
- ✓ Shows correct app name
- ✓ Step indicator shows "Step 1 of 2" → "Step 2 of 2"
- ✓ Text is masked (dots instead of characters)
- ✓ Minimum 6 characters required
- ✓ Accepts letters, numbers, special characters
- ✓ Cannot use same password as other app
- ✓ Password mismatch shows error: "Passwords do not match"
- ✓ Max 32 characters

**Test Cases**:
- [ ] Password with 5 chars → "Next" disabled
- [ ] Password with uppercase: "Password123"
- [ ] Password with special chars: "Pass@123"
- [ ] Very long password → Trimmed to 32 chars
- [ ] Different password on confirm → Error and restart

---

### Test Case 8: Backup Credential Entry - Pattern

**Objective**: Test pattern entry for backup authentication

**Prerequisites**:
- Completed: Backup type selection with PATTERN

**Steps**:
1. CredentialEntryScreen appears for "Enter Pattern for [App Name]"
2. See 3x3 grid of dots
3. Draw pattern connecting 4+ dots
4. Release to confirm pattern
5. Grid clears for "Confirm Pattern"
6. Draw same pattern
7. Release to confirm

**Expected Results**:
- ✓ Shows correct app name
- ✓ Step indicator shows "Step 1 of 2" → "Step 2 of 2"
- ✓ 3x3 grid is visible
- ✓ Can draw pattern by connecting dots
- ✓ Minimum 4 dots required
- ✓ Message: "Pattern must connect at least 4 dots"
- ✓ Cannot use same pattern as other app
- ✓ Pattern mismatch shows error: "Patterns do not match"
- ✓ Max 9 dots (entire grid)

**Test Patterns**:
- [ ] 4-dot pattern: "1-2-3-4"
- [ ] 9-dot pattern: All dots
- [ ] L-shaped pattern: "1-4-7-8"
- [ ] Different pattern on confirm → Error and restart

---

### Test Case 9: Multiple Apps with Different Backups

**Objective**: Test different backup types for App 1 vs App 2

**Scenario**: App 1 uses PIN, App 2 uses Password

**Steps**:
1. Apps Selection: Enable both apps
2. Policy Selection: Both set to "Biometric + Backup"
3. Backup for App 1: Select PIN, enter 1234
4. Backup for App 2: Select PASSWORD, enter MyPassword1
5. Complete overlay permission

**Expected Results**:
- ✓ Shows backup dialog for App 1 first
- ✓ After App 1 PIN confirmed, shows backup dialog for App 2
- ✓ After App 2 password confirmed, shows overlay dialog
- ✓ Both backup types properly saved

**Verify in Settings**:
- [ ] Go to Group Settings → Fingerprint Unlock
- [ ] App 1 shows biometric enabled with PIN fallback
- [ ] App 2 shows biometric enabled with Password fallback

---

### Test Case 10: Skip Backup Option

**Objective**: Test skipping backup at any point

**Prerequisites**:
- On BiometricBackupCredentialDialog

**Steps**:
1. Click "Skip Backup" button
2. Observe next step

**Expected Results**:
- ✓ If skipping App 1: Moves to App 2 backup (if needed)
- ✓ If skipping last app: Goes to overlay permission
- ✓ No PIN/Password/Pattern is saved
- ✓ Biometric remains enabled
- ✓ AppGroup saved with biometric-only policy

**Data Saved**:
- ✓ app1LockPin = "" (empty)
- ✓ app1FingerprintBiometricOnly = true
- ✓ No backup credential stored

---

### Test Case 11: Overlay Permission

**Objective**: Test overlay permission flow after biometric setup

**Prerequisites**:
- Completed all biometric/backup steps

**Steps**:
1. Overlay Permission dialog appears
2. Click "Open Overlay Settings" or "Grant Permission"
3. Settings app opens (ACTION_MANAGE_OVERLAY_PERMISSION)
4. Grant permission to DALE
5. Return to app (or app auto-completes)

**Expected Results**:
- ✓ Dialog message is clear
- ✓ Settings opens correctly
- ✓ After permission granted, setup completes
- ✓ Redirects to MainActivity
- ✓ Can see new group in home screen

---

### Test Case 12: Setup Completion and Home Screen

**Objective**: Verify group appears correctly after biometric setup

**Prerequisites**:
- Completed full biometric setup flow

**Steps**:
1. After overlay permission (or if already granted), app completes
2. Navigate to MainActivity/HomeScreen
3. Look for newly created group

**Expected Results**:
- ✓ Group appears in list with correct name
- ✓ Shows both app names
- ✓ Lock status shows 🔒 (locked) 
- ✓ Can open Group Settings
- ✓ Group Settings shows "Fingerprint Unlock" option

**In Group Settings**:
- ✓ "Fingerprint Unlock" card visible
- ✓ Clicking opens FingerprintSelectionDialog
- ✓ Shows correct biometric/backup configuration for each app

---

### Test Case 13: Comparison with Regular Authentication

**Objective**: Verify biometric flow doesn't break regular PIN/Password/Pattern flow

**Prerequisites**:
- None (fresh group creation)

**Steps**:
1. Start new group creation
2. Select "PIN" instead of BIOMETRICS
3. Complete regular PIN setup
4. Compare with biometric setup

**Expected Results**:
- ✓ Regular PIN flow unchanged
- ✓ Regular flow works normally
- ✓ Can mix groups with different auth methods
  - One group with biometric
  - One group with PIN
  - One group with password
- ✓ All groups function independently

---

### Test Case 14: Device Without Biometrics

**Objective**: Test behavior on device without fingerprint sensor

**Prerequisites**:
- Android device/emulator WITHOUT fingerprint capability

**Steps**:
1. Start group creation
2. Reach Lock Authentication screen
3. Look for BIOMETRICS option

**Expected Results**:
- ✓ BIOMETRICS option is greyed out (disabled)
- ✓ Hover/click shows "Fingerprint sensor not available"
- ✓ Cannot select BIOMETRICS
- ✓ PIN, PASSWORD, PATTERN still available
- ✓ Can complete setup with other methods

---

### Test Case 15: Integration with Group Settings

**Objective**: Test biometric settings visible in Group Settings

**Prerequisites**:
- Completed biometric group creation

**Steps**:
1. Open created group
2. Click "Fingerprint Unlock"
3. FingerprintSelectionDialog appears

**Expected Results**:
- ✓ Shows correct biometric settings:
  - App 1 toggle shows "Enabled" if selected
  - App 2 toggle shows "Enabled" if selected
- ✓ Sub-toggles show correct policy:
  - "Biometric only" if no backup
  - "Biometric + PIN/Password/Pattern fallback" if backup
- ✓ Can modify settings (enable/disable biometric)
- ✓ Save button works
- ✓ Changes persist

---

## Performance Tests

### Test Case 16: Dialog Transitions

**Objective**: Verify smooth dialog transitions

**Steps**:
1. Complete biometric flow, noticing each dialog
2. Go back and forth through dialogs

**Expected Results**:
- ✓ No freezing or lag
- ✓ Dialogs appear/disappear smoothly
- ✓ Back button works (cancel dialogs)
- ✓ Text fields/toggles respond immediately

---

### Test Case 17: State Management

**Objective**: Verify state doesn't leak between flows

**Steps**:
1. Start biometric group creation
2. Cancel midway
3. Start new group creation
4. Verify selections are fresh

**Expected Results**:
- ✓ Previous biometric selections not remembered
- ✓ Fresh toggles (all OFF)
- ✓ No app names from previous flow
- ✓ Can start fresh without issues

---

## Sign-off Checklist

- [ ] All 17 test cases passed
- [ ] Biometrics option correctly hidden/shown
- [ ] All backup types (PIN/Password/Pattern) work
- [ ] Mixed backup types per app works
- [ ] Skip backup option works
- [ ] Overlay permission works
- [ ] Groups appear in home screen
- [ ] Group settings show correct biometric config
- [ ] No crashes or exceptions
- [ ] Performance is acceptable
- [ ] Regular PIN/Password/Pattern flow unaffected
- [ ] Different groups can have different auth methods
- [ ] State properly managed between flows

---

## Known Limitations

- Biometrics are checked at setup time (device must have enrolled fingerprints)
- BiometricManager check requires API 28+
- Pattern entry uses simplified 3x3 grid (not full-featured)
- No Face ID vs Fingerprint distinction in UI (BiometricManager abstracts both)
- Backup credential types (PIN/Password/Pattern) are same as primary auth

## Notes for QA

- Test on both physical device and emulator
- Test on devices with and without fingerprint sensor
- Test with fingerprints enrolled and not enrolled
- Test on older Android versions (if supporting pre-API-28)
- Compare behavior with similar apps (Google Smart Lock, etc.)

