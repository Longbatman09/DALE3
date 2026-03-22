package com.example.dale

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
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
import androidx.core.net.toUri
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.dale.ui.theme.DALETheme
import com.example.dale.ui.theme.Purple40
import com.example.dale.ui.theme.Purple80
import com.example.dale.utils.MonitorStartupHelper
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
                    MainGate(
                        modifier = Modifier.padding(innerPadding),
                        activity = this
                    )
                }
            }
        }

        onBackPressedDispatcher.addCallback(this) {
            finishAndRemoveTask()
        }
    }

    override fun onResume() {
        super.onResume()
        // Restart service when returning only if user has protection enabled.
        val prefs = SharedPreferencesManager.getInstance(this)
        if (prefs.isProtectionEnabled()) {
            MonitorStartupHelper.startMonitoringIfPossible(this)
        } else {
            MonitorStartupHelper.stopMonitoringService(this)
        }
    }
}

@Composable
fun MainGate(modifier: Modifier = Modifier, activity: ComponentActivity) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var hasAccessibility by remember { mutableStateOf(MonitorStartupHelper.isAccessibilityServiceEnabled(context)) }
    var hasOverlay by remember { mutableStateOf(MonitorStartupHelper.hasOverlayPermission(context)) }
    var hasBattery by remember { mutableStateOf(MonitorStartupHelper.isIgnoringBatteryOptimizations(context)) }
    var refreshKey by remember { mutableIntStateOf(0) }

    // Re-check all permissions each time refreshKey changes (triggered on every resume)
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshKey++
            }
        }
        activity.lifecycle.addObserver(observer)
        onDispose { activity.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(refreshKey) {
        hasAccessibility = MonitorStartupHelper.isAccessibilityServiceEnabled(context)
        hasOverlay = MonitorStartupHelper.hasOverlayPermission(context)
        hasBattery = MonitorStartupHelper.isIgnoringBatteryOptimizations(context)

        if (hasOverlay) {
            MonitorStartupHelper.startMonitoringIfPossible(context)
        }
    }

    when {
        !hasOverlay -> PermissionWallScreen(
            modifier = modifier,
            icon = "🪟",
            title = "Draw Over Other Apps",
            description = "DALE needs to display the lock screen on top of other apps.\n\nFind \"DALE\" and turn on \"Allow display over other apps\".",
            buttonText = "Open Overlay Settings",
            onAction = {
                val i = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:${context.packageName}".toUri()
                )
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(i)
            }
        )
        !hasAccessibility -> PermissionWallScreen(
            modifier = modifier,
            icon = "♿",
            title = "Enable Accessibility Service",
            description = "DALE now uses accessibility-based app detection for reliable lock triggering.\n\nOpen accessibility settings and enable DALE.",
            buttonText = "Open Accessibility Settings",
            onAction = {
                MonitorStartupHelper.openAccessibilitySettings(context)
            }
        )
        !hasBattery -> PermissionWallScreen(
            modifier = modifier,
            icon = "🔋",
            title = "Disable Battery Optimization",
            description = "Battery optimization can kill DALE's background service, making the lock screen stop working.\n\nTap the button below — you'll be taken directly to DALE's battery settings. Select \"Unrestricted\" or \"Don't optimize\".",
            buttonText = "Open Battery Settings for DALE",
            onAction = {
                // Go directly to the app-specific battery optimization page
                try {
                    val i = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = "package:${context.packageName}".toUri()
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(i)
                } catch (_: Exception) {
                    MonitorStartupHelper.openBatteryOptimizationSettings(context)
                }
            }
        )
        else -> HomeScreen(modifier = modifier, activity = activity)
    }
}

@Composable
fun PermissionWallScreen(
    modifier: Modifier = Modifier,
    icon: String,
    title: String,
    description: String,
    buttonText: String,
    onAction: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(Color(0xFF1a1a2e), Color(0xFF16213e))
                )
            )
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Icon
            Text(text = icon, fontSize = 56.sp)

            // Title
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            // Divider
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(3.dp)
                    .background(Purple80, RoundedCornerShape(2.dp))
            )

            // Description
            Text(
                text = description,
                fontSize = 14.sp,
                color = Color(0xFFB0BEC5),
                lineHeight = 22.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Purple80, RoundedCornerShape(12.dp))
                    .clickable(onClick = onAction)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = buttonText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1a1a2e)
                )
            }

            // Note
            Text(
                text = "DALE will not function correctly without this permission.",
                fontSize = 11.sp,
                color = Color(0xFF546E7A),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier, activity: ComponentActivity? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val hostActivity = activity as? ComponentActivity ?: return
    val sharedPrefs = SharedPreferencesManager.getInstance(hostActivity)
    val allGroups = remember { mutableStateOf(sharedPrefs.getAllAppGroups()) }
    var refreshTrigger by remember { mutableStateOf(0) }
    var isMenuOpen by remember { mutableStateOf(false) }
    var showDestroyConfirmation by remember { mutableStateOf(false) }
    var showDestroyingScreen by remember { mutableStateOf(false) }
    var protectionActive by remember { mutableStateOf(false) }
    var protectionEnabled by remember { mutableStateOf(sharedPrefs.isProtectionEnabled()) }
    var showProtectionDisableConfirmation by remember { mutableStateOf(false) }

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

    // Ensure service keeps running (if enabled) and expose a small status on home screen.
    LaunchedEffect(refreshTrigger, protectionEnabled) {
        protectionEnabled = sharedPrefs.isProtectionEnabled()
        protectionActive = if (protectionEnabled) {
            MonitorStartupHelper.startMonitoringIfPossible(context)
        } else {
            MonitorStartupHelper.stopMonitoringService(context)
            false
        }
    }

    // Confirmation dialog for turning protection off
    if (showProtectionDisableConfirmation) {
        AlertDialog(
            onDismissRequest = { showProtectionDisableConfirmation = false },
            title = {
                Text(
                    text = "Turn protection OFF?",
                    color = Color(0xFF2A0C0C),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("We are stopping lock-screen services. Apps in groups will no longer be protected until protection is turned back ON.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showProtectionDisableConfirmation = false
                        sharedPrefs.setProtectionEnabled(false)
                        protectionEnabled = false
                        protectionActive = false
                        MonitorStartupHelper.stopMonitoringService(context)
                    }
                ) {
                    Text("Turn OFF", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showProtectionDisableConfirmation = false }) {
                    Text("Cancel")
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
                sharedPrefs.clearAllData()
                activity?.let {
                    val intent = Intent(it, WelcomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    it.startActivity(intent)
                    it.finish()
                }
            }
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(Color(0xFF1a1a2e), Color(0xFF16213e))
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
                // Left side: Menu + protection status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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

                    Box(
                        modifier = Modifier
                            .background(
                                if (protectionActive) Color(0xFF1B5E20) else Color(0xFF7f0000),
                                RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                if (protectionEnabled) {
                                    showProtectionDisableConfirmation = true
                                } else {
                                    sharedPrefs.setProtectionEnabled(true)
                                    protectionEnabled = true
                                    protectionActive = MonitorStartupHelper.startMonitoringIfPossible(context)
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (protectionActive) "Protection ON" else "Protection OFF",
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Right side: DALE logo
                Image(
                    painter = painterResource(id = R.drawable.dale_logo),
                    contentDescription = "DALE",
                    modifier = Modifier
                        .height(23.dp)
                        .padding(end = 4.dp)
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

            HorizontalDivider(
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
