package com.example.dale

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dale.ui.theme.DALETheme
import com.example.dale.ui.theme.Purple40
import com.example.dale.ui.theme.Purple80
import com.example.dale.utils.SharedPreferencesManager
import kotlinx.coroutines.delay
import java.security.MessageDigest

class DrawOverOtherAppsLockScreen : ComponentActivity() {

    private var targetPackageName: String? = null
    private var groupId: String? = null
    private var isPinVerified = false
    private val refocusHandler = Handler(Looper.getMainLooper())
    private val refocusRunnable = Runnable {
        if (!isPinVerified && !isFinishing) {
            bringToFront()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make this activity appear over other apps and stay on top
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        // Set highest priority to stay above other activities
        window.attributes = window.attributes.apply {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }

        // Handle back button press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Prevent back button from closing the lock screen
                // User must enter correct PIN
                moveTaskToBack(true)
            }
        })

        // Get the target package name and group ID from intent
        targetPackageName = intent.getStringExtra("TARGET_PACKAGE")
        groupId = intent.getStringExtra("GROUP_ID")

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
                        onUnlockFail = { /* Handle fail */ }
                    )
                }
            }
        }
    }

    private fun unlockApp(packageName: String) {
        // Mark PIN as verified before unlocking
        isPinVerified = true
        refocusHandler.removeCallbacks(refocusRunnable)

        // Mark unlock transition immediately.
        sendBroadcast(Intent(AppMonitorService.ACTION_APP_UNLOCKING).apply {
            putExtra("UNLOCKED_PACKAGE", packageName)
        })

        handler.postDelayed({
            // Mark unlock complete before launching target app.
            sendBroadcast(Intent(AppMonitorService.ACTION_APP_UNLOCKED).apply {
                putExtra("UNLOCKED_PACKAGE", packageName)
            })

            try {
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(launchIntent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Close lock screen with no animation and remove from recents
            finishAndRemoveTask()
        }, 250)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus && !isPinVerified && !isFinishing) {
            // Lock screen lost focus, schedule to bring it back
            refocusHandler.postDelayed(refocusRunnable, 100)
        }
    }

    override fun onPause() {
        super.onPause()
        if (!isPinVerified && !isFinishing) {
            // Activity paused without PIN verification, bring it back
            refocusHandler.postDelayed(refocusRunnable, 50)
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isPinVerified && !isFinishing) {
            // Activity stopped without PIN verification, bring it back immediately
            bringToFront()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        refocusHandler.removeCallbacks(refocusRunnable)
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
    onUnlockFail: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = SharedPreferencesManager.getInstance(context)

    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }

    val appGroup = remember(groupId) {
        groupId?.let { sharedPrefs.getAppGroup(it) }
    }

    val appInfo = remember(appGroup, targetPackageName) {
        when {
            appGroup == null -> Triple("Unknown App", "", "")
            targetPackageName == appGroup.app1PackageName ->
                Triple(appGroup.app1Name, appGroup.app1PackageName, appGroup.app1LockPin)
            targetPackageName == appGroup.app2PackageName ->
                Triple(appGroup.app2Name, appGroup.app2PackageName, appGroup.app2LockPin)
            else -> Triple("Unknown App", "", "")
        }
    }

    val (appName, appPackage, correctPin) = appInfo

    LaunchedEffect(pin) {
        if (pin.length == 4) {
            isVerifying = true
            delay(300)

            // Hash the entered PIN for comparison
            val hashedPin = MessageDigest.getInstance("SHA-256")
                .digest(pin.toByteArray())
                .joinToString("") { "%02x".format(it) }

            if (hashedPin == correctPin) {
                errorMessage = null
                onUnlockSuccess(appPackage)
            } else {
                // Check if user entered the other app's PIN
                val otherAppPin = when (appPackage) {
                    appGroup?.app1PackageName -> appGroup.app2LockPin
                    appGroup?.app2PackageName -> appGroup.app1LockPin
                    else -> null
                }

                val otherAppPackage = when (appPackage) {
                    appGroup?.app1PackageName -> appGroup.app2PackageName
                    appGroup?.app2PackageName -> appGroup.app1PackageName
                    else -> null
                }

                if (otherAppPin != null && hashedPin == otherAppPin && otherAppPackage != null) {
                    // User entered the other app's PIN - open that app instead
                    errorMessage = null
                    onUnlockSuccess(otherAppPackage)
                } else {
                    errorMessage = "Incorrect PIN"
                    delay(500)
                    pin = ""
                    onUnlockFail()
                }
            }

            isVerifying = false
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
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
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
                text = "Enter PIN",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Purple80,
                modifier = Modifier.padding(bottom = 16.dp)
            )

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
                                elevation = if (index < pin.length) 4.dp else 0.dp,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                            .background(
                                color = if (index < pin.length) Purple80 else Color(0xFF3a4b5d),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                }
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

            Spacer(modifier = Modifier.height(32.dp))

            // Number Pad
            Column(
                modifier = Modifier.fillMaxWidth(),
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
                                    if (pin.length < 4 && !isVerifying) {
                                        pin += number
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
                            if (pin.length < 4 && !isVerifying) {
                                pin += "0"
                                errorMessage = null
                            }
                        },
                        enabled = !isVerifying
                    )

                    // Backspace
                    NumberButton(
                        number = "⌫",
                        onClick = {
                            if (pin.isNotEmpty() && !isVerifying) {
                                pin = pin.dropLast(1)
                                errorMessage = null
                            }
                        },
                        enabled = !isVerifying
                    )
                }
            }
        }
    }
}

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


