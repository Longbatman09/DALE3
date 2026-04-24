# Biometric Unlock - Quick Testing Guide

## What Changed?

✅ **Only ONE app can now be enabled with biometric unlock per group**
✅ **No more "Biometric only" option - always has backup authentication**
✅ **Strict policy: <Lock Type> + Biometric**

---

## Testing Steps

### Test 1: Create New Group with Biometric
```
1. Start app → Create new group
2. Select App 1 & App 2 → Set Lock Type (PIN/Password/Pattern)
3. Enter credentials for both apps
4. Reach "Biometric Unlock" dialog

Expected:
- Dialog shows "Select one app to enable biometric unlock"
- Can toggle App 1 ON → App 2 becomes OFF automatically
- Can toggle App 2 ON → App 1 becomes OFF automatically
- Policy text shows: "PIN + Biometric" (or Password/Pattern)
```

### Test 2: Group Settings - Modify Biometric
```
1. Open any existing group
2. Tap "Fingerprint Unlock" button
3. See fingerprint settings dialog

Expected:
- Dialog title: "Biometric Unlock"
- Only one app can be selected
- Selecting one auto-deselects the other
- No toggle for "Biometric only" - it's gone
```

### Test 3: App Selection Dialog
```
1. Go to Change Password in Group Settings
2. See app selection dialog

Expected:
- Apps with biometric enabled show:
  "Biometric + PIN Backup" (or appropriate auth type)
- Cards are darker/disabled for biometric-only apps (but they're not)
- Clear indication of which policy each app has
```

### Test 4: Lock Screen at Runtime
```
1. Enable biometric for App 1 only
2. Open App 1

Expected Path:
a. Biometric prompt appears with:
   - "Unlock with fingerprint"
   - "Use lock credential (PIN)" button
   
b. If biometric succeeds → App opens
c. If biometric fails → Shows PIN/Password/Pattern screen

d. If user taps "Use lock credential" → PIN screen opens

1. Open App 2 (no biometric)

Expected:
- Shows PIN/Password/Pattern screen directly
- No biometric option shown
```

---

## Visual Confirmations

### Biometric Selection Dialog
```
┌─────────────────────────────────┐
│ Biometric Unlock               │
├─────────────────────────────────┤
│ Select one app...              │
│                                │
│ App 1  [Toggle ON]  ← Selected │
│ App 2  [Toggle OFF]            │
│                                │
│ Policy: PIN + Biometric        │
│                                │
│ [Cancel] [Save]               │
└─────────────────────────────────┘
```

### Lock Screen with Biometric
```
┌──────────────────────┐
│ X (Close)           │
│                      │
│ UNLOCK WITH          │
│ FINGERPRINT          │
│                      │
│ [Biometric Sensor]   │
│                      │
│ ┌──────────────────┐ │
│ │ Use lock cred.   │ │ ← Always shows
│ │ (PIN)            │ │
│ └──────────────────┘ │
│                      │
│ [Back]               │
└──────────────────────┘
```

---

## Expected Behavior for Different Scenarios

### Scenario 1: First group creation with PIN + Biometric
```
Steps:
1. Auth Type → PIN
2. Enter PIN for both apps
3. Biometric Unlock: Select App 1

Result:
- App 1: PIN + Biometric enabled, backup: PIN
- App 2: PIN only, no biometric
- Both have valid PIN hashes stored
```

### Scenario 2: Existing group - Change to biometric on different app
```
Starting: App 1 has biometric
Steps:
1. Group Settings → Fingerprint Unlock
2. Uncheck App 1
3. Check App 2
4. Save

Result:
- App 1: Now has data but biometric disabled
- App 2: Now has biometric enabled
- Only one app has biometric at end
```

### Scenario 3: Disable all biometric
```
Steps:
1. Group Settings → Fingerprint Unlock
2. Uncheck the selected app
3. Save

Result:
- Both apps: Standard lock type (PIN/Password/Pattern)
- No biometric for any app in group
- Can re-enable at any time
```

---

## Build Info
- Build Date: April 23, 2026
- Status: ✅ BUILD SUCCESSFUL
- Changes Affecting: Group creation, Group settings, Password setup, Lock screen

---

## Troubleshooting

### Issue: Both apps are selectable in biometric dialog
**Solution:** This shouldn't happen. Clear app cache and rebuild.

### Issue: Biometric prompt shows "Cancel" instead of "Use lock credential"
**Solution:** This indicates `isBiometricOnlyForTarget` is true, shouldn't be. Check app logs.

### Issue: App doesn't ask for lock screen on 2nd app from 1st app
**Solution:** Check internal activity detection - this is separate from biometric feature.

### Issue: App crashes when saving biometric settings
**Solution:** Check logcat for SharedPreferences errors. Ensure group data is valid.

---

## Success Indicators

✅ Only one app shows as biometric-enabled per group
✅ No "Biometric only" option appears anywhere
✅ Both apps always have credentials
✅ Lock screen shows fallback button when biometric enabled
✅ Switching apps properly changes lock behavior
✅ No crashes or errors in logs
✅ Group can be created successfully
✅ Existing groups still work

---

## Key Files to Check if Issues Arise

1. **GroupSettingsActivity.kt** - Lines 534-537 (save logic)
2. **PasswordSetupActivity.kt** - Lines 1051-1057, 1089-1096 (switch logic)
3. **DrawOverOtherAppsLockScreen.kt** - Line 173 (biometric check)
4. **AppGroup.kt** - Data model fields

---

## Contact / Debug

If issues arise:
1. Check logcat for errors
2. Compare with reference code in comments
3. Verify build was successful (no warnings in this feature)
4. Test on multiple devices if possible
5. Check SharedPreferences data integrity

