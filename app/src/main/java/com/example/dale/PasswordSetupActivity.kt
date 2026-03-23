package com.example.dale

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
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
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.foundation.Image
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.res.painterResource
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
        val appGroup = sharedPrefsManager.getAppGroupForSetup(groupId)
        if (appGroup != null) {
            val hashedPin = hashPin(pin)
            val newGroup = when (appIndex) {
                1 -> appGroup.copy(app1LockPin = hashedPin, isLocked = true)
                2 -> appGroup.copy(app2LockPin = hashedPin, isLocked = true)
                else -> appGroup
            }
            sharedPrefsManager.saveAppGroupForSetup(newGroup)
        }
    }

    // Save hashed credentials for both apps at the final step (only finalize at the end)
    fun saveBothCredentials(
        groupId: String,
        app1AuthType: String,
        app1RawCredential: String,
        app2AuthType: String,
        app2RawCredential: String
    ) {
        val sharedPrefsManager = SharedPreferencesManager.getInstance(this)
        val appGroup = sharedPrefsManager.getAppGroupForSetup(groupId)
        if (appGroup != null) {
            val app1HashedCredential = hashPin(app1RawCredential)
            val app2HashedCredential = hashPin(app2RawCredential)
            val app1NormalizedType = app1AuthType.uppercase()
            val app2NormalizedType = app2AuthType.uppercase()
            
            val newGroup = appGroup.copy(
                app1LockPin = app1HashedCredential,
                app1LockType = app1NormalizedType,
                app2LockPin = app2HashedCredential,
                app2LockType = app2NormalizedType,
                isLocked = true
            )
            sharedPrefsManager.saveAppGroupForSetup(newGroup)
        }
    }

    // Save hashed credential for specific app index (1 or 2) - DEPRECATED - use saveBothCredentials instead
    fun saveCredentialForApp(groupId: String, appIndex: Int, authType: String, rawCredential: String) {
        // This method is now deprecated. Credentials are saved at the final step only.
    }

    fun saveBiometricForApps(
        groupId: String,
        app1Enabled: Boolean,
        app2Enabled: Boolean,
        app1BiometricOnly: Boolean,
        app2BiometricOnly: Boolean,
        app1BackupType: String = "PIN",
        app2BackupType: String = "PIN",
        app1BackupPin: String = "",
        app2BackupPin: String = ""
    ) {
        val sharedPrefsManager = SharedPreferencesManager.getInstance(this)
        val appGroup = sharedPrefsManager.getAppGroupForSetup(groupId)
        if (appGroup != null) {
            val resolvedBackupType = app1BackupType.ifBlank {
                app2BackupType.ifBlank { "PIN" }
            }.uppercase()

            val app1BackupFinal = if (app1Enabled && !app1BiometricOnly) {
                if (app1BackupPin.isNotEmpty()) hashPin(app1BackupPin) else appGroup.app1LockPin
            } else if (app1Enabled && app1BiometricOnly) {
                appGroup.app1LockPin
            } else {
                // Non-biometric app in mixed setup uses selected app auth
                if (app1BackupPin.isNotEmpty()) hashPin(app1BackupPin) else appGroup.app1LockPin
            }

            val app2BackupFinal = if (app2Enabled && !app2BiometricOnly) {
                if (app2BackupPin.isNotEmpty()) hashPin(app2BackupPin) else appGroup.app2LockPin
            } else if (app2Enabled && app2BiometricOnly) {
                appGroup.app2LockPin
            } else {
                // Non-biometric app in mixed setup uses selected app auth
                if (app2BackupPin.isNotEmpty()) hashPin(app2BackupPin) else appGroup.app2LockPin
            }

            val app1TypeFinal = when {
                app1Enabled && app1BiometricOnly -> "BIOMETRIC"
                app1Enabled && !app1BiometricOnly -> resolvedBackupType
                else -> resolvedBackupType
            }
            val app2TypeFinal = when {
                app2Enabled && app2BiometricOnly -> "BIOMETRIC"
                app2Enabled && !app2BiometricOnly -> resolvedBackupType
                else -> resolvedBackupType
            }

            val newGroup = appGroup.copy(
                app1FingerprintEnabled = app1Enabled,
                app2FingerprintEnabled = app2Enabled,
                app1FingerprintBiometricOnly = app1Enabled && app1BiometricOnly,
                app2FingerprintBiometricOnly = app2Enabled && app2BiometricOnly,
                app1LockType = app1TypeFinal,
                app2LockType = app2TypeFinal,
                app1LockPin = app1BackupFinal,
                app2LockPin = app2BackupFinal,
                isLocked = true
            )
            sharedPrefsManager.saveAppGroupForSetup(newGroup)
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
        val appGroup = sharedPrefsManager.getAppGroupForSetup(groupId)

        if (appGroup != null) {
            // mark locked if any pin exists
            val locked = appGroup.app1LockPin.isNotEmpty() || appGroup.app2LockPin.isNotEmpty()
            sharedPrefsManager.saveAppGroupForSetup(appGroup.copy(isLocked = locked))
            sharedPrefsManager.commitPendingAppGroup(groupId)
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
    val showBiometricAppsDialog = remember { mutableStateOf(false) }
    val showBiometricBackupDialog = remember { mutableStateOf(false) }
    val showBiometricBackupPinDialog = remember { mutableStateOf<Int?>(null) }
    val pendingCredentialApps = remember { mutableStateListOf<Int>() }
    val activeCredentialApp = remember { mutableStateOf<Int?>(null) }
    
    // Store credentials temporarily until final confirmation
    val app1Credential = remember { mutableStateOf<String?>(null) }
    val app2Credential = remember { mutableStateOf<String?>(null) }
    val app1AuthType = remember { mutableStateOf<String?>(null) }
    val app2AuthType = remember { mutableStateOf<String?>(null) }
    
    // Store the first app's credential temporarily to compare with the second
    val firstAppCredential = remember { mutableStateOf<String?>(null) }
    
    // Biometric settings
    val app1BiometricEnabled = remember { mutableStateOf(false) }
    val app2BiometricEnabled = remember { mutableStateOf(false) }
    val app1BiometricOnly = remember { mutableStateOf(true) }
    val app2BiometricOnly = remember { mutableStateOf(true) }
    val groupBackupType = remember { mutableStateOf<String?>(null) }
    val app1BackupType = remember { mutableStateOf("PIN") }
    val app2BackupType = remember { mutableStateOf("PIN") }
    val app1BackupPin = remember { mutableStateOf("") }
    val app2BackupPin = remember { mutableStateOf("") }

    fun finalizeBiometricFlow() {
        (activity as? PasswordSetupActivity)?.saveBiometricForApps(
            groupId = groupId,
            app1Enabled = app1BiometricEnabled.value,
            app2Enabled = app2BiometricEnabled.value,
            app1BiometricOnly = app1BiometricOnly.value,
            app2BiometricOnly = app2BiometricOnly.value,
            app1BackupType = groupBackupType.value ?: app1BackupType.value,
            app2BackupType = groupBackupType.value ?: app1BackupType.value,
            app1BackupPin = app1BackupPin.value,
            app2BackupPin = app2BackupPin.value
        )
        if (!overlayAllowed.value) {
            showOverlayDialog.value = true
        } else {
            (activity as? PasswordSetupActivity)?.proceedToOverlayPermission(groupId)
        }
    }

    // resetKey forces PinEntryScreen recomposition when changed
    val resetKey = remember { mutableStateOf(0) }

    // Load app names for UI
    val app1Name = remember { mutableStateOf("App 1") }
    val app2Name = remember { mutableStateOf("App 2") }
    val groupName = remember { mutableStateOf("Group") }

    // Load actual names from SharedPreferences
    val sharedPrefs = SharedPreferencesManager.getInstance(activity as ComponentActivity)
    val appGroup = remember { mutableStateOf(sharedPrefs.getAppGroupForSetup(groupId)) }
    appGroup.value?.let { group ->
        if (group.app1Name.isNotEmpty()) app1Name.value = group.app1Name
        if (group.app2Name.isNotEmpty()) app2Name.value = group.app2Name
        if (group.groupName.isNotEmpty()) groupName.value = group.groupName
    }
    
    // Check biometric availability
    val hasFingerprintSensor = remember {
        activity.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
    }
    val isBiometricAvailable = remember {
        if (!hasFingerprintSensor) {
            false
        } else {
            BiometricManager.from(activity).canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.BIOMETRIC_STRONG
            ) == BiometricManager.BIOMETRIC_SUCCESS
        }
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
                            if (authType == "BIOMETRICS") {
                                showBiometricAppsDialog.value = true
                            } else {
                                selectedAuthType.value = authType
                                app1AuthType.value = authType
                                app2AuthType.value = authType
                            }
                        },
                        isBiometricAvailable = isBiometricAvailable
                    )
                } else {
                    // Show Pin entry for the current target app
                    val appName = if (targetAppIndex.value == 1) app1Name.value else app2Name.value
                    // Use key with resetKey and target index to force a fresh PinEntryScreen when switching
                    key(resetKey.value, targetAppIndex.value, selectedAuthType.value) {
                        CredentialEntryScreen(
                            authType = selectedAuthType.value ?: "PIN",
                            forAppName = appName,
                            forbiddenCredential = if (targetAppIndex.value == 2) firstAppCredential.value else null,
                            onCredentialConfirmed = { credential ->
                                val authType = selectedAuthType.value ?: "PIN"
                                
                                // Store credential temporarily
                                if (targetAppIndex.value == 1) {
                                    app1Credential.value = credential
                                    app1AuthType.value = authType
                                    app1BackupType.value = authType
                                    app2BackupType.value = authType
                                    groupBackupType.value = authType
                                    app1BackupPin.value = credential
                                    firstAppCredential.value = credential
                                    if (activeCredentialApp.value != null || pendingCredentialApps.isNotEmpty()) {
                                        if (pendingCredentialApps.isNotEmpty()) {
                                            activeCredentialApp.value = pendingCredentialApps.removeAt(0)
                                            val sharedType = groupBackupType.value
                                            if (sharedType != null) {
                                                showBiometricBackupPinDialog.value = null
                                                selectedAuthType.value = sharedType
                                                targetAppIndex.value = activeCredentialApp.value ?: 1
                                                resetKey.value = resetKey.value + 1
                                            } else {
                                                showBiometricBackupPinDialog.value = activeCredentialApp.value
                                                selectedAuthType.value = null
                                            }
                                        } else {
                                            activeCredentialApp.value = null
                                            selectedAuthType.value = null
                                            finalizeBiometricFlow()
                                        }
                                    } else {
                                        targetAppIndex.value = 2
                                        resetKey.value = resetKey.value + 1
                                    }
                                } else {
                                    // Both apps done, now save at final step
                                    app2Credential.value = credential
                                    app2AuthType.value = authType
                                    app2BackupType.value = authType
                                    app1BackupType.value = authType
                                    groupBackupType.value = authType
                                    app2BackupPin.value = credential

                                    if (activeCredentialApp.value != null || pendingCredentialApps.isNotEmpty()) {
                                        if (pendingCredentialApps.isNotEmpty()) {
                                            activeCredentialApp.value = pendingCredentialApps.removeAt(0)
                                            val sharedType = groupBackupType.value
                                            if (sharedType != null) {
                                                showBiometricBackupPinDialog.value = null
                                                selectedAuthType.value = sharedType
                                                targetAppIndex.value = activeCredentialApp.value ?: 2
                                                resetKey.value = resetKey.value + 1
                                            } else {
                                                showBiometricBackupPinDialog.value = activeCredentialApp.value
                                                selectedAuthType.value = null
                                            }
                                        } else {
                                            activeCredentialApp.value = null
                                            selectedAuthType.value = null
                                            finalizeBiometricFlow()
                                        }
                                    } else {
                                        // Save both credentials together
                                        (activity as? PasswordSetupActivity)?.saveBothCredentials(
                                            groupId = groupId,
                                            app1AuthType = app1AuthType.value ?: "PIN",
                                            app1RawCredential = app1Credential.value ?: "",
                                            app2AuthType = app2AuthType.value ?: "PIN",
                                            app2RawCredential = app2Credential.value ?: ""
                                        )

                                        // Proceed to overlay permission
                                        if (!overlayAllowed.value) {
                                            showOverlayDialog.value = true
                                        } else {
                                            (activity as? PasswordSetupActivity)?.proceedToOverlayPermission(groupId)
                                        }
                                    }
                                }
                            },
                            onBackToSelection = {
                                // go back to auth selection, clear stored credentials
                                selectedAuthType.value = null
                                targetAppIndex.value = 1
                                firstAppCredential.value = null
                                app1Credential.value = null
                                app2Credential.value = null
                                app1AuthType.value = null
                                app2AuthType.value = null
                                app1BackupPin.value = ""
                                app2BackupPin.value = ""
                                app1BackupType.value = "PIN"
                                app2BackupType.value = "PIN"
                                groupBackupType.value = null
                                pendingCredentialApps.clear()
                                activeCredentialApp.value = null
                                showBiometricBackupPinDialog.value = null
                            }
                        )
                    }
                }
            }
        }

        // Biometric Apps Selection Dialog
        if (showBiometricAppsDialog.value) {
            BiometricAppsSelectionDialog(
                app1Name = app1Name.value,
                app2Name = app2Name.value,
                app1Enabled = app1BiometricEnabled.value,
                app2Enabled = app2BiometricEnabled.value,
                onApp1Changed = { app1BiometricEnabled.value = it },
                onApp2Changed = { app2BiometricEnabled.value = it },
                onDismiss = { showBiometricAppsDialog.value = false },
                onConfirm = {
                    showBiometricAppsDialog.value = false
                    showBiometricBackupDialog.value = true
                }
            )
        }

        // Biometric Policy Selection Dialog
        if (showBiometricBackupDialog.value) {
            BiometricPolicyDialog(
                app1Name = app1Name.value,
                app2Name = app2Name.value,
                app1Enabled = app1BiometricEnabled.value,
                app2Enabled = app2BiometricEnabled.value,
                app1BiometricOnly = app1BiometricOnly.value,
                app2BiometricOnly = app2BiometricOnly.value,
                onApp1BiometricOnlyChanged = { app1BiometricOnly.value = it },
                onApp2BiometricOnlyChanged = { app2BiometricOnly.value = it },
                onDismiss = { showBiometricBackupDialog.value = false },
                onConfirm = {
                    showBiometricBackupDialog.value = false
                    pendingCredentialApps.clear()
                    groupBackupType.value = null
                    app1BackupType.value = "PIN"
                    app2BackupType.value = "PIN"

                    // Ask auth setup for apps that are non-biometric OR biometric+backup
                    val app1NeedsCredential = !app1BiometricEnabled.value || !app1BiometricOnly.value
                    val app2NeedsCredential = !app2BiometricEnabled.value || !app2BiometricOnly.value

                    if (app1NeedsCredential) pendingCredentialApps.add(1)
                    if (app2NeedsCredential) pendingCredentialApps.add(2)

                    if (pendingCredentialApps.isNotEmpty()) {
                        activeCredentialApp.value = pendingCredentialApps.removeAt(0)
                        showBiometricBackupPinDialog.value = activeCredentialApp.value
                    } else {
                        finalizeBiometricFlow()
                    }
                }
            )
        }

        // Backup PIN/PASSWORD/PATTERN Dialogs
        if (showBiometricBackupPinDialog.value == 1) {
            BiometricBackupCredentialDialog(
                groupName = groupName.value,
                onBackupTypeSelected = { backupType ->
                    app1BackupType.value = backupType
                    app2BackupType.value = backupType
                    groupBackupType.value = backupType
                    showBiometricBackupPinDialog.value = null
                    activeCredentialApp.value = 1
                    // Show PIN entry for backup
                    selectedAuthType.value = backupType
                    targetAppIndex.value = 1
                    resetKey.value = resetKey.value + 1
                }
            )
        }

        if (showBiometricBackupPinDialog.value == 2) {
            BiometricBackupCredentialDialog(
                groupName = groupName.value,
                onBackupTypeSelected = { backupType ->
                    app2BackupType.value = backupType
                    app1BackupType.value = backupType
                    groupBackupType.value = backupType
                    showBiometricBackupPinDialog.value = null
                    activeCredentialApp.value = 2
                    // Show PIN entry for backup
                    selectedAuthType.value = backupType
                    targetAppIndex.value = 2
                    resetKey.value = resetKey.value + 1
                }
            )
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
    onAuthTypeSelected: (String) -> Unit = {},
    isBiometricAvailable: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val authTypes = listOf(
            AuthType("PIN", "4 digit PIN", "PIN", iconResourceId = R.drawable.ic_pin, enabled = true),
            AuthType("PASSWORD", "Alphanumeric password", "PWD", iconResourceId = R.drawable.ic_pwd, enabled = true),
            AuthType("PATTERN", "Draw a pattern", "PAT", iconResourceId = R.drawable.ic_pat, enabled = true),
            AuthType("BIOMETRICS", "Fingerprint/Face ID", "BIO", iconResourceId = R.drawable.ic_bio, enabled = isBiometricAvailable)
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
    val iconResourceId: Int = 0,
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
            .background(
                if (authType.enabled) Color(0xFF2a2a3e) else Color(0xFF1a1a1a)
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = if (authType.enabled) Color(0xFF2a2a3e) else Color(0xFF1a1a1a))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = authType.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (authType.enabled) Color(0xFF9575CD) else Color(0xFF555555)
                )
                Text(
                    text = if (authType.enabled) authType.description else "Device Does not support Fingerprint",
                    fontSize = 11.sp,
                    color = if (authType.enabled) Color(0xFFB0B0B0) else Color.White,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (authType.iconResourceId != 0) {
                Image(
                    painter = painterResource(id = authType.iconResourceId),
                    contentDescription = authType.name,
                    modifier = Modifier
                        .size(56.dp)
                        .padding(start = 12.dp),
                    alpha = if (authType.enabled) 1f else 0.5f
                )
            }
        }
    }
}

@Composable
fun CredentialEntryScreen(
    authType: String = "PIN",
    forAppName: String = "App",
    forbiddenCredential: String? = null,
    onCredentialConfirmed: (String) -> Unit = {},
    onBackToSelection: () -> Unit = {}
) {
    val normalizedType = authType.uppercase()
    val isPinMode = normalizedType == "PIN"
    val isPatternMode = normalizedType == "PATTERN"
    val minLength = when {
        isPinMode || isPatternMode -> 4
        else -> 6
    }
    val maxLength = when {
        isPinMode -> 4
        isPatternMode -> 9
        else -> 32
    }

    val firstInput = remember { mutableStateOf("") }
    val confirmInput = remember { mutableStateOf("") }
    val step = remember { mutableStateOf(0) }
    val errorMessage = remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val currentState = if (step.value == 0) firstInput else confirmInput
    val currentValue = currentState.value
    val isButtonEnabled = currentValue.length >= minLength

    fun advanceWithValue(inputValue: String) {
        if (inputValue.length < minLength) {
            errorMessage.value = if (isPatternMode) {
                "Pattern must connect at least 4 dots"
            } else if (isPinMode) {
                "PIN must be 4 digits"
            } else {
                "Password must be at least 6 characters"
            }
            return
        }

        when (step.value) {
            0 -> {
                if (forbiddenCredential != null && inputValue == forbiddenCredential) {
                    errorMessage.value = if (isPatternMode) {
                        "Same pattern cant be used for 2 apps"
                    } else if (isPinMode) {
                        "Same PIN cant be used for 2 apps"
                    } else {
                        "Same password cant be used for 2 apps"
                    }
                    firstInput.value = ""
                    confirmInput.value = ""
                } else {
                    firstInput.value = inputValue.take(maxLength)
                    confirmInput.value = ""
                    step.value = 1
                    errorMessage.value = ""
                }
            }

            1 -> {
                confirmInput.value = inputValue.take(maxLength)
                if (firstInput.value == confirmInput.value) {
                    onCredentialConfirmed(firstInput.value)
                } else {
                    errorMessage.value = if (isPatternMode) {
                        "Patterns do not match. Please try again."
                    } else if (isPinMode) {
                        "PINs do not match. Please try again."
                    } else {
                        "Passwords do not match. Please try again."
                    }
                    step.value = 0
                    firstInput.value = ""
                    confirmInput.value = ""
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackToSelection) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }

            Text(
                text = if (step.value == 0) {
                    "Enter ${if (isPinMode) "PIN" else if (isPatternMode) "Pattern" else "Password"} for $forAppName"
                } else {
                    "Confirm ${if (isPinMode) "PIN" else if (isPatternMode) "Pattern" else "Password"} for $forAppName"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF9575CD),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Text(
                text = "Step ${step.value + 1} of 2",
                fontSize = 12.sp,
                color = Color(0xFFB0B0B0),
                modifier = Modifier.padding(bottom = 18.dp)
            )

            if (isPinMode) {
                PinDisplayBox(
                    pin = currentValue,
                    modifier = Modifier.padding(bottom = 18.dp)
                )
            } else if (isPatternMode) {
                Spacer(modifier = Modifier.height(78.dp))

                PatternCredentialBox(
                    onPatternDrawn = { drawnPattern ->
                        if (step.value == 0) {
                            firstInput.value = drawnPattern.take(maxLength)
                        } else {
                            confirmInput.value = drawnPattern.take(maxLength)
                        }
                        advanceWithValue(drawnPattern)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .padding(bottom = 16.dp)
                )

                Text(
                    text = "Draw pattern with at least 4 dots",
                    fontSize = 12.sp,
                    color = Color(0xFFB0B0B0),
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            if (errorMessage.value.isNotEmpty()) {
                Text(
                    text = errorMessage.value,
                    fontSize = 12.sp,
                    color = Color(0xFFFF6B6B),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            OutlinedTextField(
                value = currentState.value,
                onValueChange = { value ->
                    currentState.value = if (isPinMode) {
                        value.filter { it.isDigit() }.take(maxLength)
                    } else {
                        value.take(maxLength)
                    }
                },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isPinMode) KeyboardType.NumberPassword else KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                label = { Text(if (isPinMode) "4 digit PIN" else "Password (min 6 chars)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .focusRequester(focusRequester)
                    .alpha(if (isPinMode || isPatternMode) 0f else 1f)
            )

            androidx.compose.runtime.LaunchedEffect(step.value, isPinMode, isPatternMode) {
                if (isPatternMode) return@LaunchedEffect
                delay(120)
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        }

        if (!isPatternMode) {
            Button(
                onClick = {
                    advanceWithValue(currentValue)
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2a2a3e))
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
                repeat(4) { index -> PinDot(isFilled = index < pin.length) }
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

@Composable
fun PatternCredentialBox(
    onPatternDrawn: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(330.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2a2a3e))
    ) {
        PatternLockPad(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            enabled = true,
            onPatternDrawn = onPatternDrawn
        )
    }
}

@Composable
fun BiometricAppsSelectionDialog(
    app1Name: String,
    app2Name: String,
    app1Enabled: Boolean,
    app2Enabled: Boolean,
    onApp1Changed: (Boolean) -> Unit,
    onApp2Changed: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF03193B),
        title = {
            Text(
                text = "Enable Biometric for Apps",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Select which apps to protect with biometric authentication:",
                    color = Color(0xFFB0B0B0),
                    fontSize = 12.sp
                )

                // App 1 Toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2A54))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = app1Name, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = app1Enabled,
                            onCheckedChange = onApp1Changed
                        )
                    }
                }

                // App 2 Toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2A54))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = app2Name, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = app2Enabled,
                            onCheckedChange = onApp2Changed
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = app1Enabled || app2Enabled,
                onClick = onConfirm
            ) {
                Text("Next")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun BiometricPolicyDialog(
    app1Name: String,
    app2Name: String,
    app1Enabled: Boolean,
    app2Enabled: Boolean,
    app1BiometricOnly: Boolean,
    app2BiometricOnly: Boolean,
    onApp1BiometricOnlyChanged: (Boolean) -> Unit,
    onApp2BiometricOnlyChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF03193B),
        title = {
            Text(
                text = "Biometric Policy",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Choose policy: Biometric only OR Biometric + Backup (PIN/Password/Pattern)",
                    color = Color(0xFFB0B0B0),
                    fontSize = 12.sp
                )

                if (app1Enabled) {
                    BiometricPolicyRow(
                        appName = app1Name,
                        biometricOnly = app1BiometricOnly,
                        onBiometricOnlyChange = onApp1BiometricOnlyChanged
                    )
                }

                if (app2Enabled) {
                    BiometricPolicyRow(
                        appName = app2Name,
                        biometricOnly = app2BiometricOnly,
                        onBiometricOnlyChange = onApp2BiometricOnlyChanged
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Next")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun BiometricPolicyRow(
    appName: String,
    biometricOnly: Boolean,
    onBiometricOnlyChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2A54))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = appName, color = Color.White, fontWeight = FontWeight.SemiBold)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (biometricOnly) "Biometric only" else "Biometric + Backup",
                    color = Color(0xFFB8C7E0),
                    fontSize = 12.sp
                )
                Switch(
                    checked = biometricOnly,
                    onCheckedChange = onBiometricOnlyChange
                )
            }
        }
    }
}

@Composable
fun BiometricBackupCredentialDialog(
    groupName: String,
    onBackupTypeSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        containerColor = Color(0xFF03193B),
        title = {
            Text(
                text = "Backup for $groupName",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Choose authentication method for this app:",
                    color = Color(0xFFB0B0B0),
                    fontSize = 12.sp
                )

                val backupTypes = listOf(
                    AuthType("PIN", "4 digit PIN", "PIN", enabled = true),
                    AuthType("PASSWORD", "Alphanumeric password", "PWD", enabled = true),
                    AuthType("PATTERN", "Draw a pattern", "PAT", enabled = true)
                )

                backupTypes.forEach { authType ->
                    AuthenticationTypeSelectionCard(
                        authType = authType,
                        onSelected = { onBackupTypeSelected(authType.name) }
                    )
                }
            }
        },
        confirmButton = {}
    )
}
