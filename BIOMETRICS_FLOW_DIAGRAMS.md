# Biometrics Flow Diagrams

## User Journey - Complete Flow

```
┌─────────────────────────────────────────────────────────────────┐
│               DALE GROUP CREATION - BIOMETRICS FLOW              │
└─────────────────────────────────────────────────────────────────┘

START: Group Creation
  │
  ├─ [1] App Selection Screen (AppSelectionActivity)
  │   └─> User selects 2 apps
  │
  ├─ [2] Group Name Screen
  │   └─> User names the group
  │
  └─ [3] Lock Authentication (PasswordSetupActivity) ⭐ NEW BIOMETRICS HERE
      │
      ├─────────────────────────────────────────────────────────┐
      │        Authentication Type Selection Screen            │
      ├─────────────────────────────────────────────────────────┤
      │  ┌──────────────┐  ┌──────────────┐  ┌────────────────┐ │
      │  │     PIN      │  │  PASSWORD    │  │    PATTERN     │ │
      │  │  (Enabled)   │  │  (Enabled)   │  │  (Enabled)     │ │
      │  └──────────────┘  └──────────────┘  └────────────────┘ │
      │  ┌────────────────────────────────────────────────────┐  │
      │  │   BIOMETRICS (Enabled if device supports it)   ⭐  │  │
      │  └────────────────────────────────────────────────────┘  │
      └─────────────────────────────────────────────────────────┘
      │
      ├─ User clicks PIN/PASSWORD/PATTERN
      │  │
      │  └─> REGULAR FLOW (existing)
      │      └─> Credential Entry (App 1 & 2)
      │          └─> Overlay Permission
      │              └─> Setup Complete ✓
      │
      └─ User clicks BIOMETRICS ⭐
         │
         ├─────────────────────────────────────────────────────────┐
         │  [4] Biometric Apps Selection Dialog                   │
         ├─────────────────────────────────────────────────────────┤
         │                                                         │
         │  Select which apps to protect:                          │
         │  ┌─ App 1: [OFF]  ↔  [ON]                           │
         │  ┌─ App 2: [OFF]  ↔  [ON]                           │
         │                                                         │
         │  [NEXT] (enabled only if ≥1 app selected)             │
         │  [CANCEL]                                              │
         └─────────────────────────────────────────────────────────┘
         │
         └─> User selects apps
            │
            ├─────────────────────────────────────────────────────────┐
            │  [5] Biometric Policy Dialog                           │
            ├─────────────────────────────────────────────────────────┤
            │                                                         │
            │  Choose authentication policy for each app:             │
            │                                                         │
            │  App 1:                                                │
            │  [OFF] Biometric Only  ↔  Biometric + Backup [ON]    │
            │                                                         │
            │  App 2:                                                │
            │  [OFF] Biometric Only  ↔  Biometric + Backup [ON]    │
            │                                                         │
            │  [NEXT]                                                │
            │  [CANCEL]                                              │
            └─────────────────────────────────────────────────────────┘
            │
            └─> User chooses policies
               │
               ├─ ALL apps set to "Biometric Only"?
               │  │
               │  └─> YES: Save biometric settings ✓
               │       │
               │       └─> Skip to Overlay Permission
               │
               └─ ANY app set to "Biometric + Backup"?
                  │
                  └─> YES: Show backup credential dialogs
                     │
                     ├─ [6a] Backup Dialog for App 1
                     │  │
                     │  ├────────────────────────────────────┐
                     │  │  Backup Type Selection             │
                     │  ├────────────────────────────────────┤
                     │  │ ┌─────────────────────────────────┐│
                     │  │ │ PIN: 4 digit PIN           [PIN]││
                     │  │ └─────────────────────────────────┘│
                     │  │ ┌─────────────────────────────────┐│
                     │  │ │ PASSWORD: Alphanumeric  [PWD]   ││
                     │  │ └─────────────────────────────────┘│
                     │  │ ┌─────────────────────────────────┐│
                     │  │ │ PATTERN: Draw pattern   [PAT]   ││
                     │  │ └─────────────────────────────────┘│
                     │  │                                     │
                     │  │ [SKIP BACKUP]                      │
                     │  └────────────────────────────────────┘
                     │  │
                     │  └─> User selects backup type
                     │     │
                     │     ├─ [6b] Credential Entry Screen
                     │     │  │
                     │     │  ├─ "Enter [BACKUP_TYPE] for App 1"
                     │     │  ├─ Step 1 of 2: Enter credential
                     │     │  ├─ User types credential
                     │     │  ├─ [NEXT]
                     │     │  │
                     │     │  └─> Step 2 of 2: Confirm credential
                     │     │     ├─ User re-enters credential
                     │     │     ├─ [CONFIRM]
                     │     │     │
                     │     │     └─> Credential saved ✓
                     │     │
                     │     └─> Check if App 2 needs backup
                     │        │
                     │        ├─ YES: Show Backup Dialog for App 2
                     │        │   │
                     │        │   └─> [Repeat 6a-6b for App 2]
                     │        │       │
                     │        │       └─> Save all biometric ✓
                     │        │
                     │        └─ NO: Save all biometric ✓
                     │
                     └─> [7] Overlay Permission Dialog
                        │
                        ├──────────────────────────────────────┐
                        │ Enable 'Display over other apps'?   │
                        │                                      │
                        │ DALE needs this permission to       │
                        │ show the lock screen overlay        │
                        │                                      │
                        │ [OPEN OVERLAY SETTINGS]  [CANCEL]   │
                        └──────────────────────────────────────┘
                        │
                        └─> [8] Setup Complete ✓
                           │
                           └─> Group appears in Home Screen
                              ┌──────────────────────────────┐
                              │ Group Name                   │
                              │ App 1 + App 2                │
                              │ 🔒 Locked (Biometric)        │
                              └──────────────────────────────┘
```

## State Machine Diagram

```
┌────────────────────────────────────────────────────────────┐
│         PASSWORDSETUPSCREEN STATE MACHINE                  │
└────────────────────────────────────────────────────────────┘

                    START
                      │
                      ▼
        ┌─────────────────────────┐
        │ selectedAuthType = null │
        │ Show Auth Selection     │
        └─────────────────────────┘
                      │
              ┌───────┼───────┐
              │               │
         [PIN]          [BIOMETRICS]
              │               │
              │               ▼
              │    ┌──────────────────────────┐
              │    │ showBiometricAppsDialog  │
              │    │ Ask which apps (1, 2)    │
              │    └──────────────────────────┘
              │               │
              │               ▼
              │    ┌──────────────────────────┐
              │    │ showBiometricBackupDialog│
              │    │ Choose policy per app    │
              │    └──────────────────────────┘
              │               │
              │         ┌─────┴──────┐
              │         │            │
              │    [Only]        [+Backup]
              │         │            │
              │         │            ▼
              │         │ ┌────────────────────────┐
              │         │ │showBiometricBackupPin  │
              │         │ │Select backup type      │
              │         │ └────────────────────────┘
              │         │            │
              │         │            ▼
              │         │ ┌────────────────────────┐
              │         │ │SelectedAuthType=       │
              │         │ │ "PIN"/"PASSWORD"/      │
              │         │ │ "PATTERN"              │
              │         │ └────────────────────────┘
              │         │
              ▼         │
       ┌──────────────┐ │
       │selectedAuth  │ │
       │Type set      │ │
       │              │ │
       │Show Cred.    │ │
       │Entry Screen  │ │
       └──────────────┘─┘
              │
              ▼
    ┌──────────────────────┐
    │ Credential Confirmed │
    │                      │
    │ Save to AppGroup     │
    └──────────────────────┘
              │
              ▼
    ┌──────────────────────┐
    │ More apps needed?    │
    └──────────────────────┘
         │          │
        NO         YES
         │          │
         ▼          ▼
    ┌───────┐  ┌─────────────────┐
    │       │  │ targetAppIndex++│
    │       │  │ Show Cred Screen│
    │       │  └─────────────────┘
    │       │          │
    │       └──────────┘
    │
    ▼
┌─────────────────────┐
│Show Overlay Dialog  │
└─────────────────────┘
    │
    ▼
┌─────────────────────┐
│ Setup Complete ✓    │
│ Navigate to Home    │
└─────────────────────┘
```

## Data Flow Diagram

```
┌──────────────────────────────────────────────────────────┐
│              BIOMETRIC DATA SAVE FLOW                     │
└──────────────────────────────────────────────────────────┘

USER INPUT (UI)
│
├─ BiometricAppsSelectionDialog
│  └─> app1BiometricEnabled = true/false
│  └─> app2BiometricEnabled = true/false
│
├─ BiometricPolicyDialog
│  └─> app1BiometricOnly = true/false
│  └─> app2BiometricOnly = true/false
│
├─ BiometricBackupCredentialDialog
│  └─> app1BackupType = "PIN"/"PASSWORD"/"PATTERN"
│
├─ CredentialEntryScreen (for backup)
│  └─> app1BackupPin = rawCredential
│      └─> Call: saveCredentialForApp()
│
└─> Call: saveBiometricForApps()
   │
   ▼
┌─────────────────────────────────────────┐
│ saveBiometricForApps()                  │
├─────────────────────────────────────────┤
│                                         │
│ FOR EACH APP:                           │
│  If enabled && !biometricOnly:          │
│    └─> Hash backup credential          │
│  Else if enabled && biometricOnly:      │
│    └─> Clear backup (keep as empty)     │
│  Set lockType = "BIOMETRIC"             │
│                                         │
└─────────────────────────────────────────┘
   │
   ▼
┌─────────────────────────────────────────┐
│ AppGroup Updated                        │
├─────────────────────────────────────────┤
│                                         │
│ app1FingerprintEnabled: true/false      │
│ app1FingerprintBiometricOnly: true/false│
│ app1LockType: "BIOMETRIC"               │
│ app1LockPin: hashed_credential (or "")  │
│                                         │
│ app2FingerprintEnabled: true/false      │
│ app2FingerprintBiometricOnly: true/false│
│ app2LockType: "BIOMETRIC"               │
│ app2LockPin: hashed_credential (or "")  │
│                                         │
│ isLocked: true                          │
│                                         │
└─────────────────────────────────────────┘
   │
   ▼
┌─────────────────────────────────────────┐
│ SharedPreferencesManager.saveAppGroup() │
└─────────────────────────────────────────┘
   │
   ▼
┌─────────────────────────────────────────┐
│ Persistent Storage                      │
│ (SharedPreferences JSON)                │
└─────────────────────────────────────────┘
```

## Capability Detection Flow

```
┌──────────────────────────────────────────────────────┐
│      BIOMETRIC CAPABILITY DETECTION                  │
└──────────────────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│ Check: hasSystemFeature(FEATURE_FINGER) │
└─────────────────────────────────────────┘
              │
       ┌──────┴──────┐
      NO            YES
       │              │
       ▼              ▼
   ┌─────┐  ┌──────────────────────┐
   │OFF  │  │ BiometricManager.     │
   └─────┘  │ canAuthenticate()     │
            └──────────────────────┘
                     │
              ┌──────┴──────┐
         FAILURE           SUCCESS
            │                  │
            ▼                  ▼
         ┌─────┐            ┌─────┐
         │OFF  │            │ON ✓ │
         └─────┘            └─────┘
            │                  │
            ▼                  ▼
    BIOMETRICS          BIOMETRICS
    DISABLED            ENABLED ✓
    (Greyed out)        (Clickable)
```

## Dialog Sequence Diagram

```
┌──────────────────────────────────────────────────────────┐
│        DIALOG SEQUENCE - BIOMETRIC SETUP                 │
└──────────────────────────────────────────────────────────┘

Time
  │
  ▼
  1  ┌─────────────────────────────────────────┐
     │ Lock Authentication Screen              │
     │ (Auth Type Selection)                   │
     └─────────────────────────────────────────┘
        │                          │
        │ User clicks BIOMETRICS   │
        │                          │
        ▼                          │ (or PIN/PASSWORD/PATTERN)
        │                          │
        │                          ▼ (Old flow unchanged)
        │
  2  ┌──────────────────────────────┐
     │ BiometricAppsSelectionDialog │
     │ • Toggle App 1               │
     │ • Toggle App 2               │
     │ • [NEXT] or [CANCEL]        │
     └──────────────────────────────┘
        │ [NEXT]
        │
  3  ┌──────────────────────────────┐
     │ BiometricPolicyDialog        │
     │ For each selected app:       │
     │ • Toggle Biometric Only ↔    │
     │   Biometric + Backup         │
     │ • [NEXT] or [CANCEL]        │
     └──────────────────────────────┘
        │ [NEXT]
        │
        ├──────────────────┐
        │ Need backup?     │
        ├──────────────────┤
        │     │            │
       YES   NO            │
        │     │            │
        │     └────────────┼──────────┐
        │                  │          │
  4  ┌──────────────────────┐          │
     │ BiometricBackup      │          │
     │ CredentialDialog     │          │
     │ (App 1)              │          │
     │ • PIN                │          │
     │ • PASSWORD           │          │
     │ • PATTERN            │          │
     └──────────────────────┘          │
        │ [PIN/PASSWORD/PATTERN]       │
        │                              │
  5  ┌──────────────────────┐          │
     │ CredentialEntry      │          │
     │ Screen (Backup)      │          │
     │ • Step 1: Enter      │          │
     │ • Step 2: Confirm    │          │
     └──────────────────────┘          │
        │ [CONFIRM]                    │
        │                              │
        ├─ More apps? ─────────────┐   │
        │     │                   │   │
       YES   NO                   │   │
        │     │                   │   │
        │  ┌──┴──────────────────┘   │
        │  │                         │
  4B └──┼──► BiometricBackup        │
           CredentialDialog          │
           (App 2)                   │
           ...repeat...              │
                                    │
                                ┌───┘
                                │
  6                          ┌─────────────────────┐
     ┌──────────────────────►│ Overlay Permission  │
     │                       │ Dialog              │
     │                       │ [OPEN SETTINGS] or  │
     │                       │ [CANCEL]            │
     │                       └─────────────────────┘
     │                          │ [OPEN SETTINGS]
     └──────────────────────────┤
                                │
  7                          ┌─────────────────────┐
     ┌──────────────────────►│ Setup Complete      │
     │                       │ (Navigate to Home)  │
     │                       └─────────────────────┘
     │
  (end)
```

## Configuration Examples

### Example 1: Biometric Only (Both Apps)
```
User Flow:
  1. Select BIOMETRICS
  2. Enable: App 1 ✓, App 2 ✓
  3. Policy: Both set to "Biometric Only"
  4. Complete (no backup needed)

Result:
  app1FingerprintEnabled: true
  app1FingerprintBiometricOnly: true
  app1LockType: "BIOMETRIC"
  app1LockPin: ""          ◄── No backup
  
  app2FingerprintEnabled: true
  app2FingerprintBiometricOnly: true
  app2LockType: "BIOMETRIC"
  app2LockPin: ""          ◄── No backup
```

### Example 2: Biometric + PIN (Both Apps)
```
User Flow:
  1. Select BIOMETRICS
  2. Enable: App 1 ✓, App 2 ✓
  3. Policy: Both set to "Biometric + Backup"
  4. Backup Type: Both select PIN
  5. Enter PIN for App 1: 1234
  6. Enter PIN for App 2: 5678
  7. Complete

Result:
  app1FingerprintEnabled: true
  app1FingerprintBiometricOnly: false
  app1LockType: "BIOMETRIC"
  app1LockPin: "abc123..."  ◄── Hashed 1234
  
  app2FingerprintEnabled: true
  app2FingerprintBiometricOnly: false
  app2LockType: "BIOMETRIC"
  app2LockPin: "def456..."  ◄── Hashed 5678
```

### Example 3: Mixed (Different Backups)
```
User Flow:
  1. Select BIOMETRICS
  2. Enable: App 1 ✓, App 2 ✗
  3. Policy: App 1 "Biometric + Backup"
  4. Backup Type: App 1 selects PASSWORD
  5. Enter Password: MyPassword123
  6. Complete

Result:
  app1FingerprintEnabled: true
  app1FingerprintBiometricOnly: false
  app1LockType: "BIOMETRIC"
  app1LockPin: "ghi789..."  ◄── Hashed password
  
  app2FingerprintEnabled: false
  app2FingerprintBiometricOnly: false
  app2LockType: "PIN"        ◄── (unchanged, not using biometric)
  app2LockPin: ""            ◄── (not set yet for this flow)
```

## Lock Screen Behavior

```
┌────────────────────────────────────────────┐
│     WHEN USER TRIES TO UNLOCK APP          │
└────────────────────────────────────────────┘

Check: app1FingerprintEnabled?
       │
    YES╞══════════════════════════════════┐
    NO │                                  │
       │                                  ▼
       │                          ┌──────────────┐
       │                          │ Show PIN/PWD │
       │                          │ Entry Screen │
       │                          └──────────────┘
       │
       ▼
┌────────────────┐
│ Show Biometric│
│ Prompt         │
└────────────────┘
       │
    ┌──┴────────────────────────┐
    │                           │
 SUCCESS                    FAILURE
    │                           │
    ▼                           ▼
┌─────────┐         Check: app1FingerprintBiometricOnly?
│ UNLOCK! │                 │
└─────────┘            YES  │  NO
                        │    │
                        │    ▼
                        │  ┌──────────────────┐
                        │  │ Show Backup      │
                        │  │ (PIN/PWD/PAT)    │
                        │  └──────────────────┘
                        │    │
                        │    ├──┬──────────┐
                        │    │  │          │
                   LOCKED  SUCCESS   FAILURE
                    ├─────►│         │
                    │      ▼         ▼
                    │   ┌──────┐  ┌──────┐
                    │   │UNLOCK│  │LOCKED│
                    │   └──────┘  └──────┘
                    │
                    ▼
            ┌──────────────┐
            │   LOCKED!    │
            │ (Max retries │
            │   exceeded)  │
            └──────────────┘
```

---

These diagrams provide complete visual understanding of:
- ✅ User journey through biometric setup
- ✅ State transitions
- ✅ Data flow and storage
- ✅ Device capability detection
- ✅ Dialog sequences
- ✅ Configuration examples
- ✅ Lock screen behavior

