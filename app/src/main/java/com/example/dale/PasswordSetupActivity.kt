package com.example.dale

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.example.dale.ui.theme.DALETheme
import com.example.dale.ui.theme.Purple40
import com.example.dale.utils.MonitorStartupHelper
import com.example.dale.utils.SharedPreferencesManager
import java.security.MessageDigest
import androidx.compose.material3.OutlinedTextField
import kotlinx.coroutines.delay

class PasswordSetupActivity : ComponentActivity() {
    private var groupId: String = ""
    private var overlayPermissionRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        groupId = intent.getStringExtra("groupId") ?: ""
        // directly query overlay permission (project minSdk >= M)
        val overlayAllowed = Settings.canDrawOverlays(this)

        setContent {
            DALETheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PasswordSetupScreen(
                        modifier = Modifier.padding(innerPadding),
                        groupId = groupId,
                        activity = this,
                        overlayAllowedInitial = overlayAllowed
                    )
                }
            }
        }

        // Monitor overlay permission status changes
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                if (overlayPermissionRequested && Settings.canDrawOverlays(this@PasswordSetupActivity)) {
                    // Permission was granted after returning from settings
                    completePasswordSetup(groupId)
                    overlayPermissionRequested = false
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Check if permission was granted after returning from settings
        if (overlayPermissionRequested && Settings.canDrawOverlays(this)) {
            completePasswordSetup(groupId)
            overlayPermissionRequested = false
        }
    }

    // Save hashed PIN for specific app index (1 or 2)
    fun savePinForApp(groupId: String, appIndex: Int, pin: String) {
        val sharedPrefsManager = SharedPreferencesManager.getInstance(this)
        val appGroup = sharedPrefsManager.getAppGroup(groupId)
        if (appGroup != null) {
            val hashedPin = hashPin(pin)
            val newGroup = when (appIndex) {
                1 -> appGroup.copy(app1LockPin = hashedPin, isLocked = true)
                2 -> appGroup.copy(app2LockPin = hashedPin, isLocked = true)
                else -> appGroup
            }
            sharedPrefsManager.saveAppGroup(newGroup)
        }
    }

    fun proceedToOverlayPermission(groupId: String) {
        // Request overlay permission if not already granted
        if (!Settings.canDrawOverlays(this)) {
            overlayPermissionRequested = true
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:$packageName".toUri()
            )
            startActivity(intent)
        } else {
            // Permission already granted, complete setup immediately
            completePasswordSetup(groupId)
        }
    }

    fun completePasswordSetup(groupId: String) {
        val sharedPrefsManager = SharedPreferencesManager.getInstance(this)
        val appGroup = sharedPrefsManager.getAppGroup(groupId)

        if (appGroup != null) {
            // mark locked if any pin exists
            val locked = appGroup.app1LockPin.isNotEmpty() || appGroup.app2LockPin.isNotEmpty()
            sharedPrefsManager.saveAppGroup(appGroup.copy(isLocked = locked))
        }

        // Mark setup as completed
        sharedPrefsManager.setSetupCompleted(true)

        // Start monitoring immediately if the required permissions are already available.
        MonitorStartupHelper.startMonitoringIfPossible(this)

        // Navigate to main app
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun hashPin(pin: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(pin.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}

@Composable
fun PasswordSetupScreen(
    modifier: Modifier = Modifier,
    groupId: String = "",
    activity: ComponentActivity? = null,
    overlayAllowedInitial: Boolean = true
) {
    val selectedAuthType = remember { mutableStateOf<String?>(null) }
    val targetAppIndex = remember { mutableStateOf(1) } // 1 or 2
    val showOverlayDialog = remember { mutableStateOf(false) }
    val overlayAllowed = remember { mutableStateOf(overlayAllowedInitial) }
    
    // Store the first app's PIN temporarily to compare with the second
    val firstAppPin = remember { mutableStateOf<String?>(null) }

    // resetKey forces PinEntryScreen recomposition when changed
    val resetKey = remember { mutableStateOf(0) }

    // Load app names for UI
    val app1Name = remember { mutableStateOf("App 1") }
    val app2Name = remember { mutableStateOf("App 2") }

    // Load actual names from SharedPreferences
    val sharedPrefs = SharedPreferencesManager.getInstance(activity as ComponentActivity)
    val appGroup = remember { mutableStateOf(sharedPrefs.getAppGroup(groupId)) }
    appGroup.value?.let { group ->
        if (group.app1Name.isNotEmpty()) app1Name.value = group.app1Name
        if (group.app2Name.isNotEmpty()) app2Name.value = group.app2Name
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1a1a2e),
                        Color(0xFF16213e)
                    )
                )
            )
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { (activity as? PasswordSetupActivity)?.finish() },
                    modifier = Modifier.weight(0.1f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Lock Authentication",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9575CD),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(0.9f)
                )
            }

            // Info text
            Text(
                text = "Choose your authentication method to secure your dual apps",
                fontSize = 13.sp,
                color = Color(0xFFB0B0B0),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Content container that takes remaining space
            Box(modifier = Modifier.weight(1f)) {
                // When not chosen auth type yet, show selection
                if (selectedAuthType.value == null) {
                    AuthenticationTypeSelection(
                        onAuthTypeSelected = { authType ->
                            selectedAuthType.value = authType
                        }
                    )
                } else {
                    // Show Pin entry for the current target app
                    val appName = if (targetAppIndex.value == 1) app1Name.value else app2Name.value
                    // Use key with resetKey and target index to force a fresh PinEntryScreen when switching
                    key(resetKey.value, targetAppIndex.value) {
                        PinEntryScreen(
                            authType = selectedAuthType.value ?: "PIN",
                            forAppName = appName,
                            forbiddenPin = if (targetAppIndex.value == 2) firstAppPin.value else null,
                            onPinConfirmed = { pin ->
                                // Save pin for app
                                (activity as? PasswordSetupActivity)?.savePinForApp(groupId, targetAppIndex.value, pin)
                                if (targetAppIndex.value == 1) {
                                    firstAppPin.value = pin
                                    // move to set PIN for app 2 without toggling selection (avoid glitch)
                                    targetAppIndex.value = 2
                                    // bump resetKey to force recollection of PinEntryScreen internal remembers
                                    resetKey.value = resetKey.value + 1
                                    // ensure selectedAuthType stays PIN so the flow continues
                                    selectedAuthType.value = "PIN"
                                } else {
                                    // both pins set -> show overlay permission dialog if needed
                                    if (!overlayAllowed.value) {
                                        showOverlayDialog.value = true
                                    } else {
                                        (activity as? PasswordSetupActivity)?.proceedToOverlayPermission(groupId)
                                    }
                                }
                            },
                            onBackToSelection = {
                                // go back to auth selection
                                selectedAuthType.value = null
                                targetAppIndex.value = 1
                                firstAppPin.value = null
                            }
                        )
                    }
                }
            }
        }

        // Overlay permission confirmation dialog
        if (showOverlayDialog.value) {
            AlertDialog(
                onDismissRequest = { showOverlayDialog.value = false },
                title = {
                    Text(text = "Enable 'Display over other apps'?", fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        "DALE needs the 'Display over other apps' permission so it can show the lock screen overlay."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showOverlayDialog.value = false
                        (activity as? PasswordSetupActivity)?.proceedToOverlayPermission(groupId)
                    }) {
                        Text("Open Overlay Settings")
                    }
                }
            )
        }
    }
}

@Composable
fun AuthenticationTypeSelection(
    onAuthTypeSelected: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val authTypes = listOf(
            AuthType("PIN", "4-6 digit PIN", "🔐", enabled = true),
            AuthType("PASSWORD", "Alphanumeric password", "🔒", enabled = false),
            AuthType("PATTERN", "Draw a pattern", "✏️", enabled = false),
            AuthType("BIOMETRICS", "Fingerprint/Face ID", "👆", enabled = false)
        )

        authTypes.forEach { authType ->
            AuthenticationTypeSelectionCard(
                authType = authType,
                onSelected = { if (authType.enabled) onAuthTypeSelected(authType.name) }
            )
        }
    }
}

data class AuthType(
    val name: String,
    val description: String,
    val icon: String,
    val enabled: Boolean = true
)

@Composable
fun AuthenticationTypeSelectionCard(
    authType: AuthType,
    onSelected: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(8.dp))
            .then(if (authType.enabled) Modifier.clickable { onSelected() } else Modifier)
            .alpha(if (authType.enabled) 1f else 0.45f),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2a2a3e)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = authType.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9575CD)
                )
                Text(
                    text = authType.description,
                    fontSize = 11.sp,
                    color = Color(0xFFB0B0B0),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Text(
                text = authType.icon,
                fontSize = 22.sp,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Composable
fun PinEntryScreen(
    authType: String = "PIN",
    forAppName: String = "App",
    forbiddenPin: String? = null,
    onPinConfirmed: (String) -> Unit = {},
    onBackToSelection: () -> Unit = {}
) {
    val pinInput = remember { mutableStateOf("") }
    val pinConfirm = remember { mutableStateOf("") }
    val step = remember { mutableStateOf(0) } // 0: Enter PIN, 1: Confirm PIN
    val errorMessage = remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val currentPin = if (step.value == 0) pinInput.value else pinConfirm.value
    val isButtonEnabled = currentPin.length >= 4

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top content area
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onBackToSelection() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }

            // Step indicator
            Text(
                text = if (step.value == 0) "Enter PIN for $forAppName" else "Confirm PIN for $forAppName",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF9575CD),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // Step counter
            Text(
                text = "Step ${step.value + 1} of 2",
                fontSize = 12.sp,
                color = Color(0xFFB0B0B0),
                modifier = Modifier.padding(bottom = 18.dp)
            )

            // PIN Display
            PinDisplayBox(
                pin = currentPin,
                modifier = Modifier.padding(bottom = 18.dp)
            )

            // Error message
            if (errorMessage.value.isNotEmpty()) {
                Text(
                    text = errorMessage.value,
                    fontSize = 12.sp,
                    color = Color(0xFFFF6B6B),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Use system numeric keyboard via an OutlinedTextField (hidden text)
            val currentPinState = if (step.value == 0) pinInput else pinConfirm

            OutlinedTextField(
                value = currentPinState.value,
                onValueChange = { value ->
                    // allow only digits and limit to 4
                    val filtered = value.filter { it.isDigit() }.take(4)
                    currentPinState.value = filtered
                },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .focusRequester(focusRequester)
                    .alpha(0f) // hide the actual field visually
            )

            // ensure keyboard opens when screen appears
            androidx.compose.runtime.LaunchedEffect(Unit) {
                delay(150)
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        }

        // Continue Button at the bottom
        Button(
            onClick = {
                when {
                    step.value == 0 -> {
                        // Check if this PIN is forbidden (same as the other app) right at Step 1
                        if (forbiddenPin != null && currentPin == forbiddenPin) {
                            errorMessage.value = "Same PIN cant be used for 2 apps"
                            pinInput.value = ""
                        } else {
                            // Move to confirmation step
                            step.value = 1
                            errorMessage.value = ""
                        }
                    }
                    step.value == 1 -> {
                        // Verify PINs match
                        if (pinInput.value == pinConfirm.value) {
                            // PINs match, proceed (Forbidden check already done in Step 0)
                            onPinConfirmed(pinInput.value)
                        } else {
                            errorMessage.value = "PINs do not match. Please try again."
                            // Reset to first step
                            step.value = 0
                            pinInput.value = ""
                            pinConfirm.value = ""
                        }
                    }
                }
            },
            enabled = isButtonEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Purple40,
                disabledContainerColor = Purple40.copy(alpha = 0.38f),
                disabledContentColor = Color.White.copy(alpha = 0.38f)
            )
        ) {
            Text(
                text = if (step.value == 0) "Next" else "Confirm",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isButtonEnabled) Color.White else Color.White.copy(alpha = 0.38f)
            )
        }
    }
}

@Composable
fun PinDisplayBox(
    pin: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2a2a3e)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { index ->
                    PinDot(isFilled = index < pin.length)
                }
            }
        }
    }
}

@Composable
fun PinDot(isFilled: Boolean) {
    Box(
        modifier = Modifier
            .size(14.dp)
            .background(
                color = if (isFilled) Color(0xFF9575CD) else Color(0xFF4a4a5e),
                shape = RoundedCornerShape(50)
            )
    )
}
