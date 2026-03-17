package com.example.dale

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
                        title = "Change Password",
                        subtitle = "Update PIN for this group",
                        icon = Icons.Default.Lock,
                        onClick = { showAppSelection.value = true }
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
        title = { Text("Change Group Name", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                TextField(
                    value = nameInput,
                    onValueChange = { nameInput = it.take(MAX_GROUP_NAME_LENGTH) },
                    singleLine = true,
                    placeholder = { Text("Enter group name") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF0f3460),
                        unfocusedContainerColor = Color(0xFF0a2940),
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
                Text("Choose which app's password you want to change:")

                // App 1 Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAppSelected(group.app1PackageName) },
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0f3460)
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

                        Text(
                            text = app1Name,
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // App 2 Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAppSelected(group.app2PackageName) },
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0f3460)
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

                        Text(
                            text = app2Name,
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
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
