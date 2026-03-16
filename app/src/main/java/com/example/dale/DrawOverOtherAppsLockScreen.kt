package com.example.dale

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.OnBackPressedCallback
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dale.ui.theme.DALETheme
import com.example.dale.ui.theme.Purple40
import com.example.dale.ui.theme.Purple80
import com.example.dale.utils.SharedPreferencesManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DrawOverOtherAppsLockScreen : ComponentActivity() {

    private var targetPackageName by mutableStateOf<String?>(null)
    private var groupId by mutableStateOf<String?>(null)
    private var isPinVerified = false
    private var isDismissing = false

    private val relaunchHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Consume system back; only top-right arrow should dismiss.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // no-op
            }
        })

        if (!SharedPreferencesManager.getInstance(this).isProtectionEnabled()) {
            finishAndRemoveTask()
            return
        }

        // Keep screen awake and visible while lock screen is active.
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        updateTargetState(intent)

        // Safety check: Never show lock screen for DALE itself
        if (targetPackageName == packageName) {
            finish()
            return
        }

        enableEdgeToEdge()

        setContent {
            DALETheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LockScreenContent(
                        modifier = Modifier.padding(innerPadding),
                        targetPackageName = targetPackageName,
                        groupId = groupId,
                        onUnlockSuccess = { unlockApp(it) },
                        onUnlockFail = { /* Handle fail */ },
                        onDismissRequested = { dismissToHome() },
                        onVerified = { isPinVerified = true }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updateTargetState(intent)
    }

    private fun updateTargetState(intent: Intent?) {
        targetPackageName = intent?.getStringExtra("TARGET_PACKAGE")
        groupId = intent?.getStringExtra("GROUP_ID")
    }

    private fun unlockApp(packageName: String) {
        // Mark PIN as verified before unlocking
        isPinVerified = true

        val sourcePackage = targetPackageName
        val currentGroupId = groupId
        val crossUnlockSource = sourcePackage?.takeIf { it.isNotBlank() && it != packageName }
        val isCrossUnlock = crossUnlockSource != null

        // For cross-unlock, first app is explicitly closed/backgrounded before opening app 2.
        if (isCrossUnlock) {
            closeSourceAppBeforeCrossUnlock(crossUnlockSource, currentGroupId)
        }

        // Mark unlock transition immediately.
        sendBroadcast(Intent(AppMonitorService.ACTION_APP_UNLOCKING).apply {
            putExtra("UNLOCKED_PACKAGE", packageName)
            putExtra("SOURCE_PACKAGE", sourcePackage)
            putExtra("GROUP_ID", currentGroupId)
        })

        handler.postDelayed({
            // Mark unlock complete before launching target app.
            sendBroadcast(Intent(AppMonitorService.ACTION_APP_UNLOCKED).apply {
                putExtra("UNLOCKED_PACKAGE", packageName)
                putExtra("SOURCE_PACKAGE", sourcePackage)
                putExtra("GROUP_ID", currentGroupId)
            })

            recordAppOpenedLog(currentGroupId, packageName)

            try {
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(launchIntent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            finishAndRemoveTask()
        }, if (isCrossUnlock) 320 else 250)
    }

    private fun closeSourceAppBeforeCrossUnlock(sourcePackage: String, groupId: String?) {
        recordAppClosedLog(groupId, sourcePackage)

        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(homeIntent)
    }

    private fun recordAppOpenedLog(groupId: String?, openedPackage: String) {
        val targetGroupId = groupId ?: return

        val sharedPrefs = SharedPreferencesManager.getInstance(this)
        val group = sharedPrefs.getAppGroup(targetGroupId) ?: return

        val appName = when (openedPackage) {
            group.app1PackageName -> group.app1Name
            group.app2PackageName -> group.app2Name
            else -> {
                try {
                    packageManager.getApplicationLabel(
                        packageManager.getApplicationInfo(openedPackage, 0)
                    ).toString()
                } catch (_: Exception) {
                    openedPackage
                }
            }
        }

        val timestamp = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
            .format(Date())

        sharedPrefs.saveActivityLog(
            groupId = targetGroupId,
            entry = ActivityLogEntry(
                appName = appName,
                packageName = openedPackage,
                event = "OPENED",
                timestamp = timestamp
            )
        )
    }

    private fun recordAppClosedLog(groupId: String?, closedPackage: String) {
        val targetGroupId = groupId ?: return

        val sharedPrefs = SharedPreferencesManager.getInstance(this)
        val group = sharedPrefs.getAppGroup(targetGroupId) ?: return

        val appName = when (closedPackage) {
            group.app1PackageName -> group.app1Name
            group.app2PackageName -> group.app2Name
            else -> {
                try {
                    packageManager.getApplicationLabel(
                        packageManager.getApplicationInfo(closedPackage, 0)
                    ).toString()
                } catch (_: Exception) {
                    closedPackage
                }
            }
        }

        val timestamp = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
            .format(Date())

        sharedPrefs.saveActivityLog(
            groupId = targetGroupId,
            entry = ActivityLogEntry(
                appName = appName,
                packageName = closedPackage,
                event = "CLOSED",
                timestamp = timestamp
            )
        )
    }

    private fun dismissToHome() {
        if (isFinishing || isDismissing || isPinVerified) return
        isDismissing = true

        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(homeIntent)
        finishAndRemoveTask()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (!isPinVerified && !isDismissing) {
            relaunchHandler.postDelayed({
                if (!isFinishing && !isPinVerified && !isDismissing) {
                    bringToFront()
                }
            }, 120)
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isPinVerified && !isDismissing) {
            relaunchHandler.postDelayed({
                if (!isFinishing && !isPinVerified && !isDismissing) {
                    bringToFront()
                }
            }, 80)
        }
    }

    override fun onDestroy() {
        relaunchHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun bringToFront() {
        val intent = Intent(this, DrawOverOtherAppsLockScreen::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            putExtra("TARGET_PACKAGE", targetPackageName)
            putExtra("GROUP_ID", groupId)
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        if (!SharedPreferencesManager.getInstance(this).isProtectionEnabled()) {
            finishAndRemoveTask()
        }
    }

    companion object {
        private val handler = Handler(Looper.getMainLooper())
    }
}

@Composable
fun LockScreenContent(
    modifier: Modifier = Modifier,
    targetPackageName: String?,
    groupId: String?,
    onUnlockSuccess: (String) -> Unit,
    onUnlockFail: () -> Unit,
    onDismissRequested: () -> Unit,
    onVerified: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = SharedPreferencesManager.getInstance(context)

    var credentialInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val appGroup = remember(groupId) {
        groupId?.let { sharedPrefs.getAppGroup(it) }
    }

    val appInfo = remember(appGroup, targetPackageName) {
        when {
            appGroup == null -> LockTarget("", "", "PIN")
            targetPackageName == appGroup.app1PackageName ->
                LockTarget(appGroup.app1PackageName, appGroup.app1LockPin, appGroup.app1LockType)
            targetPackageName == appGroup.app2PackageName ->
                LockTarget(appGroup.app2PackageName, appGroup.app2LockPin, appGroup.app2LockType)
            else -> LockTarget("", "", "PIN")
        }
    }

    val isPinMode = appInfo.lockType.uppercase() != "PASSWORD"

    suspend fun verifyAndUnlock() {
        if (isVerifying) return
        if (isPinMode && credentialInput.length != 4) return
        if (!isPinMode && credentialInput.length < 6) return

        isVerifying = true
        delay(150)

        val hashedInput = MessageDigest.getInstance("SHA-256")
            .digest(credentialInput.toByteArray())
            .joinToString("") { "%02x".format(it) }

        if (hashedInput == appInfo.lockHash) {
            errorMessage = null
            onVerified()
            onUnlockSuccess(appInfo.appPackage)
            isVerifying = false
            return
        }

        val otherAppHash = when (appInfo.appPackage) {
            appGroup?.app1PackageName -> appGroup.app2LockPin
            appGroup?.app2PackageName -> appGroup.app1LockPin
            else -> null
        }
        val otherAppType = when (appInfo.appPackage) {
            appGroup?.app1PackageName -> appGroup.app2LockType
            appGroup?.app2PackageName -> appGroup.app1LockType
            else -> null
        }
        val otherAppPackage = when (appInfo.appPackage) {
            appGroup?.app1PackageName -> appGroup.app2PackageName
            appGroup?.app2PackageName -> appGroup.app1PackageName
            else -> null
        }

        if (otherAppHash != null && otherAppPackage != null && otherAppType == appInfo.lockType && hashedInput == otherAppHash) {
            errorMessage = null
            onVerified()
            onUnlockSuccess(otherAppPackage)
        } else {
            errorMessage = if (isPinMode) "Incorrect PIN" else "Incorrect password"
            delay(420)
            credentialInput = ""
            onUnlockFail()
        }

        isVerifying = false
    }

    LaunchedEffect(credentialInput, isPinMode) {
        if (isPinMode && credentialInput.length == 4) {
            verifyAndUnlock()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1a1a2e),
                        Color(0xFF16213e)
                    )
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onDismissRequested) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            // Lock Icon
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Lock",
                tint = Purple80,
                modifier = Modifier
                    .size(80.dp)
                    .padding(bottom = 24.dp)
            )


            // Title
            Text(
                text = if (isPinMode) "Enter PIN" else "Enter Password",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Purple80,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (isPinMode) {
                // PIN Display (dots)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .shadow(
                                    elevation = if (index < credentialInput.length) 4.dp else 0.dp,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                                .background(
                                    color = if (index < credentialInput.length) Purple80 else Color(0xFF3a4b5d),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                        )
                    }
                }
            } else {
                OutlinedTextField(
                    value = credentialInput,
                    onValueChange = { credentialInput = it.take(32) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    label = { Text("Password") },
                    enabled = !isVerifying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 16.dp)
                )
            }

            // Error Message
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                Text(
                    text = errorMessage ?: "",
                    color = Color(0xFFFF6B6B),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (isPinMode) {
                // Number Pad
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Rows 1-3 (numbers 1-9)
                    for (row in 0..2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            for (col in 1..3) {
                                val number = (row * 3) + col
                                NumberButton(
                                    number = number.toString(),
                                    onClick = {
                                        if (credentialInput.length < 4 && !isVerifying) {
                                            credentialInput += number
                                            errorMessage = null
                                        }
                                    },
                                    enabled = !isVerifying
                                )
                            }
                        }
                    }

                    // Last row (empty, 0, backspace)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Empty space
                        Spacer(modifier = Modifier.size(80.dp).padding(8.dp))

                        // Zero
                        NumberButton(
                            number = "0",
                            onClick = {
                                if (credentialInput.length < 4 && !isVerifying) {
                                    credentialInput += "0"
                                    errorMessage = null
                                }
                            },
                            enabled = !isVerifying
                        )

                        // Backspace
                        NumberButton(
                            number = "⌫",
                            onClick = {
                                if (credentialInput.isNotEmpty() && !isVerifying) {
                                    credentialInput = credentialInput.dropLast(1)
                                    errorMessage = null
                                }
                            },
                            enabled = !isVerifying
                        )
                    }
                }
            } else {
                Button(
                    onClick = {
                        if (!isVerifying) {
                            errorMessage = null
                            scope.launch { verifyAndUnlock() }
                        }
                    },
                    enabled = credentialInput.length >= 6 && !isVerifying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(bottom = 20.dp)
                ) {
                    Text("Unlock")
                }
            }
        }
    }
}

private data class LockTarget(
    val appPackage: String,
    val lockHash: String,
    val lockType: String
)

@Composable
fun NumberButton(
    number: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .size(80.dp)
            .padding(8.dp)
            .shadow(
                elevation = if (enabled) 4.dp else 0.dp,
                shape = RoundedCornerShape(40.dp)
            ),
        enabled = enabled,
        shape = RoundedCornerShape(40.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF0f3460),
            disabledContainerColor = Color(0xFF0a2940)
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = number,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) Purple80 else Color(0xFF666666)
        )
    }
}
