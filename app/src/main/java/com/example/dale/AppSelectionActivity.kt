package com.example.dale

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.dale.ui.theme.DALETheme
import com.example.dale.ui.theme.Purple40
import com.example.dale.ui.theme.Purple80
import com.example.dale.utils.SharedPreferencesManager
import android.content.pm.ResolveInfo
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width

class AppSelectionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DALETheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppSelectionScreenWithLoading(
                        modifier = Modifier.padding(innerPadding),
                        activity = this
                    )
                }
            }
        }
    }

    fun getInstalledAppsPublic(): List<AppInfo> {
        val apps = mutableListOf<AppInfo>()
        val packageManager = packageManager
        val addedPackages = mutableSetOf<String>()

        // 1) Get all installed applications (include system apps)
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        for (appInfo in installedApps) {
            if (packageName == appInfo.packageName) continue

            val label = try {
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                appInfo.packageName
            }

            val iconDrawable: Drawable? = try {
                packageManager.getApplicationIcon(appInfo.packageName)
            } catch (_: Exception) {
                null
            }

            val isSystem = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0

            apps.add(
                AppInfo(
                    packageName = appInfo.packageName,
                    appName = label,
                    icon = iconDrawable,
                    isSystem = isSystem,
                    isLauncher = false
                )
            )
            addedPackages.add(appInfo.packageName)
        }

        // 2) Also include apps that are launchable (ACTION_MAIN, CATEGORY_LAUNCHER)
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos: List<ResolveInfo> = packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
        for (resolveInfo in resolveInfos) {
            val ai = resolveInfo.activityInfo ?: continue
            val pkg = ai.packageName
            if (pkg == packageName) continue
            if (addedPackages.contains(pkg)) continue

            val label = try {
                resolveInfo.loadLabel(packageManager).toString()
            } catch (_: Exception) {
                pkg
            }

            val iconDrawable: Drawable? = try {
                resolveInfo.loadIcon(packageManager)
            } catch (_: Exception) {
                null
            }

            val isSystem = try {
                val aiInfo = packageManager.getApplicationInfo(pkg, 0)
                (aiInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            } catch (_: Exception) {
                false
            }

            apps.add(
                AppInfo(
                    packageName = pkg,
                    appName = label,
                    icon = iconDrawable,
                    isSystem = isSystem,
                    isLauncher = true
                )
            )
            addedPackages.add(pkg)
        }

        // 3) Best-effort: if Island is installed, try to surface any additional launcher entries
        try {
            packageManager.getPackageInfo("com.oasisfeng.island", 0)
            val islandResolveInfos = packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
            for (resolveInfo in islandResolveInfos) {
                val ai = resolveInfo.activityInfo ?: continue
                val pkg = ai.packageName
                if (pkg == packageName) continue
                if (addedPackages.contains(pkg)) continue

                val label = try {
                    resolveInfo.loadLabel(packageManager).toString()
                } catch (_: Exception) {
                    pkg
                }

                val iconDrawable: Drawable? = try {
                    resolveInfo.loadIcon(packageManager)
                } catch (_: Exception) {
                    null
                }

                val isSystem = try {
                    val aiInfo = packageManager.getApplicationInfo(pkg, 0)
                    (aiInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                } catch (_: Exception) {
                    false
                }

                apps.add(
                    AppInfo(
                        packageName = pkg,
                        appName = label,
                        icon = iconDrawable,
                        isSystem = isSystem,
                        isLauncher = true
                    )
                )
                addedPackages.add(pkg)
            }
        } catch (_: Exception) {
            // Island not installed — nothing extra to do
        }

        return apps.distinctBy { it.packageName }.sortedBy { it.appName }
    }
}

@Composable
fun AppSelectionScreenWithLoading(
    modifier: Modifier = Modifier,
    activity: ComponentActivity? = null
) {
    val isLoading = remember { mutableStateOf(true) }
    val allApps = remember { mutableStateOf<List<AppInfo>>(emptyList()) }

    // Load apps asynchronously on first composition
    LaunchedEffect(Unit) {
        try {
            val apps = (activity as? AppSelectionActivity)?.getInstalledAppsPublic() ?: emptyList()
            allApps.value = apps
        } catch (_: Exception) {
            allApps.value = emptyList()
        } finally {
            isLoading.value = false
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
    ) {
        if (isLoading.value) {
            // Loading Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(60.dp)
                        .padding(bottom = 24.dp),
                    color = Purple40,
                    strokeWidth = 4.dp
                )

                Text(
                    text = "Loading Apps",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Purple80,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Please wait while we fetch your installed apps...",
                    fontSize = 14.sp,
                    color = Color(0xFFB0B0B0),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Main App Selection Screen
            AppSelectionScreen(
                modifier = Modifier.fillMaxSize(),
                allApps = allApps.value,
                activity = activity
            )
        }
    }
}

@Composable
fun AppSelectionScreen(
    modifier: Modifier = Modifier,
    allApps: List<AppInfo> = emptyList(),
    activity: ComponentActivity? = null
) {
    val selectionState = remember { mutableStateOf(0) }
    val app1 = remember { mutableStateOf<AppInfo?>(null) }
    val app2 = remember { mutableStateOf<AppInfo?>(null) }
    val groupName = remember { mutableStateOf("") }

    // Category filter state: 0 -> All / Installed, 1 -> System, 2 -> Launcher
    val selectedCategory = remember { mutableStateOf(0) }

    // Partition apps into categories
    val installedApps = allApps.filter { !it.isSystem }
    val systemApps = allApps.filter { it.isSystem }
    val launcherApps = allApps.filter { it.isLauncher }

    val appsToShow = when (selectedCategory.value) {
        1 -> systemApps
        2 -> launcherApps
        else -> installedApps
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
        when (selectionState.value) {
            0 -> AppSelectionStep(
                title = "SELECT APP 1",
                apps = appsToShow,
                onAppSelected = { selectedApp ->
                    app1.value = selectedApp
                    selectionState.value = 1
                },
                onBack = null,
                selectedCategory = selectedCategory.value,
                onCategoryChange = { selectedCategory.value = it }
            )
            1 -> AppSelectionStep(
                title = "SELECT APP 2",
                apps = appsToShow.filter { it.packageName != app1.value?.packageName },
                onAppSelected = { selectedApp ->
                    app2.value = selectedApp
                    selectionState.value = 2
                },
                onBack = { selectionState.value = 0 },
                selectedCategory = selectedCategory.value,
                onCategoryChange = { selectedCategory.value = it }
            )
            2 -> GroupNameScreen(
                app1 = app1.value,
                app2 = app2.value,
                groupName = groupName.value,
                onGroupNameChange = { groupName.value = it },
                onConfirm = {
                    val appGroup = AppGroup(
                        id = System.currentTimeMillis().toString(),
                        groupName = groupName.value.ifEmpty { "${app1.value?.appName} + ${app2.value?.appName}" },
                        app1PackageName = app1.value?.packageName ?: "",
                        app1Name = app1.value?.appName ?: "",
                        app2PackageName = app2.value?.packageName ?: "",
                        app2Name = app2.value?.appName ?: ""
                    )

                    SharedPreferencesManager.getInstance(activity!!).saveAppGroup(appGroup)

                    val intent = Intent(activity, LockScreenSetupActivity::class.java)
                    intent.putExtra("groupId", appGroup.id)
                    activity.startActivity(intent)
                    activity.finish()
                },
                onBack = { selectionState.value = 1 }
            )
        }
    }
}

@Composable
fun CategoryChip(text: String, selected: Boolean = false, onClick: () -> Unit = {}, fontSize: Float = 12f) {
    Card(
        modifier = Modifier
            .shadow(elevation = if (selected) 6.dp else 2.dp, shape = RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFF0f3460) else Color(0xFF0a2940)
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = Purple80,
            fontWeight = FontWeight.SemiBold,
            fontSize = fontSize.sp
        )
    }
}

@Composable
fun AppSelectionStep(
    title: String,
    apps: List<AppInfo>,
    onAppSelected: (AppInfo) -> Unit,
    onBack: (() -> Unit)? = null,
    selectedCategory: Int = 0,
    onCategoryChange: (Int) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Purple80
                    )
                }
            }
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Purple80,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            if (onBack != null) {
                IconButton(onClick = {}) {
                    // Placeholder for alignment
                }
            }
        }

        // Category chips placed below the Select title with smaller text
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoryChip(
                text = "INSTALLED APPS",
                selected = selectedCategory == 0,
                onClick = { onCategoryChange(0) },
                fontSize = 12f
            )
            CategoryChip(
                text = "SYSTEM APPS",
                selected = selectedCategory == 1,
                onClick = { onCategoryChange(1) },
                fontSize = 12f
            )
            CategoryChip(
                text = "LAUNCHER APPS",
                selected = selectedCategory == 2,
                onClick = { onCategoryChange(2) },
                fontSize = 12f
            )
        }

        // Apps List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(apps) { app ->
                AppListCard(
                    appName = app.appName,
                    packageName = app.packageName,
                    icon = app.icon,
                    onClick = { onAppSelected(app) }
                )
            }
        }
    }
}

@Composable
fun AppListCard(
    appName: String,
    packageName: String,
    icon: Drawable? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0f3460)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                if (icon != null) {
                    val imageBitmap = remember(icon) { icon.toBitmap().asImageBitmap() }
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = appName,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    )
                    SpacerWidth(12.dp)
                }

                Column(modifier = Modifier) {
                    Text(
                        text = appName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Purple80
                    )
                    Text(
                        text = packageName,
                        fontSize = 12.sp,
                        color = Color(0xFFB0B0B0)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = Purple40,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "→",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SpacerWidth(width: Dp) {
    Spacer(modifier = Modifier.width(width))
}

@Composable
fun GroupNameScreen(
    app1: AppInfo?,
    app2: AppInfo?,
    groupName: String,
    onGroupNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Purple80
                )
            }
            Text(
                text = "Name Your Group",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Purple80,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = {}) {
                // Placeholder for alignment
            }
        }

        // Selected Apps Summary
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(12.dp)
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0f3460)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Selected Apps",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Purple80,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "App 1: ",
                        fontSize = 14.sp,
                        color = Color(0xFFB0B0B0),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = app1?.appName ?: "Not selected",
                        fontSize = 14.sp,
                        color = Purple80,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "App 2: ",
                        fontSize = 14.sp,
                        color = Color(0xFFB0B0B0),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = app2?.appName ?: "Not selected",
                        fontSize = 14.sp,
                        color = Purple80,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Group Name Input
        Text(
            text = "Enter Group Name (Optional)",
            fontSize = 14.sp,
            color = Color(0xFFB0B0B0),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        TextField(
            value = groupName,
            onValueChange = onGroupNameChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            placeholder = {
                Text(
                    text = "${app1?.appName} + ${app2?.appName}",
                    color = Color(0xFF666666)
                )
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF0f3460),
                unfocusedContainerColor = Color(0xFF0a2940),
                focusedTextColor = Color(0xFFE0E0E0),
                unfocusedTextColor = Color(0xFFB0B0B0),
                cursorColor = Purple80,
                focusedIndicatorColor = Purple80,
                unfocusedIndicatorColor = Color(0xFF1a3a52)
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )

        // Confirm Button
        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(12.dp)
                ),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50)
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Confirm",
                    tint = Color.White,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Proceed to Lock Setup",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }

        Box(modifier = Modifier.height(16.dp))
    }
}
