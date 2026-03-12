package com.example.dale

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dale.ui.theme.DALETheme
import com.example.dale.utils.SharedPreferencesManager

class ActivityLogsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val groupId = intent.getStringExtra("GROUP_ID") ?: ""
        val groupName = intent.getStringExtra("GROUP_NAME") ?: ""

        setContent {
            DALETheme {
                ActivityLogsScreen(
                    groupId = groupId,
                    groupName = groupName,
                    activity = this
                )
            }
        }
    }
}

@Composable
fun ActivityLogsScreen(
    groupId: String,
    groupName: String,
    activity: ComponentActivity
) {
    val sharedPrefs = SharedPreferencesManager.getInstance(activity)
    val logs: List<ActivityLogEntry> = remember(groupId) {
        sharedPrefs.getActivityLogs(groupId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(Color(0xFF1a1a2e), Color(0xFF16213e))
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
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = "Activity Logs",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = groupName,
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                }
            }

            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No activity logs yet",
                            fontSize = 16.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "App open/close events will appear here",
                            fontSize = 12.sp,
                            color = Color.DarkGray,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(items = logs, key = { index, log ->
                        "${log.timestamp}-${log.packageName}-${log.event}-$index"
                    }) { _, log ->
                        ActivityLogItem(log)
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityLogItem(log: ActivityLogEntry) {
    val lineColor = if (log.event == "OPENED") {
        Color(0x332ECC71)
    } else {
        Color(0x33E74C3C)
    }

    val textColor = if (log.event == "OPENED") {
        Color(0xFF8DF6B5)
    } else {
        Color(0xFFFFA3A3)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(lineColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "[${log.timestamp}] ${log.event}: ${log.appName}",
            fontSize = 12.sp,
            color = textColor,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
    }
}
