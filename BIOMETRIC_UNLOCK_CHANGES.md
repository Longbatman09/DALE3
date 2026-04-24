# Biometric Unlock System - Changes Summary

## Overview
Updated the fingerprint/biometric unlock feature to enforce a strict single-app policy with mandatory backup authentication. Only ONE app per group can be enabled with biometric unlock.

## Key Changes

### 1. **GroupSettingsActivity.kt**

#### FingerprintSelectionDialog - Complete Redesign
- **Old Behavior**: Both apps could be enabled independently with optional "Biometric only" vs "Biometric + PIN fallback" toggle
- **New Behavior**: 
  - Only ONE app can be enabled at a time (mutually exclusive)
  - Clicking one app automatically disables the other
  - No "Biometric only" option - always includes backup method
  - Policy displayed: `<Auth Type> + Biometric` for the selected app

#### Removed Components
- `FingerprintPolicyRow` composable (no longer needed)
- "Biometric only" toggle functionality

#### Updated AppSelectionDialog
- Shows backup method for apps with biometric enabled
- Display format: "Biometric + PIN Backup" (or other auth type)

### 2. **PasswordSetupActivity.kt**

#### BiometricAppsSelectionDialog - Mutually Exclusive Selection
- Changed from independent toggles to radio-button-like behavior
- Only one app can be enabled at any time
- Enabling one app automatically disables the other
- Policy guidance text added

#### Removed Components
- `BiometricPolicyDialog` composable (entire biometric policy selection removed)
- `BiometricPolicyRow` composable

#### Updated Biometric Flow Logic
- Skips the policy dialog step entirely
- Both apps now require credentials during setup:
  - Selected app: Gets biometric + backup credential
  - Other app: Gets regular credential
- Updated `PasswordSetupScreen` to handle new flow

#### Updated saveBiometricForApps Function
```kotlin
- Always uses backup credentials (never biometric-only)
- Sets lock type to backup type (PIN/PASSWORD/PATTERN)
- Always sets app1FingerprintBiometricOnly = false
- Always sets app2FingerprintBiometricOnly = false
```

### 3. **DrawOverOtherAppsLockScreen.kt** (No Changes Required)
- Existing logic remains compatible
- `isBiometricOnlyForTarget()` always returns false (correct behavior)
- Fallback button always shown (correct for new policy)

## Data Model Impact

### AppGroup Data Class
No changes to fields, but their usage is simplified:
- `app1FingerprintBiometricOnly`: Always false
- `app2FingerprintBiometricOnly`: Always false
- `app1FingerprintEnabled`: Only one can be true at a time
- `app2FingerprintEnabled`: Only one can be true at a time

## User Experience Changes

### Before
```
Biometric Unlock Dialog
├─ App 1: [Toggle] → When enabled: [Biometric only / Biometric + PIN] toggle
└─ App 2: [Toggle] → When enabled: [Biometric only / Biometric + PIN] toggle
```

### After
```
Biometric Unlock Dialog
├─ App 1: [Switch] ← Only one can be ON
└─ App 2: [Switch] ← Enabling one disables the other
   + Policy: PIN + Biometric (for selected app)
```

## Security Improvements
1. Simplified, clear authentication policy
2. Always provides fallback authentication method
3. Prevents confusion about multi-app biometric setups
4. Enforces consistent lock type across group

## Build Status
✅ BUILD SUCCESSFUL - No compilation errors
- All Kotlin changes comply with Android API requirements
- No breaking changes to existing data structures
- Backward compatible with existing app groups (fields still exist)

## Testing Recommendations
1. Create a new group and enable biometric for each app individually
2. Verify only one app can have biometric enabled
3. Verify credentials are properly set for both apps
4. Test biometric authentication with fallback
5. Verify lock screen shows backup method option
6. Test pattern and password auth types with biometric

## Future Enhancements
- Could add per-app biometric enrollment (currently uses device biometrics)
- Could add biometric strength indicators
- Could add revocation of biometric for already-created groups

