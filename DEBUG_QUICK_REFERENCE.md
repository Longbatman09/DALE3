# Quick Debug Reference Card

## 🚀 Quick Start

### To see app open/close logs in real-time:

1. Connect Android device or open Emulator
2. Open Android Studio
3. Go to: View → Tool Windows → Logcat (Alt+6)
4. In filter box, type: `AppDetection`
5. Open a protected app on your phone
6. **Watch logs appear in real-time!** 🎯

---

## 📋 Log Event Cheat Sheet

| Event | Logcat Message | Meaning |
|-------|---|---|
| App Opens | `📱 APP_OPENED: [App] ([Package])` | Protected app opened |
| Lock Shows | `🔐 LOCK_SCREEN_TRIGGERED` | Need credentials |
| App Closes | `🔒 APP_CLOSED: [App] ([Package])` | Left the app |
| Home | `🏠 HOME_SCREEN_OPENED` | User on home screen |
| Recents | `📋 RECENTS_OPENED` | Recents app open |
| Switch | `🔄 APP_SWITCHED_FROM_RECENTS` | Opened from recents |
| Grace | `⏳ GRACE_PERIOD_ACTIVE` | Recently unlocked |
| Unlocked | `🔓 APP_TEMPORARILY_UNLOCKED` | Already unlocked |
| Error | `❌ ERROR_[TYPE]` | Something went wrong |

---

## 🔍 Logcat Filter Examples

**Show only app detection:**
```
AppDetection
```

**Show only errors:**
```
AppDetection level:ERROR
```

**Show opens and closes:**
```
AppDetection AND (APP_OPENED OR APP_CLOSED)
```

**Show lock screen events:**
```
AppDetection AND LOCK_SCREEN_TRIGGERED
```

---

## 📱 What Gets Logged

✅ When app opens  
✅ When lock screen shows  
✅ When app closes  
✅ When home screen shows  
✅ When recents opened  
✅ When switching apps  
✅ Errors and issues  
✅ Grace periods  

---

## 💾 Log Files

**File Location:** `/data/data/com.example.dale/files/activity_log.txt`

**View via Android Studio:**
1. Device Explorer (bottom)
2. Navigate to: data → data → com.example.dale → files
3. Double-click activity_log.txt

**View via Terminal:**
```bash
adb shell cat /data/data/com.example.dale/files/activity_log.txt
```

---

## 🧪 Test Checklist

- [ ] Accessibility service enabled
- [ ] Created test group
- [ ] Opened protected app
- [ ] Saw lock screen
- [ ] Entered PIN
- [ ] App unlocked
- [ ] Closed app
- [ ] Saw close log
- [ ] Checked logcat for all events
- [ ] Verified app/close timestamps

---

## ⚡ Troubleshooting

**Not seeing logs?**
- Check accessibility service is enabled
- Make sure filter is set to "AppDetection"
- Clear logcat (⌘ K or Ctrl+L)
- Reopen app

**Seeing lock screen but no log?**
- Check file permissions
- Verify AppActivityLogger initialized
- Check for errors with: `AppDetection level:ERROR`

**Duplicate events?**
- Normal - accessibility service is working fine
- Events may fire multiple times per transition

---

## 📊 Example Log Output

```
📡 Event received - Package: com.whatsapp, Type: WINDOW_STATE_CHANGED
📱 APP_OPENED: WhatsApp (com.whatsapp) from group 'Social Media' [via Accessibility Service]
🔐 LOCK_SCREEN_TRIGGERED ========================================
   App: WhatsApp
   Package: com.whatsapp
   Group: Social Media
   Method: Accessibility Service
   Status: User must enter credentials to unlock
🔐 ========================================
🔒 APP_CLOSED: WhatsApp (com.whatsapp) from group 'Social Media' [via Accessibility Service]
```

---

## 🎯 Key Points

✅ Logcat is **real-time** - events show instantly  
✅ File logs are **persistent** - stored on device  
✅ Filter by **"AppDetection"** - shows only relevant events  
✅ Emoji indicators make **identification easy**  
✅ Include full context: **app name, package, group**  

---

**Status:** ✅ App logging working perfectly!

