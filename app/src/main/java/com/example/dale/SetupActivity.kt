package com.example.dale

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.net.toUri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dale.ui.theme.DALETheme
import com.example.dale.ui.theme.Purple40
import com.example.dale.ui.theme.Purple80

class SetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val deviceModel = Build.MODEL
        val deviceManufacturer = Build.MANUFACTURER
        val hasDualAppSupport = hasDualAppNativeSupport()

        setContent {
            DALETheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SetupScreen(
                        modifier = Modifier.padding(innerPadding),
                        deviceModel = deviceModel,
                        deviceManufacturer = deviceManufacturer,
                        hasDualAppSupport = hasDualAppSupport,
                        onOpenDualAppSettings = { openDualAppSettings() },
                        onOpenIsland = { openIslandPlayStore() },
                        activity = this
                    )
                }
            }
        }
    }

    private fun hasDualAppNativeSupport(): Boolean {
        val supportedManufacturers = listOf(
            "xiaomi", "redmi", "poco",
            "samsung",
            "honor", "huawei",
            "oppo", "realme", "vivo"
        )
        return Build.MANUFACTURER.lowercase() in supportedManufacturers
    }

    private fun openDualAppSettings() {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val intent = when (manufacturer) {
            in listOf("xiaomi", "redmi", "poco") -> {
                // MIUI - Open App Cloner/Dual Apps
                Intent().apply {
                    component = ComponentName(
                        "com.android.settings",
                        "com.android.settings.Settings\$AppCloneListActivity"
                    )
                }
            }
            "samsung" -> {
                // Samsung - Open Dual Messenger or Clone App
                Intent().apply {
                    component = ComponentName(
                        "com.android.settings",
                        "com.android.settings.Settings"
                    )
                    putExtra(":settings:show_fragment", "com.samsung.android.settings.dualsim.DualSimSettingsFragment")
                }
            }
            "oppo" -> {
                // OPPO ColorOS - Open Dual Apps
                Intent().apply {
                    component = ComponentName(
                        "com.android.settings",
                        "com.android.settings.Settings"
                    )
                }
            }
            "vivo" -> {
                // Vivo Funtouch OS
                Intent().apply {
                    component = ComponentName(
                        "com.android.settings",
                        "com.android.settings.Settings"
                    )
                }
            }
            "realme" -> {
                // Realme UI
                Intent().apply {
                    component = ComponentName(
                        "com.android.settings",
                        "com.android.settings.Settings"
                    )
                }
            }
            "honor" -> {
                // Honor OS / EMUI
                Intent().apply {
                    component = ComponentName(
                        "com.android.settings",
                        "com.android.settings.Settings"
                    )
                }
            }
            "huawei" -> {
                // Huawei EMUI
                Intent().apply {
                    component = ComponentName(
                        "com.android.settings",
                        "com.android.settings.Settings"
                    )
                }
            }
            else -> {
                Intent(android.provider.Settings.ACTION_SETTINGS)
            }
        }

        try {
            startActivity(intent)
        } catch (_: Exception) {
            // Fallback to general settings
            startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
        }
    }

    private fun openIslandPlayStore() {
        val playStoreUrl = "https://play.google.com/store/apps/details?id=com.oasisfeng.island"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = playStoreUrl.toUri()
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW).apply {
                data = playStoreUrl.toUri()
            }
            startActivity(webIntent)
        }
    }
}

@Composable
fun SetupScreen(
    modifier: Modifier = Modifier,
    deviceModel: String = "Unknown",
    deviceManufacturer: String = "Unknown",
    hasDualAppSupport: Boolean = false,
    onOpenDualAppSettings: () -> Unit = {},
    onOpenIsland: () -> Unit = {},
    activity: ComponentActivity? = null
) {
    val showMethod1Dialog = remember { mutableStateOf(false) }

    if (showMethod1Dialog.value) {
        Method1Dialog(
            deviceManufacturer = deviceManufacturer,
            deviceModel = deviceModel,
            onDismiss = { showMethod1Dialog.value = false },
            onOpenSettings = {
                onOpenDualAppSettings()
                showMethod1Dialog.value = false
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
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Step Header
            Text(
                text = "Step 1: Creation of Dual App",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Purple80,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                textAlign = TextAlign.Left
            )

            // Device Info Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0f3460)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Device Info",
                            tint = Purple80,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 8.dp)
                        )
                        Text(
                            text = "Device Information",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Purple80
                        )
                    }

                    HorizontalDivider(color = Color(0xFF1a3a52), modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Manufacturer:",
                            fontSize = 13.sp,
                            color = Color(0xFFB0B0B0)
                        )
                        Text(
                            text = deviceManufacturer,
                            fontSize = 13.sp,
                            color = Color(0xFFE0E0E0),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Model:",
                            fontSize = 13.sp,
                            color = Color(0xFFB0B0B0)
                        )
                        Text(
                            text = deviceModel,
                            fontSize = 13.sp,
                            color = Color(0xFFE0E0E0),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (hasDualAppSupport) {
                        HorizontalDivider(color = Color(0xFF1a3a52), modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Supported",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(end = 8.dp)
                            )
                            Text(
                                text = "Built-in Dual App Support Available",
                                fontSize = 12.sp,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Choose Your Method Header
            Text(
                text = "Choose Your Method",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFCACBF3),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                textAlign = TextAlign.Center
            )

            // Methods Container
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0a2940).copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    // Method 1: Built-in (if supported)
                    if (hasDualAppSupport) {
                        MethodCardClickable(
                            title = "Method 1: Native Dual App",
                            description = "Use your device's built-in dual app feature",
                            isRecommended = true,
                            onClick = { showMethod1Dialog.value = true }
                        )
                    } else {
                        // Show Method 1 as unsupported card with dark background
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .shadow(
                                    elevation = 2.dp,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF051a2e)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Method 1: Native Dual App",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF666666)
                                    )
                                    Text(
                                        text = "UNSUPPORTED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier
                                            .background(
                                                color = Color(

                                                ),
                                                shape = RoundedCornerShape(3.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "Not available for your device. Use Method 2 instead.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF555555)
                                )
                            }
                        }
                    }

                    // Method 2: Island App (Universal)
                    MethodCardClickable(
                        title = "Method 2: Island App (Universal)",
                        description = "Works on all devices",
                        isRecommended = !hasDualAppSupport,
                        onClick = onOpenIsland,
                        isMethod2 = true
                    )
                }
            }

            // Info Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1a3a52)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "ℹ️ Tip",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB0B0B0),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "You can use either method. Island is recommended for maximum privacy and compatibility across all devices.",
                        fontSize = 12.sp,
                        color = Color(0xFFB0B0B0),
                        lineHeight = 16.sp
                    )
                }
            }

            // Spacer to push button to bottom
            Spacer(modifier = Modifier.weight(1f))

            // Next Step Button
            Button(
                onClick = {
                    val intent = Intent(activity, AppSelectionActivity::class.java)
                    activity?.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(12.dp)
                    ),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Purple40
                )
            ) {
                Text(
                    text = "Next Step (Ensure dual app created)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun MethodCardClickable(
    title: String,
    description: String,
    isRecommended: Boolean = false,
    onClick: () -> Unit = {},
    isMethod2: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecommended) Color(0xFF0f3460) else Color(0xFF0a2940)
        )
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            elevation = ButtonDefaults.elevatedButtonElevation(
                defaultElevation = 0.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Purple80
                    )
                    if (isRecommended) {
                        Text(
                            text = "RECOMMENDED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .background(
                                    color = Color(0xFF4CAF50),
                                    shape = RoundedCornerShape(3.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = Color(0xFFB0B0B0),
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = if (isMethod2) "Tap to download from Play Store →" else "Tap to open device settings →",
                    fontSize = 11.sp,
                    color = Purple80,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun Method1Dialog(
    deviceManufacturer: String = "Unknown",
    deviceModel: String = "Unknown",
    onDismiss: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val supportedDevices = mapOf(
        "xiaomi" to listOf(
            "Xiaomi Mi Series",
            "Xiaomi Redmi Series",
            "Xiaomi Poco Series",
            "Xiaomi Note Series"
        ),
        "samsung" to listOf(
            "Samsung Galaxy S Series",
            "Samsung Galaxy A Series",
            "Samsung Galaxy M Series",
            "Samsung Galaxy J Series",
            "Samsung Galaxy Note Series"
        ),
        "oppo" to listOf(
            "OPPO A Series",
            "OPPO F Series",
            "OPPO Reno Series",
            "OPPO K Series"
        ),
        "vivo" to listOf(
            "Vivo Y Series",
            "Vivo V Series",
            "Vivo X Series",
            "Vivo iQOO Series"
        ),
        "realme" to listOf(
            "Realme C Series",
            "Realme 5/6/7/8/9 Series",
            "Realme X Series",
            "Realme GT Series"
        ),
        "honor" to listOf(
            "Honor 9/10/20/30 Series",
            "Honor View Series",
            "Honor Play Series",
            "Honor X Series"
        ),
        "huawei" to listOf(
            "Huawei P Series",
            "Huawei Mate Series",
            "Huawei Nova Series",
            "Huawei Y Series"
        )
    )

    val manufacturerLower = deviceManufacturer.lowercase()
    val deviceList = supportedDevices[manufacturerLower] ?: listOf("Your device")

    val featureNames = mapOf(
        "xiaomi" to "App Cloner / Dual Apps (MIUI)",
        "samsung" to "Dual Messenger / Clone App",
        "oppo" to "Clone App (ColorOS)",
        "vivo" to "Dual Apps (Funtouch OS)",
        "realme" to "Clone App (Realme UI)",
        "honor" to "Clone App (Honor OS)",
        "huawei" to "Clone App (EMUI)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                modifier = Modifier.height(40.dp)
            ) {
                Text("Open Settings", color = Color.White, fontSize = 13.sp)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1a3a52)
                ),
                modifier = Modifier.height(40.dp)
            ) {
                Text("Cancel", color = Color(0xFFB0B0B0), fontSize = 13.sp)
            }
        },
        icon = null,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Method 1: Native Dual App",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Purple80,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.height(40.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Purple80
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Feature name
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0f3460)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Feature Available:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        Text(
                            text = featureNames[manufacturerLower] ?: "Native Dual App",
                            fontSize = 14.sp,
                            color = Color(0xFFE0E0E0),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Device compatibility
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0a2940)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Supported",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier
                                    .height(8.dp)
                                    .padding(end = 4.dp)
                            )
                            Text(
                                text = "Your Device is Supported!",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                        Text(
                            text = "Your $deviceManufacturer $deviceModel supports native dual app feature",
                            fontSize = 12.sp,
                            color = Color(0xFFB0B0B0)
                        )
                    }
                }

                // Advantages
                Text(
                    text = "Advantages over Island:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE0E0E0),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                listOf(
                    "✓ Optimized for your device",
                    "✓ Better performance and compatibility",
                    "✓ Native OS-level integration",
                    "✓ Lower resource usage",
                    "✓ Faster app cloning"
                ).forEach { advantage ->
                    Text(
                        text = advantage,
                        fontSize = 12.sp,
                        color = Color(0xFFB0B0B0),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                HorizontalDivider(
                    color = Color(0xFF1a3a52),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Supported devices list
                Text(
                    text = "Compatible ${deviceManufacturer.uppercase()} Models:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE0E0E0),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                deviceList.forEach { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "• ",
                            fontSize = 14.sp,
                            color = Purple80,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            text = device,
                            fontSize = 12.sp,
                            color = Color(0xFFB0B0B0)
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFF0f3460),
        titleContentColor = Purple80,
        textContentColor = Color(0xFFE0E0E0),
        tonalElevation = 8.dp
    )
}


@Preview(showBackground = true)
@Composable
fun SetupScreenPreview() {
    DALETheme {
        SetupScreen(
            deviceModel = "Redmi Note 12",
            deviceManufacturer = "Xiaomi",
            hasDualAppSupport = true
        )
    }
}

