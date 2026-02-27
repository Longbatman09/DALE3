package com.example.dale

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dale.ui.theme.DALETheme
import com.example.dale.ui.theme.Purple40
import com.example.dale.utils.SharedPreferencesManager
import java.security.MessageDigest

class PasswordSetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val groupId = intent.getStringExtra("groupId") ?: ""
        val overlayAllowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else true

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
    }

    // Save hashed PIN for specific app index (1 or 2)
    fun savePinForApp(groupId: String, appIndex: Int, pin: String) {
        val sharedPrefsManager = SharedPreferencesManager.getInstance(this)
        val appGroup = sharedPrefsManager.getAppGroup(groupId)
        if (appGroup != null) {
            val hashedPin = hashPin(pin)
            val newGroup = when (appIndex) {
                1 -> appGroup.copy(app1LockPin = hashedPin, isLocked = appGroup.isLocked || true)
                2 -> appGroup.copy(app2LockPin = hashedPin, isLocked = appGroup.isLocked || true)
                else -> appGroup
            }
            sharedPrefsManager.saveAppGroup(newGroup)
        }
    }

    fun proceedToOverlayPermission(groupId: String) {
        // Check if overlay permission is granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                // Request overlay permission
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                // Permission already granted, complete setup
                completePasswordSetup(groupId)
            }
        } else {
            // For older Android versions, permission is automatically granted
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

    // Load app names for UI
    val app1Name = remember { mutableStateOf("App 1") }
    val app2Name = remember { mutableStateOf("App 2") }

    // Load actual names from SharedPreferences
    val sharedPrefs = SharedPreferencesManager.getInstance((activity as? ComponentActivity)!!)
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
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
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
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9575CD),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(0.9f)
                )
            }

            // Info text
            Text(
                text = "Choose your authentication method to secure your dual apps",
                fontSize = 14.sp,
                color = Color(0xFFB0B0B0),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

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
                PinEntryScreen(
                    authType = selectedAuthType.value ?: "PIN",
                    groupId = groupId,
                    activity = activity,
                    forAppName = appName,
                    onPinConfirmed = { pin ->
                        // Save pin for app
                        (activity as? PasswordSetupActivity)?.savePinForApp(groupId, targetAppIndex.value, pin)
                        if (targetAppIndex.value == 1) {
                            // move to set PIN for app 2
                            targetAppIndex.value = 2
                            // reset internal PinEntryScreen state by toggling selectedAuthType
                            selectedAuthType.value = null
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
                    }
                )
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
                        "DALE needs the 'Display over other apps' permission so it can show the lock screen overlay.\n\n" +
                                "We will open the system settings where you can grant the permission to DALE."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showOverlayDialog.value = false
                        (activity as? PasswordSetupActivity)?.proceedToOverlayPermission(groupId)
                    }) {
                        Text("Open Settings")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showOverlayDialog.value = false
                        // user cancelled; still mark setup complete (pins saved)
                        (activity as? PasswordSetupActivity)?.completePasswordSetup(groupId)
                    }) {
                        Text("Skip")
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
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val authTypes = listOf(
            AuthType("PIN", "4-6 digit PIN", "🔐"),
            AuthType("PASSWORD", "Alphanumeric password", "🔒"),
            AuthType("PATTERN", "Draw a pattern", "✏️"),
            AuthType("BIOMETRICS", "Fingerprint/Face ID", "👆")
        )

        authTypes.forEach { authType ->
            AuthenticationTypeCard(
                authType = authType,
                onSelected = { onAuthTypeSelected(authType.name) }
            )
        }
    }
}

data class AuthType(
    val name: String,
    val description: String,
    val icon: String
)

@Composable
fun AuthenticationTypeCard(
    authType: AuthType,
    onSelected: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp))
            .clickable { onSelected() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2a2a3e)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = authType.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9575CD)
                )
                Text(
                    text = authType.description,
                    fontSize = 12.sp,
                    color = Color(0xFFB0B0B0),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Text(
                text = authType.icon,
                fontSize = 32.sp,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

@Composable
fun PinEntryScreen(
    authType: String = "PIN",
    groupId: String = "",
    activity: ComponentActivity? = null,
    forAppName: String = "App",
    onPinConfirmed: (String) -> Unit = {},
    onBackToSelection: () -> Unit = {}
) {
    val pinInput = remember { mutableStateOf("") }
    val pinConfirm = remember { mutableStateOf("") }
    val step = remember { mutableStateOf(0) } // 0: Enter PIN, 1: Confirm PIN
    val errorMessage = remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
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
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF9575CD),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Step counter
        Text(
            text = "Step ${step.value + 1} of 2",
            fontSize = 12.sp,
            color = Color(0xFFB0B0B0),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // PIN Display
        PinDisplayBox(
            pin = if (step.value == 0) pinInput.value else pinConfirm.value,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Error message
        if (errorMessage.value.isNotEmpty()) {
            Text(
                text = errorMessage.value,
                fontSize = 12.sp,
                color = Color(0xFFFF6B6B),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Number Keyboard
        NumberKeyboard(
            onNumberClick = { number ->
                val currentPin = if (step.value == 0) pinInput.value else pinConfirm.value
                if (currentPin.length < 6) {
                    if (step.value == 0) {
                        pinInput.value = currentPin + number
                    } else {
                        pinConfirm.value = currentPin + number
                    }
                    errorMessage.value = ""
                }
            },
            onBackspace = {
                if (step.value == 0) {
                    if (pinInput.value.isNotEmpty()) {
                        pinInput.value = pinInput.value.dropLast(1)
                    }
                } else {
                    if (pinConfirm.value.isNotEmpty()) {
                        pinConfirm.value = pinConfirm.value.dropLast(1)
                    }
                }
            },
            onClear = {
                if (step.value == 0) {
                    pinInput.value = ""
                } else {
                    pinConfirm.value = ""
                }
            }
        )

        // Continue Button
        Button(
            onClick = {
                val currentPin = if (step.value == 0) pinInput.value else pinConfirm.value

                when {
                    currentPin.isEmpty() -> {
                        errorMessage.value = "Please enter a $authType"
                    }
                    currentPin.length < 4 -> {
                        errorMessage.value = "PIN must be at least 4 digits"
                    }
                    step.value == 0 -> {
                        // Move to confirmation step
                        step.value = 1
                        errorMessage.value = ""
                    }
                    step.value == 1 -> {
                        // Verify PINs match
                        if (pinInput.value == pinConfirm.value) {
                            // PINs match, proceed
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
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(top = 24.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Purple40
            )
        ) {
            Text(
                text = if (step.value == 0) "Next" else "Confirm",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
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
            .height(80.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2a2a3e)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(6) { index ->
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
            .size(16.dp)
            .background(
                color = if (isFilled) Color(0xFF9575CD) else Color(0xFF4a4a5e),
                shape = RoundedCornerShape(50)
            )
    )
}

@Composable
fun NumberKeyboard(
    onNumberClick: (String) -> Unit = {},
    onBackspace: () -> Unit = {},
    onClear: () -> Unit = {}
) {
    val numbers = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("*", "0", "#")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        numbers.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { number ->
                    if (number == "*" || number == "#") {
                        // Skip these keys or make them function keys
                        Box(modifier = Modifier.weight(1f))
                    } else {
                        KeyboardButton(
                            text = number,
                            onClick = { onNumberClick(number) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Action buttons row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onClear,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4a4a5e)
                )
            ) {
                Text("Clear", color = Color.White)
            }

            Button(
                onClick = onBackspace,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4a4a5e)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Backspace",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun KeyboardButton(
    text: String,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF2a2a3e)
        )
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF9575CD)
        )
    }
}
