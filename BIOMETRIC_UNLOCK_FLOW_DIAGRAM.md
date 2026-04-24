# Biometric Unlock - New Implementation Flow

## Group Creation Flow (with Biometric)

```
┌─────────────────────────────────────────────────────────────┐
│ Auth Type Selection                                         │
├─────────────────────────────────────────────────────────────┤
│ [PIN]      [PASSWORD]      [PATTERN]                        │
└─────────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│ Credential Entry (PIN/Password/Pattern)                     │
├─────────────────────────────────────────────────────────────┤
│ Enter App 1 Credential                                      │
│ Confirm App 1 Credential                                    │
└─────────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│ Credential Entry for Second App                             │
├─────────────────────────────────────────────────────────────┤
│ Enter App 2 Credential                                      │
│ Confirm App 2 Credential                                    │
└─────────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│ Biometric Unlock Selection                                  │
├─────────────────────────────────────────────────────────────┤
│ "Select one app to enable biometric unlock"                 │
│                                                              │
│ ┌──────────────────────────────────────────┐                │
│ │ App 1 Name                      [TOGGLE] │ ← Select ONE   │
│ └──────────────────────────────────────────┘                │
│                                                              │
│ ┌──────────────────────────────────────────┐                │
│ │ App 2 Name                      [TOGGLE] │ ← Only one can │
│ └──────────────────────────────────────────┘    be enabled  │
│                                                              │
│ Policy: <Auth Type> + Biometric for selected app            │
│                                                              │
│ [Cancel] [Save]                                             │
└─────────────────────────────────────────────────────────────┘
```

## Runtime Behavior

### When User Opens Protected App

```
┌─────────────────────────────────┐
│ App Opened                      │
│ (e.g., Instagram)               │
└──────────────┬──────────────────┘
               ↓
┌─────────────────────────────────┐
│ Check if app has Biometric      │
│ (app1FingerprintEnabled = true) │
└──────────────┬──────────────────┘
               ↓
         ┌─────┴─────┐
         │           │
      ✓ (YES)    ✗ (NO)
         │           │
         ↓           ↓
   ┌────────────┐  ┌──────────┐
   │ Show Biom. │  │ Show     │
   │ Prompt +   │  │ PIN/PWD/ │
   │ "Use PIN"  │  │ PAT      │
   │ Button     │  │ Screen   │
   └────────────┘  └──────────┘
         ↓              ↓
    ┌────┴────┐     ┌───┴───┐
    │          │     │       │
  Success   Tap      Enter  Enter
  or Fail   "Use     Auth   Backup
  Biom.     PIN"     Type   Auth Type
    │          │     │       │
    ↓          ↓     ↓       ↓
    ├──────────┤    │       │
    │          └────┤   Or  ├────┐
    │              └───────┘    │
    ↓                           ↓
    APP UNLOCKED & OPENED
    User can now use the app
```

## Policy Enforcement

### Before (Old System)
```
App 1: Biometric ✓ (Biometric Only Policy)   ← No fallback
App 2: Biometric ✓ (Biometric + PIN)         ← PIN fallback

Result: Inconsistent policies, confusing UX
```

### After (New System)
```
App 1: Biometric ✓ + PIN Backup              ← Always has backup
App 2: PIN (No Biometric)                    ← Standard auth

Result: Clear, consistent policy across group
```

## Group Settings - Biometric Management

```
┌─────────────────────────────────────────────────────────────┐
│ Group Settings                                              │
├─────────────────────────────────────────────────────────────┤
│ [← Back] Group Settings                                     │
│                                                              │
│ ♦ Change Password/Pattern                                   │
│ ♦ Fingerprint Unlock                   [Enable]             │
│ ♦ Change Group Name                                         │
│ ♦ App Logs                                                  │
│ ♦ Customisation                                             │
│ ♦ Delete Group                                              │
└─────────────────────────────────────────────────────────────┘
                        ↓ (Click Fingerprint Unlock)
┌─────────────────────────────────────────────────────────────┐
│ Biometric Unlock                                            │
├─────────────────────────────────────────────────────────────┤
│ "Select one app for biometric unlock"                       │
│ Policy: PIN + Biometric                                     │
│                                                              │
│ ┌──────────────────────────────────────────┐                │
│ │ Instagram                       [ON]  ← Currently enabled │
│ └──────────────────────────────────────────┘                │
│ Biometric + PIN Backup                                      │
│                                                              │
│ ┌──────────────────────────────────────────┐                │
│ │ WhatsApp                        [OFF]     │                │
│ └──────────────────────────────────────────┘                │
│ PIN only                                                    │
│                                                              │
│ [Cancel] [Save]                                             │
└─────────────────────────────────────────────────────────────┘
```

## Key Differences from Previous System

| Aspect | Previous | New |
|--------|----------|-----|
| **Apps with Biometric** | Both could be enabled | Only ONE allowed |
| **Fallback Option** | Optional (Biometric Only available) | Required (Always present) |
| **User Experience** | Complex with multiple options | Simple, clear single choice |
| **Policy Selection** | Separate dialog step | Automatic (<AuthType> + Biometric) |
| **Consistency** | Could differ per app | Consistent across group |
| **Backup Credentials** | If "Biometric only" chosen, no backup | Always present |
| **Security** | Variable by app choice | Standardized per group |

## Configuration Examples

### Example 1: PIN + Biometric for App 1
```
Group: Instagram + WhatsApp
Lock Type: PIN
App 1 (Instagram):
  - Biometric Enabled: YES
  - Lock Type: PIN
  - Hashed PIN: abc123...
  - Backup Method: PIN
  - Authentication Flow: Try Biometric → Fallback to PIN

App 2 (WhatsApp):
  - Biometric Enabled: NO
  - Lock Type: PIN
  - Hashed PIN: def456...
  - Authentication Flow: Direct PIN entry
```

### Example 2: Pattern + Biometric for App 2
```
Group: Telegram + Messenger
Lock Type: PATTERN
App 1 (Telegram):
  - Biometric Enabled: NO
  - Lock Type: PATTERN
  - Hashed Pattern: xyz789...
  - Authentication Flow: Draw pattern

App 2 (Messenger):
  - Biometric Enabled: YES
  - Lock Type: PATTERN
  - Hashed Pattern: uvw234...
  - Backup Method: PATTERN
  - Authentication Flow: Try Biometric → Fallback to Pattern
```

## Lock Screen UI When Biometric is Enabled

### For App with Biometric
```
┌──────────────────────────────────┐
│         X (Close)                │
│                                  │
│       UNLOCK WITH FINGERPRINT    │
│                                  │
│    [Fingerprint Sensor Ready]    │
│                                  │
│    ┌────────────────────────────┐│
│    │ Use lock credential (PIN)  ││
│    └────────────────────────────┘│
│                                  │
│       [Back Arrow]               │
└──────────────────────────────────┘
```

### For App without Biometric
```
┌──────────────────────────────────┐
│         X (Close)                │
│                                  │
│      ENTER Your PIN              │
│                                  │
│      ●  ●  ●  ●    (Dots)        │
│                                  │
│   [1] [2] [3]                    │
│   [4] [5] [6]                    │
│   [7] [8] [9]                    │
│  [0]  [Backspace]  [OK]          │
│                                  │
│       [Back Arrow]               │
└──────────────────────────────────┘
```

