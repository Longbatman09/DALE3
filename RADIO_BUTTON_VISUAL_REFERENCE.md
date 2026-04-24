# Biometric Unlock UI - Visual Reference Guide

## Dialog Layout - Detailed

### Phone Screen Size (360 x 800dp)

```
╔════════════════════════════════════════════╗
║  Biometric Unlock                          ║  ← Title (Bold, 20sp)
╠════════════════════════════════════════════╣
║                                            ║
║ Select one app to enable biometric unlock.║  ← Description
║ The backup authentication method will be  ║     (12sp, gray)
║ the same as the group's lock type.        ║
║                                            ║
║ Policy: PIN + Biometric                   ║  ← Policy text
║ (italicized, blue, 11sp)                  ║
║                                            ║
╠════════════════════════════════════════════╣  (Card Separator)
║                                            ║
║ ┌──────────────────────────────────────┐  ║
║ │  OFF                              [✓] │  ║  ← OFF option (selected)
║ └──────────────────────────────────────┘  ║     Dark blue background
║                                            ║
║ ┌──────────────────────────────────────┐  ║
║ │ [📷] Instagram                 [ ] │  ║  ← App 1 (32dp icon)
║ │                                      │  ║     Unselected
║ └──────────────────────────────────────┘  ║
║                                            ║
║ ┌──────────────────────────────────────┐  ║
║ │ [📍] WhatsApp                  [ ]  │  ║  ← App 2 (32dp icon)
║ │                                      │  ║     Unselected
║ └──────────────────────────────────────┘  ║
║                                            ║
╠════════════════════════════════════════════╣
║ [Cancel]                          [Save]  ║  ← Action Buttons
╚════════════════════════════════════════════╝
```

---

## Card Styling Details

### Unselected Card
```
┌─────────────────────────────────────────┐
│ [APP_ICON] App Name                  [ ]│  ← Unchecked box
│                                         │
│ Background: #0F2A54 (Dark Blue)         │
│ Text Color: White (14sp)                │
│ Padding: 12dp on all sides              │
│ Corner Radius: 8dp                      │
│ Clickable: Yes (changes on tap)         │
└─────────────────────────────────────────┘

Icon Details:
├─ Size: 32dp (32x32 pixels)
├─ Radius: 6dp (rounded corners)
├─ Spacing from left: 12dp
└─ Content: App launcher icon

Checkbox Details:
├─ Size: 24dp
├─ Radius: 4dp
├─ Background: #546E7A (Gray)
├─ Border: None
└─ Content: Empty (no checkmark)
```

### Selected Card
```
┌─────────────────────────────────────────┐
│ [APP_ICON] App Name                  [✓]│  ← Checked box
│                                         │
│ Background: #0F4A8F (Bright Blue)       │
│ Text Color: White (14sp)                │
│ Padding: 12dp on all sides              │
│ Corner Radius: 8dp                      │
│ Clickable: Yes (can deselect)           │
└─────────────────────────────────────────┘

Checkbox Details:
├─ Size: 24dp
├─ Radius: 4dp
├─ Background: #5DADE2 (Light Blue)
├─ Border: None
└─ Content: ✓ (Checkmark, white, bold)
```

---

## Icon Rendering

### Icon in Card
```
   Icon Area (32dp × 32dp)
   ┌────────────────────┐
   │                    │
   │     📱 Icon       │  ← 32px app launcher icon
   │   (with radius)    │     from PackageManager
   │                    │
   └────────────────────┘
   
   Spacing:
   ├─ From left edge: 12dp
   ├─ From top edge: Bottom-aligned to text
   └─ Border radius: 6dp
```

### Multiple States
```
Unselected with Icon              Selected with Icon
┌─────────────────────────┐      ┌─────────────────────────┐
│ [📱] App Name      [ ]  │      │ [📱] App Name      [✓]  │
│ Bkg: #0F2A54           │      │ Bkg: #0F4A8F           │
└─────────────────────────┘      └─────────────────────────┘

Icon Rendering:
├─ Format: Android app launcher icon
├─ Source: PackageManager.getApplicationIcon()
├─ Size in Compose: 32.dp with .clip(RoundedCornerShape(6.dp))
├─ Bitmap conversion: icon.toBitmap().asImageBitmap()
└─ Fallback: Icon not shown if fails to load
```

---

## Complete Dialog Flow

### Step 1: Dialog Opens (Default State)
```
✓ OFF selected by default
✗ App1 unselected
✗ App2 unselected
→ "Next" button enabled (always, no validation needed)
```

### Step 2: User Taps App 1
```
✗ OFF deselected
✓ App1 selected (checkbox appears, bg turns blue)
✗ App2 unselected
→ "Next" button ready to proceed
```

### Step 3: User Taps OFF
```
✓ OFF selected again
✗ App1 deselected
✗ App2 unselected
→ "Next" button still enabled
```

### Step 4: User Taps Save/Next
```
Configuration saved based on selection:
- If OFF: app1FingerprintEnabled = false, app2FingerprintEnabled = false
- If App1: app1FingerprintEnabled = true, app2FingerprintEnabled = false
- If App2: app1FingerprintEnabled = false, app2FingerprintEnabled = true
```

---

## Responsive Layout

### Small Phone (360dp width)
```
✓ Full width cards
✓ Icons visible
✓ Text truncated if needed
✓ All 3 options visible
```

### Large Phone / Tablet (480dp+ width)
```
✓ Cards remain full width
✓ Icons scaled proportionally
✓ More spacious appearance
✓ Better for landscape orientation
```

---

## Animation & Transitions

### Card Selection Animation
```
Unselected → Selected:
├─ Background color change: 200ms
├─ Checkbox appears: Immediate
├─ Wave effect: None (tap feedback only)
└─ Text remains same

Selected → Unselected:
├─ Background color change: 200ms
├─ Checkbox disappears: Immediate
└─ Text remains same
```

### Dialog Appearance
```
On Open:
├─ Fade in: 300ms
├─ All cards visible
├─ Default selection: OFF
└─ Icons load from PackageManager

On Close:
├─ Fade out: 300ms
├─ State preserved if reopened
└─ Icons cached during session
```

---

## Accessibility Considerations

### Text Sizes
```
- Dialog Title: 20sp, Bold
- Description: 12sp, Gray
- App Names: 14sp, White
- Policy Text: 11sp, Blue, Italic
- Checkbox: 24dp (good touch target)
```

### Touch Targets
```
- Entire Card: Touchable (not just checkbox)
- Minimum Size: 48dp (recommended by Material Design)
- Checkbox: 24dp (inside 48dp card area)
- Card Padding: 12dp (allows comfortable clicking)
```

### Color Contrast
```
- White text on #0F3460: ✓ Good contrast
- White text on #0F4A8F: ✓ Good contrast
- Gray text on #1a1a2e: ✓ Acceptable
- Blue #5DADE2 on #0F4A8F: ✓ Visible
```

---

## Example Screenshots Description

### Dialog with OFF Selected
```
┌────────────────────────────────┐
│ Biometric Unlock              │
├────────────────────────────────┤
│ Select one app...             │
│                               │
│ ┌─────────────────────────┐  │
│ │ OFF               [✓]  │  │ ← Blue bg
│ └─────────────────────────┘  │
│                               │
│ ┌─────────────────────────┐  │
│ │ [📱] Instagram    [ ]  │  │ ← Dark bg
│ └─────────────────────────┘  │
│                               │
│ ┌─────────────────────────┐  │
│ │ [💬] WhatsApp     [ ]  │  │ ← Dark bg
│ └─────────────────────────┘  │
│                               │
│ [Cancel]                [Save] │
└────────────────────────────────┘
```

### Dialog with App 1 Selected
```
┌────────────────────────────────┐
│ Biometric Unlock              │
├────────────────────────────────┤
│ Select one app...             │
│                               │
│ ┌─────────────────────────┐  │
│ │ OFF                [ ]  │  │ ← Dark bg
│ └─────────────────────────┘  │
│                               │
│ ┌─────────────────────────┐  │
│ │ [📱] Instagram    [✓]  │  │ ← Blue bg
│ └─────────────────────────┘  │
│                               │
│ ┌─────────────────────────┐  │
│ │ [💬] WhatsApp     [ ]  │  │ ← Dark bg
│ └─────────────────────────┘  │
│                               │
│ When biometric fails, user    │
│ will use PIN as backup.       │
│                               │
│ [Cancel]                [Save] │
└────────────────────────────────┘
```

---

## Technical Specifications

### Compose UI Hierarchy
```
AlertDialog
├─ title: Text("Biometric Unlock")
├─ text: Column
│  ├─ Text(Description)
│  ├─ Text(Policy)
│  ├─ Card[OFF]
│  ├─ Spacer
│  ├─ Card[App1]
│  │  ├─ Row (clickable)
│  │  ├─ Image(icon1)
│  │  ├─ Text(app1Name)
│  │  └─ Box(checkbox)
│  ├─ Spacer
│  ├─ Card[App2]
│  │  ├─ Row (clickable)
│  │  ├─ Image(icon2)
│  │  ├─ Text(app2Name)
│  │  └─ Box(checkbox)
│  └─ Text(Helper)
├─ confirmButton: TextButton(Save)
└─ dismissButton: TextButton(Cancel)
```

### State Management
```
selectedApp: MutableState<String>
├─ "" (Empty) = OFF selected
├─ "app1" = App 1 selected
└─ "app2" = App 2 selected
```

---

## Platform Support
- ✓ Android 26+
- ✓ All screen sizes
- ✓ All orientations
- ✓ Dark mode compatible
- ✓ Accessibility support

