# Biometrics Implementation in Group Creation

## Overview
This document describes the implementation of biometric authentication in the DALE app's group creation flow.

## Changes Made

### 1. **PasswordSetupActivity.kt**

#### Imports Added
- `androidx.biometric.BiometricManager` - For biometric availability checking
- `android.content.pm.PackageManager` - For feature detection
- `androidx.compose.material3.Switch` - For UI toggles

#### New Functions

##### `saveBiometricForApps()`
Saves biometric settings for both apps including:
- Biometric enabled/disabled state for each app
- Biometric-only or biometric+backup policy
- Backup authentication type and credentials

```kotlin
fun saveBiometricForApps(
    groupId: String,
    app1Enabled: Boolean,
    app2Enabled: Boolean,
    app1BiometricOnly: Boolean,
    app2BiometricOnly: Boolean,
    app1BackupType: String = "PIN",
    app2BackupType: String = "PIN",
    app1BackupPin: String = "",
    app2BackupPin: String = ""
)
```

#### Updated PasswordSetupScreen Composable
Enhanced with biometric flow management:

**New State Variables:**
- `showBiometricAppsDialog` - Controls app selection dialog
- `showBiometricBackupDialog` - Controls policy selection dialog
- `showBiometricBackupPinDialog` - Controls backup credential dialogs
- `app1BiometricEnabled` / `app2BiometricEnabled` - Track which apps have biometrics
- `app1BiometricOnly` / `app2BiometricOnly` - Track policy preference
- Biometric availability detection variables

**Features:**
- Detects biometric sensor availability on device
- Checks if biometrics are enrolled using BiometricManager
- Only enables BIOMETRICS option if device supports it

#### Updated AuthenticationTypeSelection
- Added `isBiometricAvailable` parameter
- Dynamically enables/disables BIOMETRICS option based on device capability
- Shows all four auth types: PIN, PASSWORD, PATTERN, BIOMETRICS

### 2. **New Dialog Composables**

#### BiometricAppsSelectionDialog
Allows user to select which apps (1, 2, or both) to protect with biometric.
- Shows toggles for app1 and app2
- Prevents confirmation if no app is selected
- Explains biometric approach

#### BiometricPolicyDialog
Lets user choose authentication policy for each enabled app:
- **Biometric Only**: Just fingerprint/face ID
- **Biometric + Backup**: Fingerprint + fallback (PIN/Password/Pattern)

#### BiometricPolicyRow
Reusable component for policy configuration of a single app.

#### BiometricBackupCredentialDialog
Prompts user to select backup authentication type:
- PIN (4 digits)
- PASSWORD (alphanumeric, min 6 chars)
- PATTERN (gesture pattern, 4+ dots)

Includes "Skip Backup" option for biometric-only setup.

### 3. **Flow Integration**

#### Group Creation Flow (Biometrics Path)

```
1. Authentication Type Selection Screen
   ↓
2. User selects "BIOMETRICS" 
   ↓
3. BiometricAppsSelectionDialog
   - Select which app(s) to enable
   ↓
4. BiometricPolicyDialog  
   - Choose "Biometric Only" or "Biometric + Backup" per app
   ↓
5. (If Biometric + Backup selected)
   BiometricBackupCredentialDialog (App 1)
   - Select backup type
   ↓
6. CredentialEntryScreen
   - Enter backup credential
   ↓
7. (If both apps need backup)
   Repeat steps 5-6 for App 2
   ↓
8. Overlay Permission Confirmation
   ↓
9. Complete Setup
```

## AppGroup Model Updates

The existing AppGroup data class already supports biometric settings:

```kotlin
data class AppGroup(
    // ... existing fields ...
    val app1FingerprintEnabled: Boolean = false,
    val app2FingerprintEnabled: Boolean = false,
    val app1FingerprintBiometricOnly: Boolean = false,
    val app2FingerprintBiometricOnly: Boolean = false,
    val app1LockType: String = "PIN",     // Can be "BIOMETRIC"
    val app2LockType: String = "PIN",     // Can be "BIOMETRIC"
    val app1LockPin: String = "",         // Stores hashed backup credential
    val app2LockPin: String = ""          // Stores hashed backup credential
)
```

## Biometric Availability Detection

The implementation checks:
1. **Fingerprint Sensor**: `PackageManager.FEATURE_FINGERPRINT`
2. **Biometric Enrollment**: `BiometricManager.canAuthenticate()`

```kotlin
val isBiometricAvailable = remember {
    if (!hasFingerprintSensor) {
        false
    } else {
        BiometricManager.from(activity).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.BIOMETRIC_STRONG
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }
}
```

## User Flow Scenarios

### Scenario 1: Biometric Only (No Backup)
1. Select Biometrics
2. Enable for desired app(s)
3. Choose "Biometric Only" policy
4. Complete overlay permission
5. Done!

**Result**: App locked with biometric only, no fallback.

### Scenario 2: Biometric with PIN Fallback
1. Select Biometrics
2. Enable for desired app(s)
3. Choose "Biometric + Backup" policy
4. Select PIN as backup
5. Enter PIN for each app
6. Complete overlay permission
7. Done!

**Result**: App uses biometric, with PIN fallback if biometric fails.

### Scenario 3: Mixed (Both Apps, Different Policies)
1. Select Biometrics
2. Enable both App 1 and App 2
3. For App 1: Choose "Biometric Only"
4. For App 2: Choose "Biometric + Backup"
5. Select Password as backup for App 2
6. Enter password
7. Complete overlay permission
8. Done!

**Result**: App 1 has biometric only, App 2 has biometric + password backup.

## Device Compatibility

- **Minimum SDK**: Works with devices supporting BiometricManager (API 28+)
- **Fingerprint Sensors**: Automatically disabled if not available
- **No Enrolled Biometrics**: Option disabled if no fingerprints/face enrolled

## Testing Checklist

- [ ] Biometrics option only appears if device has sensor + enrolled biometrics
- [ ] Can enable/disable per-app biometric
- [ ] Can select biometric-only or biometric+backup
- [ ] Can select different backup types (PIN/Password/Pattern)
- [ ] Backup credentials are properly hashed
- [ ] AppGroup saved with correct lock types and biometric flags
- [ ] Different apps can have different biometric policies
- [ ] Works with both biometric-only and biometric+backup flows
- [ ] Overlay permission dialog appears after biometric setup
- [ ] Group appears in home screen after creation

## Technical Notes

1. **Hashing**: All backup credentials are hashed with SHA-256 before saving
2. **Per-App Configuration**: Each app maintains separate biometric and backup settings
3. **Lock Type**: Set to "BIOMETRIC" when biometrics enabled
4. **Backward Compatibility**: Existing PIN/Password/Pattern flow unchanged
5. **State Management**: Uses Compose remember for dialog state management

## Future Enhancements

- [ ] Support for Face ID
- [ ] Biometric re-enrollment in group settings
- [ ] Advanced biometric policies (e.g., fingerprint + PIN combo)
- [ ] Biometric fallback timeout configuration
- [ ] Biometric usage statistics and logs

