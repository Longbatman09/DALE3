package com.example.dale

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dale.ui.theme.DALETheme
import com.example.dale.ui.theme.Purple40
import com.example.dale.utils.SharedPreferencesManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if setup is completed
        val sharedPrefsManager = SharedPreferencesManager.getInstance(this)
        if (!sharedPrefsManager.isSetupCompleted()) {
            // Redirect to WelcomeActivity if setup is not completed
            val intent = Intent(this, WelcomeActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        enableEdgeToEdge()
        setContent {
            DALETheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeScreen(
                        modifier = Modifier.padding(innerPadding),
                        activity = this
                    )
                }
            }
        }

        // Start the monitoring service if permissions are granted
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val hasUsageAccess = hasUsageStatsPermission()
        val hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }

        if (hasUsageAccess && hasOverlayPermission) {
            startMonitoringService()
        }
        // If permissions are missing, they'll be shown in the HomeScreen UI
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun startMonitoringService() {
        val serviceIntent = Intent(this, AppMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    override fun onResume() {
        super.onResume()
        // Check permissions again when returning to the app
        checkAndRequestPermissions()
    }
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier, activity: ComponentActivity? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = SharedPreferencesManager.getInstance(activity as ComponentActivity)
    val allGroups = remember { mutableStateOf(sharedPrefs.getAllAppGroups()) }

    val showUsagePermissionDialog = remember { mutableStateOf(false) }
    val showOverlayPermissionDialog = remember { mutableStateOf(false) }

    // Check permissions
    LaunchedEffect(Unit) {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val hasUsageAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            ) == AppOpsManager.MODE_ALLOWED
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            ) == AppOpsManager.MODE_ALLOWED
        }

        val hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }

        if (!hasUsageAccess) {
            showUsagePermissionDialog.value = true
        } else if (!hasOverlayPermission) {
            showOverlayPermissionDialog.value = true
        }
    }

    // Usage Stats Permission Dialog
    if (showUsagePermissionDialog.value) {
        AlertDialog(
            onDismissRequest = { showUsagePermissionDialog.value = false },
            title = { Text("Usage Access Required") },
            text = { Text("DALE needs usage access permission to monitor and lock apps. Please grant this permission in the next screen.") },
            confirmButton = {
                TextButton(onClick = {
                    showUsagePermissionDialog.value = false
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    context.startActivity(intent)
                }) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUsagePermissionDialog.value = false }) {
                    Text("Later")
                }
            }
        )
    }

    // Overlay Permission Dialog
    if (showOverlayPermissionDialog.value) {
        AlertDialog(
            onDismissRequest = { showOverlayPermissionDialog.value = false },
            title = { Text("Draw Over Other Apps") },
            text = { Text("DALE needs permission to display lock screen over other apps. Please enable this in settings.") },
            confirmButton = {
                TextButton(onClick = {
                    showOverlayPermissionDialog.value = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                }) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverlayPermissionDialog.value = false }) {
                    Text("Later")
                }
            }
        )
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
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar with Menu and DALE title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color(0xFF0f3460))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Menu Icon
                IconButton(
                    onClick = { /* Menu action */ },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // DALE Title moved to right
                Text(
                    text = "DALE",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Groups List
            if (allGroups.value.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No groups created yet.\nTap + to create one.",
                        fontSize = 16.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allGroups.value) { group ->
                        GroupCard(
                            groupName = group.groupName,
                            app1Package = group.app1PackageName,
                            app2Package = group.app2PackageName,
                            isLocked = group.isLocked,
                            onClick = {
                                // Handle group click
                            }
                        )
                    }
                }
            }
        }

        // Floating Action Button (Add)
        FloatingActionButton(
            onClick = {
                // Navigate to app selection
                activity?.let {
                    val intent = Intent(it, AppSelectionActivity::class.java)
                    it.startActivity(intent)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Purple40
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Group",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun GroupCard(
    groupName: String,
    app1Package: String,
    app2Package: String,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0f3460)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = groupName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = "$app1Package + $app2Package",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Lock Status Indicator
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = if (isLocked) Color(0xFF4CAF50) else Color(0xFF757575),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isLocked) "🔒" else "🔓",
                    fontSize = 16.sp
                )
            }
        }
    }
}
