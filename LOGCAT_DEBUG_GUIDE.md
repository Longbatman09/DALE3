# LOGCAT MONITORING - QUICK DEBUG GUIDE

## Testing the New Implementation

### 1. Install the App
```bash
cd C:\Users\Admin\AndroidStudioProjects\DALE3
.\gradlew assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### 2. Grant READ_LOGS Permission (if needed)
```bash
adb shell pm grant com.example.dale android.permission.READ_LOGS
```

### 3. Check Service is Running
```bash
adb shell dumpsys activity services | grep AppMonitorService
```
**Expected output**: Should show service is running

### 4. Watch Live Logs
```bash
adb logcat | grep "AppMonitorService"
```

**Expected logs when opening Instagram:**
```
D/AppMonitorService: App launched: com.instagram.android (previous: com.android.launcher)
D/AppMonitorService: Protected app detected: com.instagram.android (group: 12345)
D/AppMonitorService: Showing lock screen for: com.instagram.android
```

**Expected logs when entering PIN:**
```
D/AppMonitorService: App unlocking: com.instagram.android
D/AppMonitorService: App unlocked: com.instagram.android
```

**Expected logs when app launches after unlock:**
```
D/AppMonitorService: App launched: com.instagram.android (previous: com.example.dale)
D/AppMonitorService: Protected app detected: com.instagram.android (group: 12345)
D/AppMonitorService: Within grace period: 234 ms
```

---

## Common Issues & Solutions

### Issue 1: Lock Screen Doesn't Appear
**Symptoms**: Opening protected app doesn't show lock screen

**Debug steps:**
```bash
# Check if service is running
adb shell dumpsys activity services | grep AppMonitorService

# Check logs
adb logcat | grep "AppMonitorService"

# Check if READ_LOGS is granted
adb shell dumpsys package com.example.dale | grep READ_LOGS
```

**Solutions:**
- Grant READ_LOGS: `adb shell pm grant com.example.dale android.permission.READ_LOGS`
- Restart service: Kill and reopen DALE app
- Check if app is in a group: Open DALE and verify group exists

---

### Issue 2: Lock Screen Appears Twice
**Symptoms**: Lock screen shows, closes, shows again

**Debug steps:**
```bash
# Watch for duplicate launch events
adb logcat | grep "Showing lock screen"
```

**Expected**: Should only see one "Showing lock screen" per app open  
**If seeing multiple**: This should NOT happen with new implementation

**Solutions:**
- Check logs for timing of multiple events
- Verify `lockInProgress` set is working
- Report bug with full logs

---

### Issue 3: Lock Screen Appears After Entering PIN
**Symptoms**: Enter correct PIN, app opens, lock appears again

**Debug steps:**
```bash
# Watch grace period logs
adb logcat | grep "grace period"
```

**Expected**: Should see "Within grace period: X ms" for 5 seconds after unlock

**Solutions:**
- Check if unlock timestamp is being recorded
- Verify broadcasts are being received
- Check for race conditions in logs

---

### Issue 4: DALE Shows Lock Screen to Itself
**Symptoms**: Opening DALE shows lock screen

**Debug steps:**
```bash
# Check package detection
adb logcat | grep "App launched: com.example.dale"
```

**Expected**: Should see "App launched: com.example.dale" but NO "Showing lock screen"

**Solutions:**
- This should NEVER happen with new implementation
- If it does, check the DALE package check: `if (packageName == this.packageName)`

---

### Issue 5: High Battery Usage
**Symptoms**: DALE draining battery

**Debug steps:**
```bash
# Check CPU usage
adb shell top | grep dale

# Check memory usage
adb shell dumpsys meminfo com.example.dale
```

**Expected**: 
- CPU: < 1%
- Memory: ~5-10 MB

**Solutions:**
- Logcat thread should be efficient
- Check for infinite loops in logs
- Monitor thread count

---

## Monitoring ActivityManager Logs

### View Raw ActivityManager Logs
```bash
adb logcat ActivityManager:I *:S
```

**When you open Instagram, you should see:**
```
I/ActivityManager: START u0 {act=android.intent.action.MAIN cat=[android.intent.category.LAUNCHER] flg=0x10200000 cmp=com.instagram.android/.MainActivity ...}
I/ActivityManager: Displayed com.instagram.android/.MainActivity: +234ms
```

---

## Manual Testing Scenarios

### Test 1: Basic Lock
1. Create a group with Instagram
2. Open Instagram
3. **Expected**: Lock screen appears
4. Enter correct PIN
5. **Expected**: Instagram opens

### Test 2: Grace Period
1. Open protected Instagram
2. Enter correct PIN
3. Immediately press home
4. Open Instagram again (within 5 seconds)
5. **Expected**: No lock screen appears

### Test 3: Return Window
1. Open protected Instagram
2. Enter correct PIN
3. Use app for 10 seconds (grace period expires)
4. Press home
5. Open Instagram again within 2 seconds
6. **Expected**: No lock screen appears
7. Press home again
8. Wait 3 seconds
9. Open Instagram
10. **Expected**: Lock screen appears

### Test 4: DALE Immunity
1. Create any group
2. Open DALE
3. **Expected**: Lock screen NEVER appears
4. Navigate through all DALE screens
5. **Expected**: Lock screen NEVER appears

### Test 5: Multiple Apps
1. Create group: Instagram + Calculator
2. Open Instagram → Enter PIN → Close
3. Wait 3 seconds
4. Open Calculator
5. **Expected**: Lock screen appears for Calculator
6. Enter correct PIN
7. **Expected**: Calculator opens

---

## Performance Monitoring

### Check Thread Count
```bash
adb shell ps -T | grep dale
```
**Expected**: Main thread + logcat thread = 2 threads

### Check Memory Over Time
```bash
while true; do adb shell dumpsys meminfo com.example.dale | grep "TOTAL:"; sleep 5; done
```
**Expected**: Memory should be stable, not growing

### Check CPU Usage
```bash
adb shell top -n 1 | grep dale
```
**Expected**: < 1% CPU most of the time

---

## Advanced Debugging

### Dump All Service State
```bash
adb shell dumpsys activity services com.example.dale.AppMonitorService
```

### Force Stop Service
```bash
adb shell am force-stop com.example.dale
```
Service should restart when DALE is opened

### Clear App Data & Test Fresh
```bash
adb shell pm clear com.example.dale
```
Then reinstall and test

---

## Log Analysis Tips

### Good Log Pattern:
```
D/AppMonitorService: App launched: com.instagram.android
D/AppMonitorService: Protected app detected: com.instagram.android (group: xxx)
D/AppMonitorService: Showing lock screen for: com.instagram.android
D/AppMonitorService: App unlocking: com.instagram.android
D/AppMonitorService: App unlocked: com.instagram.android
D/AppMonitorService: App launched: com.instagram.android
D/AppMonitorService: Within grace period: 234 ms
```

### Bad Log Pattern (Should Not Happen):
```
D/AppMonitorService: Showing lock screen for: com.instagram.android
D/AppMonitorService: Showing lock screen for: com.instagram.android  ← DUPLICATE!
```

### Missing Permission Pattern:
```
E/AppMonitorService: Logcat monitoring error
java.io.IOException: Permission denied
```
**Solution**: Grant READ_LOGS permission

---

## Quick Commands

```bash
# View DALE logs only
adb logcat | grep dale

# View DALE + ActivityManager
adb logcat | grep -E "(dale|ActivityManager)"

# Save logs to file
adb logcat > dale_logs.txt

# Clear logcat
adb logcat -c

# View last 100 lines
adb logcat -t 100

# Follow new logs
adb logcat -T 1
```

---

## Reporting Issues

If you find a bug, provide:
1. Full logcat output: `adb logcat > bug_report.txt`
2. Steps to reproduce
3. Expected vs actual behavior
4. Device model and Android version
5. DALE version

---

## Success Criteria

✅ Lock screen appears instantly when opening protected app  
✅ No duplicate lock screens  
✅ Grace period works for 5 seconds  
✅ Return window works for 2 seconds  
✅ DALE never locks itself  
✅ Battery usage < 1% per hour  
✅ CPU usage < 1%  
✅ No crashes or ANRs  

If all criteria are met, implementation is working correctly!

