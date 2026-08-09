package com.example.ui.device

import android.app.ActivityManager
import android.content.Context
import android.os.Build
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WsConnectionState
import com.example.data.repository.AgentSettings
import com.example.ui.theme.CardBackground
import com.example.ui.theme.PageBackground
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.StatusActiveGreen
import com.example.ui.theme.StatusErrorRed
import com.example.ui.theme.StatusPausedOrange
import com.example.ui.theme.Teal500
import com.example.ui.theme.Teal700
import com.example.ui.viewmodel.MainViewModel

@Composable
fun DeviceScreen(
    viewModel: MainViewModel,
    settings: AgentSettings,
    wsState: WsConnectionState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val deviceName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
    val memoryInfo = getMemoryInfo(context)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // Dark Teal Gradient Card
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
            modifier = Modifier.fillMaxWidth().testTag("device_header_card")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Slate900, Teal700)
                        )
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(deviceName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                Text("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            }
                        }

                        // WS Connection status badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when (wsState) {
                                WsConnectionState.ONLINE -> StatusActiveGreen.copy(alpha = 0.25f)
                                WsConnectionState.CONNECTING, WsConnectionState.RECONNECTING -> StatusPausedOrange.copy(alpha = 0.25f)
                                WsConnectionState.OFF -> Color.White.copy(alpha = 0.15f)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            when (wsState) {
                                                WsConnectionState.ONLINE -> StatusActiveGreen
                                                WsConnectionState.CONNECTING, WsConnectionState.RECONNECTING -> StatusPausedOrange
                                                WsConnectionState.OFF -> Color.Gray
                                            },
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "WS: ${wsState.label}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Device ID: ${settings.deviceId}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                    Text("Agent Version: 1.0.0 (LyfStack.Agent.Android)", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                }
            }
        }

        // Technical Specs Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeveloperBoard, contentDescription = null, tint = Teal700, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("HARDWARE & SYSTEM", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate900, letterSpacing = 1.1.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                DetailRow("Manufacturer", Build.MANUFACTURER.replaceFirstChar { it.uppercase() })
                DetailRow("Model", Build.MODEL)
                DetailRow("Brand / Device", "${Build.BRAND} (${Build.DEVICE})")
                DetailRow("Android Version", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                DetailRow("Build ID", Build.ID)
                DetailRow("Architecture", Build.SUPPORTED_ABIS.joinToString(", "))
                DetailRow("Total Memory", memoryInfo.totalRam)
                DetailRow("Available Memory", memoryInfo.availRam)
                DetailRow("Hardware / Board", "${Build.HARDWARE} / ${Build.BOARD}")
            }
        }

        // Agent Metadata Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Teal700, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AGENT & SYNC METADATA", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate900, letterSpacing = 1.1.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                DetailRow("Agent Name", "LyfStack.Agent.Android")
                DetailRow("Agent Version", "1.0.0")
                DetailRowMultiline("Device UUID", settings.deviceId)
                DetailRow("First Installed", MainViewModel.formatTimestamp(settings.firstInstalledAt))
                DetailRow("First Sync", MainViewModel.formatTimestamp(settings.firstSyncAt))
                DetailRow("Last Sync", MainViewModel.formatTimestamp(settings.lastSyncAt))
                DetailRowMultiline("Sync Endpoint", settings.syncEndpointUrl)
                DetailRowMultiline("WebSocket Endpoint", if (settings.wsEnabled) settings.wsUrl else "Disabled")
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = Slate500, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 12.sp, color = Slate900, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DetailRowMultiline(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(label, fontSize = 12.sp, color = Slate500, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(3.dp))
        Surface(
            color = PageBackground,
            shape = RoundedCornerShape(6.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = value,
                fontSize = 11.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = Slate900,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

private data class MemoryData(val totalRam: String, val availRam: String)

private fun getMemoryInfo(context: Context): MemoryData {
    return try {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val totalMb = memInfo.totalMem / (1024 * 1024)
        val availMb = memInfo.availMem / (1024 * 1024)
        MemoryData("${totalMb} MB", "${availMb} MB")
    } catch (e: Exception) {
        MemoryData("Unknown", "Unknown")
    }
}
