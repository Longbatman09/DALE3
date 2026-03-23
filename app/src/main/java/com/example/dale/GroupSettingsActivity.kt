package com.example.dale

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.dale.ui.theme.DALETheme
import com.example.dale.ui.theme.Purple80
import com.example.dale.utils.SharedPreferencesManager
import kotlinx.coroutines.delay
import java.util.Locale

class GroupSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val groupId = intent.getStringExtra("GROUP_ID") ?: ""
        val groupName = intent.getStringExtra("GROUP_NAME") ?: ""

        setContent {
            DALETheme {
                GroupSettingsScreen(
                    groupId = groupId,
                    groupName = groupName,
                    activity = this
                )
            }
        }
    }
}

@Composable
fun GroupSettingsScreen(
    groupId: String,
    groupName: String,
    activity: ComponentActivity
) {
    val sharedPrefs = SharedPreferencesManager.getInstance(activity)
    var group by remember(groupId, groupName) { mutableStateOf(
        when {
            groupId.isNotBlank() -> sharedPrefs.getAppGroup(groupId)
            groupName.isNotBlank() -> sharedPrefs.getAllAppGroups().firstOrNull { it.groupName == groupName }
            else -> null
        }
    ) }

    val currentGroup = group

    val resolvedGroupName = when {
        !currentGroup?.groupName.isNullOrBlank() -> currentGroup?.groupName.orEmpty()
        groupName.isNotBlank() -> groupName
        else -> "Unknown Group"
    }

    val showAppSelection = remember { mutableStateOf(false) }
    val showDeleteConfirmation = remember { mutableStateOf(false) }
    val showDestroyingLoader = remember { mutableStateOf(false) }
    val showRenameDialog = remember { mutableStateOf(false) }
    val showFingerprintDialog = remember { mutableStateOf(false) }
    val groupUsesPattern = remember(currentGroup) {
        val app1Type = currentGroup?.app1LockType?.uppercase(Locale.ROOT)
        val app2Type = currentGroup?.app2LockType?.uppercase(Locale.ROOT)
        app1Type == "PATTERN" || app2Type == "PATTERN"
    }
    val hasFingerprintSensor = remember {
        activity.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
    }
    val isFingerprintAvailable = remember {
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
                    text = "Group Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Text(
                text = resolvedGroupName,
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 4.dp)
            )

            HorizontalDivider(
                color = Color(0xFF0f3460),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Settings Options
            if (currentGroup != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Change Password Option
                    SettingsCard(
                        title = if (groupUsesPattern) "Change Pattern" else "Change Password",
                        subtitle = if (groupUsesPattern) {
                            "Update pattern for this group"
                        } else {
                            "Update PIN for this group"
                        },
                        icon = Icons.Default.Lock,
                        onClick = { showAppSelection.value = true }
                    )

                    SettingsCard(
                        title = "Fingerprint Unlock",
                        subtitle = when {
                            !hasFingerprintSensor -> "Fingerprint sensor not available on this device"
                            !isFingerprintAvailable -> "Add a fingerprint in device settings first"
                            else -> "Enable fingerprint unlock per app"
                        },
                        icon = Icons.Default.Settings,
                        enabled = hasFingerprintSensor,
                        onClick = {
                            showFingerprintDialog.value = true
                        }
                    )

                    SettingsCard(
                        title = "Change Group Name",
                        subtitle = "Rename this group",
                        icon = Icons.Default.Edit,
                        onClick = { showRenameDialog.value = true }
                    )

                    // App Logs Option
                    SettingsCard(
                        title = "App Logs",
                        subtitle = "Activity logs & usage statistics",
                        icon = Icons.Default.Settings,
                        onClick = {
                            val intent = Intent(activity, AppLogsActivity::class.java)
                            intent.putExtra("GROUP_ID", currentGroup.id)
                            intent.putExtra("GROUP_NAME", currentGroup.groupName)
                            activity.startActivity(intent)
                        }
                    )

                    // Customisation Option
                    SettingsCard(
                        title = "Customisation",
                        subtitle = "Screen lock type and appearance",
                        icon = Icons.Default.Settings,
                        onClick = {
                            val intent = Intent(activity, CustomisationActivity::class.java)
                            intent.putExtra("GROUP_ID", currentGroup.id)
                            intent.putExtra("GROUP_NAME", currentGroup.groupName)
                            activity.startActivity(intent)
                        }
                    )

                    // Delete Group Option
                    SettingsCard(
                        title = "Delete Group",
                        subtitle = "Remove this group permanently",
                        icon = Icons.Default.Delete,
                        onClick = { showDeleteConfirmation.value = true },
                        iconTint = Color(0xFFFF5252)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Group data not found. Please go back and open the group again.",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // App Selection Dialog
        if (showAppSelection.value && currentGroup != null) {
            AppSelectionDialog(
                group = currentGroup,
                activity = activity,
                onDismiss = { showAppSelection.value = false },
                onAppSelected = { selectedApp ->
                    showAppSelection.value = false
                    // Navigate to password change screen
                    val intent = Intent(activity, ChangePasswordActivity::class.java)
                    intent.putExtra("GROUP_ID", currentGroup.id)
                    intent.putExtra("GROUP_NAME", currentGroup.groupName)
                    intent.putExtra("APP_PACKAGE", selectedApp)
                    activity.startActivity(intent)
                }
            )
        }

        if (showRenameDialog.value && currentGroup != null) {
            RenameGroupDialog(
                currentGroup = currentGroup,
                sharedPrefs = sharedPrefs,
                onDismiss = { showRenameDialog.value = false },
                onRenamed = { updatedGroup ->
                    group = updatedGroup
                    showRenameDialog.value = false
                }
            )
        }

        if (showFingerprintDialog.value && currentGroup != null) {
            FingerprintSelectionDialog(
                currentGroup = currentGroup,
                sharedPrefs = sharedPrefs,
                activity = activity,
                isFingerprintAvailable = isFingerprintAvailable,
                onDismiss = { showFingerprintDialog.value = false },
                onSaved = { updatedGroup ->
                    group = updatedGroup
                    showFingerprintDialog.value = false
                }
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteConfirmation.value) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation.value = false },
                title = {
                    Text(
                        text = "Delete Group?",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text("Are you sure you want to delete the group \"$resolvedGroupName\"? This action cannot be undone.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmation.value = false
                            showDestroyingLoader.value = true
                        }
                    ) {
                        Text("Delete", color = Color(0xFFFF5252))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation.value = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Destroying Loader
        AnimatedVisibility(
            visible = showDestroyingLoader.value,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            DestroyingLoader(
                onComplete = {
                    sharedPrefs.deleteAppGroup(group?.id ?: groupId)
                    activity.finish()
                }
            )
        }
    }
}

@Composable
fun FingerprintSelectionDialog(
    currentGroup: AppGroup,
    sharedPrefs: SharedPreferencesManager,
    activity: ComponentActivity,
    isFingerprintAvailable: Boolean,
    onDismiss: () -> Unit,
    onSaved: (AppGroup) -> Unit
) {
    var app1Enabled by remember(currentGroup.id) { mutableStateOf(currentGroup.app1FingerprintEnabled) }
    var app2Enabled by remember(currentGroup.id) { mutableStateOf(currentGroup.app2FingerprintEnabled) }
    var app1BiometricOnly by remember(currentGroup.id) { mutableStateOf(currentGroup.app1FingerprintBiometricOnly) }
    var app2BiometricOnly by remember(currentGroup.id) { mutableStateOf(currentGroup.app2FingerprintBiometricOnly) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF03193B),
        title = {
            Text(
                text = "Fingerprint Unlock",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Android does not expose individual fingerprint identities to apps. Enable biometric unlock separately for each app.",
                    color = Color(0xFFB0B0B0),
                    fontSize = 12.sp
                )

                if (!isFingerprintAvailable) {
                    Text(
                        text = "No biometrics enrolled on this device.",
                        color = Color(0xFFFFB74D),
                        fontSize = 12.sp
                    )

                    TextButton(onClick = { openBiometricEnrollmentSettings(activity) }) {
                        Text("Register Biometric")
                    }
                }

                Text(
                    text = "Policy: Biometric only OR Biometric + PIN fallback",
                    color = Color(0xFFB0B0B0),
                    fontSize = 12.sp
                )

                FingerprintPolicyRow(
                    appName = currentGroup.app1Name,
                    enabled = app1Enabled,
                    biometricOnly = app1BiometricOnly,
                    controlsEnabled = isFingerprintAvailable,
                    onEnabledChange = {
                        app1Enabled = it
                        if (!it) app1BiometricOnly = false
                    },
                    onBiometricOnlyChange = { app1BiometricOnly = it }
                )

                FingerprintPolicyRow(
                    appName = currentGroup.app2Name,
                    enabled = app2Enabled,
                    biometricOnly = app2BiometricOnly,
                    controlsEnabled = isFingerprintAvailable,
                    onEnabledChange = {
                        app2Enabled = it
                        if (!it) app2BiometricOnly = false
                    },
                    onBiometricOnlyChange = { app2BiometricOnly = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val updated = currentGroup.copy(
                        app1FingerprintEnabled = app1Enabled,
                        app2FingerprintEnabled = app2Enabled,
                        app1FingerprintBiometricOnly = app1Enabled && app1BiometricOnly,
                        app2FingerprintBiometricOnly = app2Enabled && app2BiometricOnly
                    )
                    sharedPrefs.saveAppGroup(updated)
                    onSaved(updated)
                }
            ) {
                Text("Save")
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
private fun FingerprintPolicyRow(
    appName: String,
    enabled: Boolean,
    biometricOnly: Boolean,
    controlsEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
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
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    enabled = controlsEnabled
                )
            }

            if (enabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (biometricOnly) "Biometric only" else "Biometric + PIN fallback",
                        color = Color(0xFFB8C7E0),
                        fontSize = 12.sp
                    )
                    Switch(
                        checked = biometricOnly,
                        onCheckedChange = onBiometricOnlyChange,
                        enabled = controlsEnabled
                    )
                }
            }
        }
    }
}

private fun openBiometricEnrollmentSettings(activity: ComponentActivity) {
    try {
        val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
            putExtra(
                Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.BIOMETRIC_STRONG
            )
        }
        activity.startActivity(enrollIntent)
    } catch (_: Exception) {
        activity.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
    }
}

private const val MAX_GROUP_NAME_LENGTH = 30

@Composable
fun RenameGroupDialog(
    currentGroup: AppGroup,
    sharedPrefs: SharedPreferencesManager,
    onDismiss: () -> Unit,
    onRenamed: (AppGroup) -> Unit
) {
    var nameInput by remember(currentGroup.id, currentGroup.groupName) {
        mutableStateOf(currentGroup.groupName)
    }

    val trimmedName = nameInput.trim()
    val normalizedName = trimmedName.lowercase(Locale.ROOT)
    val existingNames = remember(currentGroup.id) {
        sharedPrefs.getAllAppGroups()
            .asSequence()
            .filter { it.id != currentGroup.id }
            .mapNotNull { it.groupName.trim().takeIf { name -> name.isNotEmpty() }?.lowercase(Locale.ROOT) }
            .toSet()
    }

    val isBlank = trimmedName.isEmpty()
    val isDuplicate = normalizedName in existingNames
    val isLengthValid = trimmedName.length <= MAX_GROUP_NAME_LENGTH
    val isChanged = trimmedName != currentGroup.groupName.trim()
    val canSave = !isBlank && !isDuplicate && isLengthValid && isChanged

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF03193B),
        title = {
            Text(
                "Change Group Name",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column {
                TextField(
                    value = nameInput,
                    onValueChange = { nameInput = it.take(MAX_GROUP_NAME_LENGTH) },
                    singleLine = true,
                    placeholder = { Text("Enter group name") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFB3C5DC),
                        unfocusedContainerColor = Color(0xFF062752),
                        focusedTextColor = Color(0xFFE0E0E0),
                        unfocusedTextColor = Color(0xFFB0B0B0),
                        cursorColor = Purple80
                    )
                )

                Text(
                    text = "${trimmedName.length}/$MAX_GROUP_NAME_LENGTH",
                    color = Color(0xFFB0B0B0),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )

                val errorText = when {
                    isBlank -> "Group name cannot be blank"
                    isDuplicate -> "Group name already exists"
                    !isLengthValid -> "Maximum 30 characters"
                    else -> null
                }

                if (errorText != null) {
                    Text(
                        text = errorText,
                        color = Color(0xFFFF6B6B),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    val updatedGroup = currentGroup.copy(groupName = trimmedName)
                    sharedPrefs.saveAppGroup(updatedGroup)
                    onRenamed(updatedGroup)
                }
            ) {
                Text("Save")
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
fun SettingsCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    iconTint: Color = Purple80
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) Color(0xFF0f3460) else Color(0xFF0f3460).copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (enabled) iconTint else Color.Gray,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) Color.White else Color.Gray
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = if (enabled) Color.Gray else Color.DarkGray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun AppSelectionDialog(
    group: AppGroup,
    activity: ComponentActivity,
    onDismiss: () -> Unit,
    onAppSelected: (String) -> Unit
) {
    val app1BiometricOnly = group.app1FingerprintEnabled && group.app1FingerprintBiometricOnly
    val app2BiometricOnly = group.app2FingerprintEnabled && group.app2FingerprintBiometricOnly

    // Load app icons and names
    val app1Icon = remember {
        try {
            activity.packageManager.getApplicationIcon(group.app1PackageName)
        } catch (e: Exception) {
            null
        }
    }

    val app2Icon = remember {
        try {
            activity.packageManager.getApplicationIcon(group.app2PackageName)
        } catch (e: Exception) {
            null
        }
    }

    val app1Name = remember {
        try {
            activity.packageManager.getApplicationLabel(
                activity.packageManager.getApplicationInfo(group.app1PackageName, 0)
            ).toString()
        } catch (e: Exception) {
            group.app1PackageName
        }
    }

    val app2Name = remember {
        try {
            activity.packageManager.getApplicationLabel(
                activity.packageManager.getApplicationInfo(group.app2PackageName, 0)
            ).toString()
        } catch (e: Exception) {
            group.app2PackageName
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select App",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Choose which app lock you want to change:")

                // App 1 Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !app1BiometricOnly) {
                            onAppSelected(group.app1PackageName)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (app1BiometricOnly) Color(0xFF1b2a40) else Color(0xFF0f3460)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (app1Icon != null) {
                            Image(
                                bitmap = app1Icon.toBitmap().asImageBitmap(),
                                contentDescription = app1Name,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = app1Name,
                                fontSize = 16.sp,
                                color = if (app1BiometricOnly) Color(0xFFB0BEC5) else Color.White,
                                fontWeight = FontWeight.Medium
                            )
                            if (app1BiometricOnly) {
                                Text(
                                    text = "Biometrics only Policy",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }

                // App 2 Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !app2BiometricOnly) {
                            onAppSelected(group.app2PackageName)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (app2BiometricOnly) Color(0xFF1b2a40) else Color(0xFF0f3460)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (app2Icon != null) {
                            Image(
                                bitmap = app2Icon.toBitmap().asImageBitmap(),
                                contentDescription = app2Name,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = app2Name,
                                fontSize = 16.sp,
                                color = if (app2BiometricOnly) Color(0xFFB0BEC5) else Color.White,
                                fontWeight = FontWeight.Medium
                            )
                            if (app2BiometricOnly) {
                                Text(
                                    text = "Biometrics only Policy",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DestroyingLoader(onComplete: () -> Unit) {
    var dotCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        // Animate dots
        while (true) {
            delay(400)
            dotCount = (dotCount + 1) % 4
        }
    }

    LaunchedEffect(Unit) {
        delay(2000)
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Destroying" + ".".repeat(dotCount),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
