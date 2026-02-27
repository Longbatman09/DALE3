# DALE PIN Setup - Visual Flow Diagram

## Complete App Flow (with PIN Setup)

```
┌─────────────────────────────────────────────────────────────────┐
│  LAUNCH APP                                                     │
│  (MainActivity - Debug Mode)                                    │
└────────────────┬────────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────────┐
│  WELCOME ACTIVITY                                               │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ 🔐 DALE                                                  │  │
│  │                                                           │  │
│  │ DALE is a app which allows users to create a app lock   │  │
│  │ for dual app.                                           │  │
│  │                                                           │  │
│  │ DALE helps users to keep their privacy safe by turning  │  │
│  │ a app locker into a secret gateway to opening           │  │
│  │ secondary apps.                                         │  │
│  │                                                           │  │
│  │             [Start Setup Button]                        │  │
│  └───────────────────────────────────────────────────────────┘  │
└────────────────┬────────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────────┐
│  SETUP ACTIVITY (Step 1)                                        │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ Creation of Dual App                                     │  │
│  │                                                           │  │
│  │ Detected Device: Samsung Galaxy S21                      │  │
│  │ Manufacturer: Samsung                                    │  │
│  │ Supports Native Dual App: YES ✓                          │  │
│  │                                                           │  │
│  │ Method 1: Use Built-in Dual App                         │  │
│  │ [Opens Settings → Dual Messenger Setup]                 │  │
│  │                                                           │  │
│  │ Method 2: Use Island App                                │  │
│  │ [Opens Google Play → Island App Page]                   │  │
│  │                                                           │  │
│  │             [Next Step Button]                          │  │
│  └───────────────────────────────────────────────────────────┘  │
└────────────────┬────────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────────┐
│  APP SELECTION ACTIVITY (Step 2)                                │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ SELECT APP 1                                             │  │
│  │ [WhatsApp] [Telegram] [Signal] ...                       │  │
│  │                                                           │  │
│  │ User selects: WhatsApp                                   │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ SELECT APP 2                                             │  │
│  │ [Telegram] [Signal] [Instagram] ...                      │  │
│  │                                                           │  │
│  │ User selects: Telegram                                   │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ NAME GROUP                                               │  │
│  │ Group Name: [WhatsApp + Telegram        ]               │  │
│  │                                                           │  │
│  │              [Proceed to Lock Setup]                    │  │
│  └───────────────────────────────────────────────────────────┘  │
└────────────────┬────────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────────┐
│  LOCK SCREEN SETUP ACTIVITY (Step 3)                            │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ Step 2: Lock Setup                                      │  │
│  │                                                           │  │
│  │ Ensure your dual app has been created successfully       │  │
│  │ before proceeding                                         │  │
│  │                                                           │  │
│  │ Group ID: 1708960800000                                 │  │
│  │                                                           │  │
│  │           [Setup Lock Screen Button]                   │  │
│  └───────────────────────────────────────────────────────────┘  │
└────────────────┬────────────────────────────────────────────────┘
                 │
                 ▼
╔═════════════════════════════════════════════════════════════════╗
║  PASSWORD SETUP ACTIVITY ⭐ NEW FEATURE                         ║
║  ┌───────────────────────────────────────────────────────────┐  ║
║  │ Lock Authentication                                      │  ║
║  │                                                           │  ║
║  │ Choose your authentication method to secure              │  ║
║  │ your dual apps                                           │  ║
║  │                                                           │  ║
║  │ ┌─────────────────────────────────────────────────────┐  │  ║
║  │ │ 🔐 PIN                                              │  │  ║
║  │ │    4-6 digit PIN                                    │  │  ║
║  │ └─────────────────────────────────────────────────────┘  │  ║
║  │                                                           │  ║
║  │ ┌─────────────────────────────────────────────────────┐  │  ║
║  │ │ 🔒 PASSWORD                                         │  │  ║
║  │ │    Alphanumeric password                            │  │  ║
║  │ └─────────────────────────────────────────────────────┘  │  ║
║  │                                                           │  ║
║  │ ┌─────────────────────────────────────────────────────┐  │  ║
║  │ │ ✏️  PATTERN                                          │  │  ║
║  │ │    Draw a pattern                                   │  │  ║
║  │ └─────────────────────────────────────────────────────┘  │  ║
║  │                                                           │  ║
║  │ ┌─────────────────────────────────────────────────────┐  │  ║
║  │ │ 👆 BIOMETRICS                                       │  │  ║
║  │ │    Fingerprint/Face ID                              │  │  ║
║  │ └─────────────────────────────────────────────────────┘  │  ║
║  │                                                           │  ║
║  │ User clicks: PIN                                         │  ║
║  └───────────────────────────────────────────────────────────┘  ║
║                                                                  ║
║  ┌───────────────────────────────────────────────────────────┐  ║
║  │ Enter your PIN                                           │  ║
║  │ Step 1 of 2                                              │  ║
║  │                                                           │  ║
║  │ ● ● ● ● ● ●                                              │  ║
║  │ (6 dots for PIN display)                                 │  ║
║  │                                                           │  ║
║  │ ┌─────┬─────┬─────┐                                      │  ║
║  │ │  1  │  2  │  3  │                                      │  ║
║  │ ├─────┼─────┼─────┤                                      │  ║
║  │ │  4  │  5  │  6  │                                      │  ║
║  │ ├─────┼─────┼─────┤                                      │  ║
║  │ │  7  │  8  │  9  │                                      │  ║
║  │ ├─────┴─────┴─────┤                                      │  ║
║  │ │    Clear │ Del  │                                      │  ║
║  │ └─────────────────┘                                      │  ║
║  │                                                           │  ║
║  │ User enters: 1234                                        │  ║
║  │ Display: ● ● ● ● ○ ○                                     │  ║
║  │                                                           │  ║
║  │               [Next Button]                             │  ║
║  └───────────────────────────────────────────────────────────┘  ║
║                                                                  ║
║  ┌───────────────────────────────────────────────────────────┐  ║
║  │ Confirm your PIN                                         │  ║
║  │ Step 2 of 2                                              │  ║
║  │                                                           │  ║
║  │ ● ● ● ● ● ●                                              │  ║
║  │ (6 dots for PIN display)                                 │  ║
║  │                                                           │  ║
║  │ ┌─────┬─────┬─────┐                                      │  ║
║  │ │  1  │  2  │  3  │                                      │  ║
║  │ ├─────┼─────┼─────┤                                      │  ║
║  │ │  4  │  5  │  6  │                                      │  ║
║  │ ├─────┼─────┼─────┤                                      │  ║
║  │ │  7  │  8  │  9  │                                      │  ║
║  │ ├─────┴─────┴─────┤                                      │  ║
║  │ │    Clear │ Del  │                                      │  ║
║  │ └─────────────────┘                                      │  ║
║  │                                                           │  ║
║  │ User re-enters: 1234                                     │  ║
║  │ Display: ● ● ● ● ○ ○                                     │  ║
║  │                                                           │  ║
║  │ ✓ PINs match!                                            │  ║
║  │                                                           │  ║
║  │            [Confirm Button]                             │  ║
║  └───────────────────────────────────────────────────────────┘  ║
║                                                                  ║
║  PIN: 1234                                                       ║
║       ↓ (SHA-256 Hash)                                          ║
║  Hash: a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e... ║
║       ↓ (Saved to SharedPreferences)                            ║
║  AppGroup {                                                      ║
║    id: "1708960800000"                                          ║
║    lockPin: "a665a45920..."                                     ║
║    isLocked: true                                               ║
║  }                                                               ║
║                                                                  ║
╚════════════════┬═══════════════════════════════════════════════╝
                 │
                 ▼
┌─────────────────────────────────────────────────────────────────┐
│  OVERLAY PERMISSION REQUEST                                     │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ "Allow DALE to appear on top of other apps?"             │  │
│  │                                                           │  │
│  │ This permission is needed to display the lock screen      │  │
│  │ when you open your protected apps.                        │  │
│  │                                                           │  │
│  │           [Allow] [Deny]                                │  │
│  └───────────────────────────────────────────────────────────┘  │
└────────────────┬────────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────────┐
│  MAIN ACTIVITY                                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ Setup Complete! ✓                                         │  │
│  │                                                           │  │
│  │ Your dual app group is now protected with a PIN lock.    │  │
│  │                                                           │  │
│  │ When you open WhatsApp or Telegram, you'll need to       │  │
│  │ enter your PIN to access them.                           │  │
│  │                                                           │  │
│  │ Group: WhatsApp + Telegram                              │  │
│  │ Lock Type: PIN                                           │  │
│  │ Status: 🔐 Protected                                     │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## PIN Entry Flow (Detailed)

```
START PIN SETUP
     │
     ▼
SELECT AUTH TYPE
     │
     ├─ PIN 🔐 ← User selects
     ├─ PASSWORD 🔒
     ├─ PATTERN ✏️
     └─ BIOMETRICS 👆
     │
     ▼ (PIN selected)
STEP 1: ENTER PIN
     │
     ├─ User presses: 1, 2, 3, 4
     │  Display: ● ● ● ● ○ ○
     │
     ├─ User presses: Clear (resets)
     │  Display: ○ ○ ○ ○ ○ ○
     │
     ├─ User presses: 1, 2, 3, 4 again
     │  Display: ● ● ● ● ○ ○
     │
     ├─ User presses: Next
     │
     ▼
STEP 2: CONFIRM PIN
     │
     ├─ User presses: 1, 2, 3, 4
     │  Display: ● ● ● ● ○ ○
     │
     ├─ Validation
     │  ├─ First PIN == Second PIN? ✓ YES
     │  │   └─ Continue
     │  └─ First PIN != Second PIN? ✗ NO
     │      └─ Show error "PINs do not match"
     │      └─ Reset to Step 1
     │
     ▼
HASH & SAVE
     │
     ├─ PIN: 1234
     ├─ Hash (SHA-256): a665a459...
     ├─ Save to SharedPreferences
     │
     ▼
REQUEST OVERLAY PERMISSION
     │
     ├─ Check Android version (≥ 6.0)
     │
     ├─ Permission granted? ✓ YES
     │  └─ Complete setup
     │
     └─ Permission not granted? ✗ NO
        └─ Open Settings to request
        └─ User grants permission
        └─ Complete setup
     │
     ▼
SETUP COMPLETE ✓
     │
     ├─ Mark isSetupCompleted = true
     ├─ Mark isLocked = true
     ├─ Save AppGroup to SharedPreferences
     └─ Navigate to MainActivity
```

## State Management

```
PasswordSetupScreen
├── selectedAuthType: String? = null
│   ├─ null → Show AuthenticationTypeSelection
│   └─ "PIN" → Show PinEntryScreen
│
└── PinEntryScreen
    ├── pinInput: String = ""
    │   └─ First PIN entry
    ├── pinConfirm: String = ""
    │   └─ Second PIN entry (confirmation)
    ├── step: Int = 0
    │   ├─ 0 → Enter first PIN
    │   └─ 1 → Confirm PIN
    └── errorMessage: String = ""
        ├─ "Please enter a PIN"
        ├─ "PIN must be at least 4 digits"
        └─ "PINs do not match. Please try again."
```

## Data Storage Visualization

```
BEFORE SETUP:
AppGroup {
    id: "1708960800000"
    groupName: "WhatsApp + Telegram"
    app1PackageName: "com.whatsapp"
    app2PackageName: "org.telegram.messenger"
    isLocked: false ❌
    lockPin: "" (empty)
    createdAt: 1708960800000
}

AFTER PIN SETUP:
AppGroup {
    id: "1708960800000"
    groupName: "WhatsApp + Telegram"
    app1PackageName: "com.whatsapp"
    app2PackageName: "org.telegram.messenger"
    isLocked: true ✓ CHANGED
    lockPin: "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3" ← HASHED PIN
    createdAt: 1708960800000
}

SharedPreferences Storage:
{
    "DALE_PREFS": {
        "setup_completed": true,
        "app_group_1708960800000": "{...AppGroup JSON...}"
    }
}
```

## Error Handling Flow

```
PIN ENTRY ERRORS:

Empty PIN?
└─ Show: "Please enter a PIN"

PIN < 4 digits?
└─ Show: "PIN must be at least 4 digits"

PIN > 6 digits?
└─ Ignore (max length enforced)

PINs don't match?
└─ Show: "PINs do not match. Please try again."
└─ Reset step to 0
└─ Clear both PIN fields

Permission denied?
└─ Redirect to Settings
└─ User manually grants permission
└─ Complete setup on return
```

## Color & Style Guide

```
PRIMARY COLORS:
Background Gradient: #1a1a2e → #16213e
Card Background: #2a2a3e
Accent: #9575CD (Purple)
Error: #FF6B6B (Red)

TEXT COLORS:
Primary: #FFFFFF (White)
Secondary: #B0B0B0 (Gray)

SPACING:
Standard Padding: 16.dp
Card Spacing: 12.dp
Dot Spacing: 8.dp

SHADOWS:
Card Elevation: 8.dp
Button Elevation: 4.dp
```

---

**Created**: February 27, 2026  
**Status**: ✅ Production Ready

