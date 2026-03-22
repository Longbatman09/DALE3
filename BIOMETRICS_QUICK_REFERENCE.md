# Biometrics Implementation - Developer Quick Reference

## Quick Overview

**What**: Biometric (fingerprint/face ID) authentication in group creation
**Where**: `PasswordSetupActivity.kt`
**When**: During group creation, after selecting authentication method
**Why**: Secure device-level biometric authentication with optional backup credentials

## Key Classes & Composables

### Main Components

| Component | Purpose | Location |
|-----------|---------|----------|
| `PasswordSetupScreen` | Main composable, handles all auth flows | PasswordSetupActivity.kt:183-445 |
| `BiometricAppsSelectionDialog` | Select which apps get biometrics | PasswordSetupActivity.kt:972-1029 |
| `BiometricPolicyDialog` | Choose biometric-only or +backup | PasswordSetupActivity.kt:1031-1090 |
| `BiometricPolicyRow` | Single app policy row | PasswordSetupActivity.kt:1092-1124 |
| `BiometricBackupCredentialDialog` | Select backup type (PIN/PWD/PAT) | PasswordSetupActivity.kt:1126-1165 |
| `AuthenticationTypeSelection` | Lists all auth methods | PasswordSetupActivity.kt:699-723 |
| `CredentialEntryScreen` | PIN/Password/Pattern entry | PasswordSetupActivity.kt:738-905 |

## Key Methods

### In PasswordSetupActivity Class

```kotlin
// Check if biometric is available
val isBiometricAvailable = BiometricManager.from(activity)
    .canAuthenticate(BIOMETRIC_WEAK or BIOMETRIC_STRONG) 
    == BIOMETRIC_SUCCESS

// Save biometric settings for an app
fun saveBiometricForApps(
    groupId: String,
    app1Enabled: Boolean,
    app2Enabled: Boolean,
    app1BiometricOnly: Boolean,
    app2BiometricOnly: Boolean,
    ...
)

// Save backup credential
fun saveCredentialForApp(
    groupId: String,
    appIndex: Int,
    authType: String,
    rawCredential: String
)
```

## State Management

### Core State Variables

```kotlin
// Which auth type selected (null = selection screen, "BIOMETRICS", "PIN", etc.)
val selectedAuthType = remember { mutableStateOf<String?>(null) }

// Biometric flow dialogs
val showBiometricAppsDialog = remember { mutableStateOf(false) }
val showBiometricBackupDialog = remember { mutableStateOf(false) }
val showBiometricBackupPinDialog = remember { mutableStateOf<Int?>(null) }

// Biometric settings
val app1BiometricEnabled = remember { mutableStateOf(false) }
val app1BiometricOnly = remember { mutableStateOf(true) }  // false = needs backup
```

### Device Capability Detection

```kotlin
val hasFingerprintSensor = remember {
    activity.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
}

val isBiometricAvailable = remember {
    if (!hasFingerprintSensor) false
    else BiometricManager.from(activity).canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_WEAK or BIOMETRIC_STRONG
    ) == BiometricManager.BIOMETRIC_SUCCESS
}
```

## Data Model

### AppGroup Biometric Fields

```kotlin
// Biometric enabled for each app
app1FingerprintEnabled: Boolean
app2FingerprintEnabled: Boolean

// Biometric policy: true = only, false = + backup
app1FingerprintBiometricOnly: Boolean
app2FingerprintBiometricOnly: Boolean

// Lock type
app1LockType: String  // "BIOMETRIC", "PIN", "PASSWORD", "PATTERN"
app2LockType: String

// Backup credential (SHA-256 hashed)
app1LockPin: String   // Empty if biometric-only
app2LockPin: String
```

## Flow Diagram

```
AuthenticationTypeSelection
    │
    ├─ Non-Biometric
    │  └─> CredentialEntryScreen → Overlay → Complete
    │
    └─ BIOMETRICS
       └─> BiometricAppsSelectionDialog
           └─> BiometricPolicyDialog
               └─> Check if backup needed
                   ├─ No: Overlay → Complete
                   └─ Yes: BiometricBackupCredentialDialog
                       └─> CredentialEntryScreen (backup)
                           └─> Check for App 2 backup
                               ├─ No: Overlay → Complete
                               └─ Yes: BiometricBackupCredentialDialog (App 2)
                                   └─> CredentialEntryScreen
                                       └─> Overlay → Complete
```

## Dialog Progression

### User Selects BIOMETRICS

```kotlin
if (authType == "BIOMETRICS") {
    showBiometricAppsDialog.value = true
}
```

### Step 1: Apps Selection

```kotlin
BiometricAppsSelectionDialog(
    app1Enabled = app1BiometricEnabled.value,
    app2Enabled = app2BiometricEnabled.value,
    onConfirm = {
        showBiometricAppsDialog.value = false
        showBiometricBackupDialog.value = true  // Next dialog
    }
)
```

### Step 2: Policy Selection

```kotlin
BiometricPolicyDialog(
    app1BiometricOnly = app1BiometricOnly.value,
    app2BiometricOnly = app2BiometricOnly.value,
    onConfirm = {
        if ((app1BiometricEnabled && !app1BiometricOnly) || 
            (app2BiometricEnabled && !app2BiometricOnly)) {
            showBiometricBackupPinDialog.value = 1  // App 1 backup
        } else {
            saveBiometricForApps(...)  // No backup needed
            showOverlayDialog.value = true
        }
    }
)
```

### Step 3: Backup Type Selection

```kotlin
BiometricBackupCredentialDialog(
    appName = app1Name.value,
    onBackupTypeSelected = { backupType ->
        app1BackupType.value = backupType
        selectedAuthType.value = backupType  // PIN/PASSWORD/PATTERN
        targetAppIndex.value = 1
        // CredentialEntryScreen will be shown
    }
)
```

### Step 4: Backup Credential Entry

Same as regular PIN/Password/Pattern entry, but:
- Updates `app1BackupPin` instead of core PIN
- After confirmed, continues to next app if needed

## Saving Data Flow

### For Regular Authentication (PIN/Password/Pattern)

```
User enters credential
    ↓
saveCredentialForApp(groupId, appIndex, authType, rawCredential)
    ↓
AppGroup updated with:
  - app[X]LockPin = hashPin(rawCredential)
  - app[X]LockType = authType
  - isLocked = true
```

### For Biometric with Backup

```
User enters backup credential
    ↓
saveCredentialForApp(groupId, appIndex, backupType, rawCredential)
    ↓
AppGroup updated with backup data
    ↓
All apps processed, then saveBiometricForApps() called
    ↓
AppGroup updated with:
  - app[X]FingerprintEnabled = true/false
  - app[X]FingerprintBiometricOnly = true/false
  - app[X]LockType = "BIOMETRIC" (if enabled)
  - app[X]LockPin = hashed backup (if backup+enabled)
```

## Key Functions to Call

### Enable Biometric

```kotlin
(activity as? PasswordSetupActivity)?.saveBiometricForApps(
    groupId = groupId,
    app1Enabled = app1BiometricEnabled.value,
    app2Enabled = app2BiometricEnabled.value,
    app1BiometricOnly = app1BiometricOnly.value,
    app2BiometricOnly = app2BiometricOnly.value
)
```

### Save Backup Credential

```kotlin
(activity as? PasswordSetupActivity)?.saveCredentialForApp(
    groupId = groupId,
    appIndex = targetAppIndex.value,
    authType = authType,
    rawCredential = credential
)
```

### Check Biometric Availability

```kotlin
val available = BiometricManager.from(activity).canAuthenticate(
    BiometricManager.Authenticators.BIOMETRIC_WEAK or BIOMETRIC_STRONG
) == BiometricManager.BIOMETRIC_SUCCESS
```

## Common Issues & Solutions

### Biometrics Option Disabled

**Symptom**: BIOMETRICS option greyed out

**Check**:
```kotlin
val hasSensor = activity.packageManager
    .hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
val enrolled = BiometricManager.from(activity).canAuthenticate(...) == SUCCESS
```

**Solution**: 
- Check device has fingerprint sensor
- Check at least one fingerprint enrolled
- Check API 28+

### Backup Not Saving

**Symptom**: Biometric enabled but backup lost

**Check**:
- Did `saveCredentialForApp()` get called?
- Is credential being hashed?
- Is `saveBiometricForApps()` overwriting the pin?

**Solution**:
- Ensure `saveCredentialForApp()` called before `saveBiometricForApps()`
- In `saveBiometricForApps()`, use `appGroup.app1LockPin` as fallback

### Wrong Policy Shown in Settings

**Symptom**: Settings show different policy than selected

**Check**:
- Is `app1FingerprintBiometricOnly` correctly set?
- Is it being overwritten somewhere?

**Solution**:
```kotlin
app1FingerprintBiometricOnly = app1Enabled && app1BiometricOnly
```

## Testing Checklist

- [ ] Biometrics option appears only on capable devices
- [ ] Can select apps (1 or 2)
- [ ] Can select policies (only or +backup)
- [ ] Can skip backup
- [ ] Can select different backup types
- [ ] Backup credentials properly hashed
- [ ] AppGroup saved correctly
- [ ] Different per-app settings respected
- [ ] Home screen shows locked group

## Integration with Existing Code

### No Changes Needed

- ✅ `SharedPreferencesManager` - Already supports biometric fields
- ✅ `AppGroup` - Already has biometric properties
- ✅ `GroupSettingsActivity` - Already shows fingerprint settings
- ✅ `FingerprintSelectionDialog` - Already handles biometric display
- ✅ Lock screen authentication - Already checks biometric flags

### Just Added

- ✨ Biometric flow in `PasswordSetupActivity`
- ✨ Dialog composables for user selection
- ✨ Capability detection with `BiometricManager`

## Performance Notes

- ✅ Biometric detection is quick (device feature check)
- ✅ Dialog transitions are smooth
- ✅ State management is efficient
- ✅ No blocking calls during UI transitions

## Security Notes

- ✅ Credentials hashed with SHA-256
- ✅ Hashed values stored in SharedPreferences
- ✅ Biometric flags prevent plaintext storage
- ✅ Backup credentials only stored if backup enabled
- ✅ Device-level biometric (OS handles security)

## Debugging Tips

```kotlin
// Log biometric availability
Log.d("Biometric", "Sensor: $hasFingerprintSensor, Available: $isBiometricAvailable")

// Log biometric settings saved
val group = sharedPrefs.getAppGroup(groupId)
Log.d("Biometric", "App1: ${group.app1FingerprintEnabled}, " + 
      "BiometricOnly: ${group.app1FingerprintBiometricOnly}")

// Log credential hashing
val hashed = hashPin("1234")
Log.d("Biometric", "Hashed: $hashed")
```

## Quick Copy-Paste Code Snippets

### Check Biometric Availability

```kotlin
val canUseBiometric = remember {
    val hasSensor = activity.packageManager
        .hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
    hasSensor && BiometricManager.from(activity).canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_WEAK or 
        BiometricManager.Authenticators.BIOMETRIC_STRONG
    ) == BiometricManager.BIOMETRIC_SUCCESS
}
```

### Save Biometric Settings

```kotlin
val group = appGroup.value?.copy(
    app1FingerprintEnabled = true,
    app1FingerprintBiometricOnly = false,
    app1LockType = "BIOMETRIC",
    app1LockPin = hashPin(backupPin)
) ?: return
sharedPrefs.saveAppGroup(group)
```

### Read Biometric Settings

```kotlin
val isBiometricEnabled = group.app1FingerprintEnabled
val bioOnlyPolicy = group.app1FingerprintBiometricOnly
val hasBackup = group.app1LockPin.isNotEmpty()
```

## References

- `PasswordSetupActivity.kt` - Full implementation
- `GroupSettingsActivity.kt` - How biometric is displayed
- `BIOMETRICS_IMPLEMENTATION.md` - Detailed documentation
- `BIOMETRICS_TESTING_GUIDE.md` - Test cases

