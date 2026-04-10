package com.example.dale

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dale.ui.theme.DALETheme
import com.example.dale.utils.AppActivityLogger

class DeveloperConsoleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DALETheme {
                DeveloperConsoleScreen(
                    onClose = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperConsoleScreen(onClose: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var activityLogs by remember { mutableStateOf(AppActivityLogger.getLastLogs(50)) }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshKey) {
        activityLogs = AppActivityLogger.getLastLogs(50)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Developer Console",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0f3460)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color(0xFF1a1a2e), Color(0xFF16213e))
                    )
                )
                .padding(innerPadding)
        ) {
            ActivityLogsTab(
                logs = activityLogs,
                onRefresh = { refreshKey++ },
                onClearLogs = {
                    AppActivityLogger.clearActivityLogs()
                    refreshKey++
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )
        }
    }
}


@Composable
fun TabButtons(
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, tab ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onTabSelected(index) }
                    .background(
                        if (selectedTab == index) Color(0xFF5DADE2) else Color.Transparent
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selectedTab == index) Color(0xFF0f3460) else Color.White
                )
            }
        }
    }
}


@Composable
fun ActivityLogsTab(
    logs: List<String>,
    onRefresh: () -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        // Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onRefresh,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF5DADE2)
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Refresh", fontSize = 12.sp)
            }

            Button(
                onClick = onClearLogs,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF5252)
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Clear", fontSize = 12.sp)
            }
        }

        // Logs count
        Text(
            text = "Total Logs: ${logs.size}",
            fontSize = 12.sp,
            color = Color(0xFF5DADE2),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Logs list
        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No activity logs yet",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(logs) { log ->
                    LogEntry(log)
                }
            }
        }
    }
}

@Composable
fun LogEntry(log: String) {
    val isOpened = log.contains("APP_OPENED") || log.contains("LOCK_SCREEN_TRIGGERED")
    val isClosed = log.contains("APP_CLOSED")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isOpened -> Color(0xFF1B5E20).copy(alpha = 0.3f)
                isClosed -> Color(0xFFB71C1C).copy(alpha = 0.3f)
                else -> Color(0xFF0C2340)
            }
        )
    ) {
        Text(
            text = log,
            fontSize = 10.sp,
            color = when {
                isOpened -> Color(0xFF4CAF50)
                isClosed -> Color(0xFFFF5252)
                else -> Color(0xFFB0B0B0)
            },
            modifier = Modifier.padding(8.dp),
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            maxLines = 3
        )
    }
}

