package com.example.dale

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dale.ui.theme.DALETheme
import com.example.dale.ui.theme.Purple80
import com.example.dale.utils.SharedPreferencesManager
import java.security.MessageDigest

class ChangePasswordActivity : ComponentActivity() {
    private fun hashPin(pin: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(pin.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private fun verifyPin(inputPin: String, storedHash: String): Boolean {
        return hashPin(inputPin) == storedHash
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val groupId = intent.getStringExtra("GROUP_ID") ?: ""
        val groupName = intent.getStringExtra("GROUP_NAME") ?: ""
        val appPackage = intent.getStringExtra("APP_PACKAGE") ?: ""
        val isBackupRegistration = intent.getBooleanExtra("IS_BACKUP_REGISTRATION", false)

        setContent {
            DALETheme {
                ChangePasswordScreen(
                    groupId = groupId,
                    groupName = groupName,
                    appPackage = appPackage,
                    isBackupRegistration = isBackupRegistration,
                    activity = this,
                    hashPin = { pin -> this@ChangePasswordActivity.hashPin(pin) },
                    verifyPin = { input, stored -> this@ChangePasswordActivity.verifyPin(input, stored) }
                )
            }
        }
    }
}

@Composable
fun ChangePasswordScreen(
    groupId: String,
    groupName: String,
    appPackage: String,
    isBackupRegistration: Boolean,
    activity: ComponentActivity,
    hashPin: (String) -> String,
    verifyPin: (String, String) -> Boolean
) {
    val sharedPrefs = SharedPreferencesManager.getInstance(activity)
    val group = remember(groupId, groupName) {
        when {
            groupId.isNotBlank() -> sharedPrefs.getAppGroup(groupId)
            groupName.isNotBlank() -> sharedPrefs.getAllAppGroups().firstOrNull { it.groupName == groupName }
            else -> null
        }
    }

    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(if (isBackupRegistration) 2 else 1) } // 1: current, 2: new, 3: confirm
    var errorMessage by remember { mutableStateOf("") }

    val selectedLockType = remember(group, appPackage) {
        when (appPackage) {
            group?.app1PackageName -> group.app1LockType
            group?.app2PackageName -> group.app2LockType
            else -> "PIN"
        }
    }
    val isPatternMode = selectedLockType.equals("PATTERN", ignoreCase = true)
    val credentialLabel = if (isPatternMode) "Pattern" else "PIN"

    val appName = remember {
        try {
            activity.packageManager.getApplicationLabel(
                activity.packageManager.getApplicationInfo(appPackage, 0)
            ).toString()
        } catch (e: Exception) {
            appPackage
        }
    }

    Box(
        modifier = Modifier
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
                .statusBarsPadding()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color(0xFF0f3460))
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { activity.finish() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Text(
                    text = when {
                        isBackupRegistration && isPatternMode -> "Set Backup Pattern"
                        isBackupRegistration -> "Set Backup Password"
                        isPatternMode -> "Change Pattern"
                        else -> "Change Password"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = when (step) {
                        1 -> "Enter Current $credentialLabel"
                        2 -> if (isBackupRegistration) "Enter Backup $credentialLabel" else "Enter New $credentialLabel"
                        else -> "Confirm New $credentialLabel"
                    },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "for $appName",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                fun processCredentialAttempt(attempt: String) {
                    if (attempt.length < 4) {
                        errorMessage = if (isPatternMode) "Pattern must connect at least 4 dots" else "PIN must be 4 digits"
                        return
                    }

                    when (step) {
                        1 -> {
                            val storedPin = if (appPackage == group?.app1PackageName) {
                                group?.app1LockPin
                            } else {
                                group?.app2LockPin
                            }

                            if (storedPin != null && verifyPin(attempt, storedPin)) {
                                errorMessage = ""
                                currentPin = attempt
                                step = 2
                            } else {
                                errorMessage = if (isPatternMode) "Incorrect pattern" else "Incorrect PIN"
                                currentPin = ""
                            }
                        }

                        2 -> {
                            val oldPin = if (appPackage == group?.app1PackageName) {
                                group?.app1LockPin
                            } else {
                                group?.app2LockPin
                            }

                            val otherAppPin = if (appPackage == group?.app1PackageName) {
                                group?.app2LockPin
                            } else {
                                group?.app1LockPin
                            }

                            val otherAppName = if (appPackage == group?.app1PackageName) {
                                group?.app2Name
                            } else {
                                group?.app1Name
                            }

                            val isOldPin = oldPin != null && verifyPin(attempt, oldPin)
                            val isOtherAppPin = otherAppPin != null && verifyPin(attempt, otherAppPin)

                            when {
                                isOldPin -> {
                                    errorMessage = if (isPatternMode) "Same as old pattern" else "Same as old pin"
                                    newPin = ""
                                }

                                isOtherAppPin -> {
                                    errorMessage = if (isPatternMode) {
                                        "Same as $otherAppName pattern"
                                    } else {
                                        "Same as $otherAppName pin"
                                    }
                                    newPin = ""
                                }

                                else -> {
                                    errorMessage = ""
                                    newPin = attempt
                                    step = 3
                                }
                            }
                        }

                        3 -> {
                            confirmPin = attempt
                            if (newPin == confirmPin) {
                                if (group != null) {
                                    val hashedPin = hashPin(newPin)
                                    val updatedGroup = if (appPackage == group.app1PackageName) {
                                        group.copy(
                                            app1LockPin = hashedPin,
                                            app1FingerprintBiometricOnly = if (isBackupRegistration) false else group.app1FingerprintBiometricOnly
                                        )
                                    } else {
                                        group.copy(
                                            app2LockPin = hashedPin,
                                            app2FingerprintBiometricOnly = if (isBackupRegistration) false else group.app2FingerprintBiometricOnly
                                        )
                                    }
                                    sharedPrefs.saveAppGroup(updatedGroup)
                                    Toast.makeText(
                                        activity,
                                        when {
                                            isBackupRegistration && isPatternMode -> "Backup pattern set successfully"
                                            isBackupRegistration -> "Backup password set successfully"
                                            isPatternMode -> "Pattern changed successfully"
                                            else -> "Password changed successfully"
                                        },
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    activity.finish()
                                }
                            } else {
                                errorMessage = if (isPatternMode) "Patterns don't match" else "PINs don't match"
                                confirmPin = ""
                            }
                        }
                    }
                }

                if (isPatternMode) {
                    Spacer(modifier = Modifier.height(78.dp))

                    PatternChangePad(
                        onPatternDrawn = { pattern ->
                            errorMessage = ""
                            processCredentialAttempt(pattern)
                        }
                    )

                    Text(
                        text = "Draw a pattern with at least 4 dots",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                } else {
                    PinDisplay(
                        pin = when (step) {
                            1 -> currentPin
                            2 -> newPin
                            else -> confirmPin
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Error Message
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFFF5252),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (!isPatternMode) {
                    NumberPad(
                        onNumberClick = { number ->
                            when (step) {
                                1 -> {
                                    if (currentPin.length < 4) {
                                        currentPin += number
                                        if (currentPin.length == 4) {
                                            processCredentialAttempt(currentPin)
                                        }
                                    }
                                }

                                2 -> {
                                    if (newPin.length < 4) {
                                        newPin += number
                                        if (newPin.length == 4) {
                                            processCredentialAttempt(newPin)
                                        }
                                    }
                                }

                                3 -> {
                                    if (confirmPin.length < 4) {
                                        confirmPin += number
                                        if (confirmPin.length == 4) {
                                            processCredentialAttempt(confirmPin)
                                        }
                                    }
                                }
                            }
                        },
                        onBackspace = {
                            when (step) {
                                1 -> if (currentPin.isNotEmpty()) currentPin = currentPin.dropLast(1)
                                2 -> if (newPin.isNotEmpty()) newPin = newPin.dropLast(1)
                                3 -> if (confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1)
                            }
                            errorMessage = ""
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PatternChangePad(
    onPatternDrawn: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2a2a3e))
    ) {
        PatternLockPad(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            onPatternDrawn = onPatternDrawn
        )
    }
}

@Composable
fun PinDisplay(pin: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(
                        color = if (index < pin.length) Purple80 else Color.Gray.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
                    .border(
                        width = 2.dp,
                        color = if (index < pin.length) Purple80 else Color.Gray,
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
fun NumberPad(
    onNumberClick: (String) -> Unit,
    onBackspace: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Rows 1-3
        for (row in 0..2) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (col in 1..3) {
                    val number = (row * 3 + col).toString()
                    NumberButton(
                        text = number,
                        onClick = { onNumberClick(number) }
                    )
                }
            }
        }

        // Bottom Row with 0 and backspace
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Empty space
            Box(modifier = Modifier.size(70.dp))

            // 0
            NumberButton(
                text = "0",
                onClick = { onNumberClick("0") }
            )

            // Backspace
            Button(
                onClick = onBackspace,
                modifier = Modifier.size(70.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0f3460)
                )
            ) {
                Text(
                    text = "⌫",
                    fontSize = 24.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun NumberButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(70.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF0f3460)
        )
    ) {
        Text(
            text = text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}
