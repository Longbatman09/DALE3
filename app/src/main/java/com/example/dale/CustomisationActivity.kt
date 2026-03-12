package com.example.dale

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dale.ui.theme.DALETheme

class CustomisationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val groupName = intent.getStringExtra("GROUP_NAME") ?: ""

        setContent {
            DALETheme {
                CustomisationScreen(
                    groupName = groupName,
                    activity = this
                )
            }
        }
    }
}

@Composable
fun CustomisationScreen(
    groupName: String,
    activity: ComponentActivity
) {
    val showLockTypeDialog = remember { mutableStateOf(false) }

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
                    text = "Customisation",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Text(
                text = if (groupName.isBlank()) "Current Group" else groupName,
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 4.dp)
            )

            HorizontalDivider(
                color = Color(0xFF0f3460),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsCard(
                    title = "Screen Lock type Change",
                    subtitle = "Choose lock screen style",
                    icon = Icons.Default.Lock,
                    onClick = { showLockTypeDialog.value = true }
                )
            }
        }

        if (showLockTypeDialog.value) {
            AlertDialog(
                onDismissRequest = { showLockTypeDialog.value = false },
                title = {
                    Text(
                        text = "Screen Lock type Change",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text("This option is now added to Group Settings. Full lock type switching can be wired to storage next.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLockTypeDialog.value = false
                            Toast.makeText(activity, "Lock type option opened", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }
}
