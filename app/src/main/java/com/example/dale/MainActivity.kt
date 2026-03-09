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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.dale.ui.theme.DALETheme
import com.example.dale.ui.theme.Purple40
import com.example.dale.ui.theme.Purple80
import com.example.dale.utils.SharedPreferencesManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    override fun onBackPressed() {
        // Close DALE completely and return to home screen when back is pressed
        finishAndRemoveTask()
    }
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier, activity: ComponentActivity? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = SharedPreferencesManager.getInstance(activity as ComponentActivity)
    val allGroups = remember { mutableStateOf(sharedPrefs.getAllAppGroups()) }
    var refreshTrigger by remember { mutableStateOf(0) }

    val showUsagePermissionDialog = remember { mutableStateOf(false) }
    val showOverlayPermissionDialog = remember { mutableStateOf(false) }
    val showBatteryOptimizationDialog = remember { mutableStateOf(false) }
    var isMenuOpen by remember { mutableStateOf(false) }
    var showDestroyConfirmation by remember { mutableStateOf(false) }
    var showDestroyingScreen by remember { mutableStateOf(false) }

    // Refresh groups when screen is visible
    LaunchedEffect(refreshTrigger) {
        allGroups.value = sharedPrefs.getAllAppGroups()
    }

    // Add a listener to refresh when activity resumes
    DisposableEffect(Unit) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
            }
        }
        activity?.lifecycle?.addObserver(lifecycleObserver)
        onDispose {
            activity?.lifecycle?.removeObserver(lifecycleObserver)
        }
    }

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

        // Check battery optimization
        val isBatteryOptimizationDisabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }

        if (!hasUsageAccess) {
            showUsagePermissionDialog.value = true
        } else if (!hasOverlayPermission) {
            showOverlayPermissionDialog.value = true
        } else if (!isBatteryOptimizationDisabled) {
            showBatteryOptimizationDialog.value = true
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

    // Battery Optimization Dialog
    if (showBatteryOptimizationDialog.value) {
        AlertDialog(
            onDismissRequest = { showBatteryOptimizationDialog.value = false },
            title = { Text("Disable Battery Optimization") },
            text = { Text("DALE needs to be excluded from battery optimization to work reliably. This ensures the lock screen appears consistently when you open protected apps.") },
            confirmButton = {
                TextButton(onClick = {
                    showBatteryOptimizationDialog.value = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    }
                }) {
                    Text("Disable Optimization")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatteryOptimizationDialog.value = false }) {
                    Text("Later")
                }
            }
        )
    }

    // Destroy Confirmation Dialog
    if (showDestroyConfirmation) {
        AlertDialog(
            onDismissRequest = { showDestroyConfirmation = false },
            title = {
                Text(
                    "Destroy DALE?",
                    color = Color(0xFFFF5252),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("This will permanently delete all groups and app data. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDestroyConfirmation = false
                        showDestroyingScreen = true
                    }
                ) {
                    Text("DESTROY", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDestroyConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Destroying Loading Screen
    if (showDestroyingScreen) {
        DestroyingLoadingScreen(
            onComplete = {
                // Clear all app data
                sharedPrefs.clearAllData()

                // Navigate back to welcome screen
                activity?.let {
                    val intent = Intent(it, WelcomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    it.startActivity(intent)
                    it.finish()
                }
            }
        )
        return // Don't render the rest of the UI
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
                    onClick = { isMenuOpen = !isMenuOpen },
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

            // "All Groups" header section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            ) {
                Text(
                    text = "All Groups",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Purple80,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Divider line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFF2a4a6a))
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
                                // Open GroupSettingsActivity
                                val intent = Intent(activity, GroupSettingsActivity::class.java)
                                intent.putExtra("GROUP_ID", group.id)
                                intent.putExtra("GROUP_NAME", group.groupName)
                                activity.startActivity(intent)
                            },
                            context = activity
                        )
                    }
                }
            }
        }

        // Semi-transparent overlay when menu is open
        if (isMenuOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { isMenuOpen = false }
                    .zIndex(1f)
            )
        }

        // Sliding Menu
        AnimatedVisibility(
            visible = isMenuOpen,
            enter = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(durationMillis = 300)
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(durationMillis = 300)
            ),
            modifier = Modifier.zIndex(2f)
        ) {
            SideMenu(
                onClose = { isMenuOpen = false },
                onMenuItemClick = { menuItem ->
                    if (menuItem == "Destroy") {
                        showDestroyConfirmation = true
                    }
                    isMenuOpen = false
                }
            )
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
                .padding(16.dp)
                .zIndex(0f),
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
    onClick: () -> Unit,
    context: Context
) {
    // Load app icons
    val app1Icon = remember(app1Package) {
        try {
            context.packageManager.getApplicationIcon(app1Package)
        } catch (e: Exception) {
            null
        }
    }

    val app2Icon = remember(app2Package) {
        try {
            context.packageManager.getApplicationIcon(app2Package)
        } catch (e: Exception) {
            null
        }
    }

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

            // App Icons Display
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App 1 Icon
                if (app1Icon != null) {
                    Image(
                        bitmap = app1Icon.toBitmap().asImageBitmap(),
                        contentDescription = "App 1 Icon",
                        modifier = Modifier.size(32.dp)
                    )
                }

                // App 2 Icon
                if (app2Icon != null) {
                    Image(
                        bitmap = app2Icon.toBitmap().asImageBitmap(),
                        contentDescription = "App 2 Icon",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SideMenu(
    onClose: () -> Unit,
    onMenuItemClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(250.dp)
            .background(Color(0xFF0f3460))
            .shadow(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp)
        ) {
            // Menu Header
            Text(
                text = "Menu",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )

            Divider(
                color = Color.White.copy(alpha = 0.2f),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Spacer(modifier = Modifier.weight(1f))

            // Destroy DALE Button at bottom
            MenuItem(
                text = "Destroy DALE",
                icon = "🗑️",
                onClick = { onMenuItemClick("Destroy") },
                isDestructive = true
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun MenuItem(
    text: String,
    icon: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isDestructive) Color(0xFFD32F2F).copy(alpha = 0.15f) else Color.Transparent
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            fontSize = 20.sp,
            modifier = Modifier.padding(end = 16.dp)
        )
        Text(
            text = text,
            fontSize = 16.sp,
            color = if (isDestructive) Color(0xFFFF5252) else Color.White,
            fontWeight = if (isDestructive) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun DestroyingLoadingScreen(onComplete: () -> Unit) {
    val dotState = remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        // Animate dots
        val job = launch {
            while (true) {
                dotState.intValue = (dotState.intValue + 1) % 4
                delay(350)
            }
        }

        // Wait minimum 2 seconds
        delay(2000L)
        job.cancel()

        // Complete the destruction
        onComplete()
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
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Text(
            text = "Destroying" + ".".repeat(dotState.intValue),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFFF5252),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 14.dp)
        )

        LinearProgressIndicator(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(6.dp),
            color = Color(0xFFFF5252),
            trackColor = Color(0xFF0A2940)
        )
    }
}
