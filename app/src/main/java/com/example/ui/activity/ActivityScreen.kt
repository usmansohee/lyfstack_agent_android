package com.example.ui.activity

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material.icons.filled.AppSettingsAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.CategoryStat
import com.example.data.db.TopAppStat
import com.example.data.model.SyncRange
import com.example.data.repository.AgentSettings
import com.example.data.repository.RangeStats
import com.example.service.TrackingStatus
import com.example.ui.theme.CardBackground
import com.example.ui.theme.PageBackground
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900
import com.example.ui.theme.StatusActiveGreen
import com.example.ui.theme.StatusPausedOrange
import com.example.ui.theme.Teal100
import com.example.ui.theme.Teal500
import com.example.ui.theme.Teal700
import com.example.ui.viewmodel.MainViewModel

@Composable
fun ActivityScreen(
    viewModel: MainViewModel,
    trackingStatus: TrackingStatus,
    rangeStats: RangeStats,
    topApps: List<TopAppStat>,
    categoryStats: List<CategoryStat>,
    settings: AgentSettings,
    selectedRange: SyncRange,
    isSyncing: Boolean,
    syncMessage: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // Permission Banner if usage permission missing
        if (!trackingStatus.hasUsagePermission) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("permission_banner")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Usage Access Required", fontWeight = FontWeight.Bold, color = Color(0xFF991B1B), fontSize = 14.sp)
                        Text("Grant Usage Stats permission so LyfStack can track app activity.", color = Color(0xFF7F1D1D), fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Grant", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }

        // Top Status & Quick Stats Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("top_status_card")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    if (trackingStatus.isTrackingActive) StatusActiveGreen else StatusPausedOrange,
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (trackingStatus.isTrackingActive) "Tracking Active" else "Tracking Paused",
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            fontSize = 15.sp
                        )
                    }

                    Button(
                        onClick = { viewModel.toggleTrackingPause() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (trackingStatus.isTrackingActive) StatusPausedOrange else Teal700
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("toggle_tracking_button")
                    ) {
                        Icon(
                            imageVector = if (trackingStatus.isTrackingActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (trackingStatus.isTrackingActive) "Pause" else "Resume",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stats Row Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatBox(
                        label = "NOW",
                        value = if (trackingStatus.currentAppName.isNotBlank()) trackingStatus.currentAppName else "None",
                        sub = trackingStatus.currentCategory,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatBox(
                        label = "ACTIVE",
                        value = MainViewModel.formatDuration(rangeStats.activeSeconds),
                        sub = "${rangeStats.sessionCount} sessions",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatBox(
                        label = "SYNC",
                        value = if (rangeStats.pendingSyncCount == 0) "Synced" else "${rangeStats.pendingSyncCount} Pending",
                        sub = MainViewModel.formatTimestamp(settings.lastSyncAt),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Range Selector Bar
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "SUMMARY RANGE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate500,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        SyncRange.TODAY to "Today",
                        SyncRange.WEEK to "Week",
                        SyncRange.MONTH to "Month",
                        SyncRange.YEAR to "Year",
                        SyncRange.ALL to "All"
                    ).forEach { (range, label) ->
                        FilterChip(
                            selected = selectedRange == range,
                            onClick = { viewModel.setRange(range) },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Teal700,
                                selectedLabelColor = Color.White,
                                containerColor = PageBackground,
                                labelColor = Slate600
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("range_chip_${range.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // Metrics Grid (Active, Idle, Tracked, Focus%)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                title = "Active Time",
                value = MainViewModel.formatDuration(rangeStats.activeSeconds),
                badgeColor = StatusActiveGreen,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Idle Time",
                value = MainViewModel.formatDuration(rangeStats.idleSeconds),
                badgeColor = StatusPausedOrange,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Tracked",
                value = MainViewModel.formatDuration(rangeStats.totalTrackedSeconds),
                badgeColor = Teal500,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Focus %",
                value = "${rangeStats.focusPercentage}%",
                badgeColor = Teal700,
                modifier = Modifier.weight(1f)
            )
        }

        // Meta Bar
        Surface(
            color = Teal100.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "${rangeStats.sessionCount} sessions · ${rangeStats.uniqueAppsCount} apps tracked · Pending sync: ${rangeStats.pendingSyncCount}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Teal700,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        // Manual Sync Card (Moved above Top Apps)
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("manual_sync_card")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = Teal700, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("MANUAL DATA SYNC", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900, letterSpacing = 0.8.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var rangeDropdownExpanded by remember { mutableStateOf(false) }
                    var selectedSyncRange by remember { mutableStateOf(SyncRange.SINCE_LAST) }

                    // Sync Range Dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { rangeDropdownExpanded = true },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sync_range_dropdown")
                        ) {
                            Text(selectedSyncRange.displayName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Slate900, maxLines = 1)
                        }
                        DropdownMenu(
                            expanded = rangeDropdownExpanded,
                            onDismissRequest = { rangeDropdownExpanded = false }
                        ) {
                            SyncRange.entries.filter { it != SyncRange.CUSTOM }.forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(r.displayName, fontSize = 12.sp) },
                                    onClick = {
                                        selectedSyncRange = r
                                        rangeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Sync Now Button (White text on Green background as requested)
                    Button(
                        onClick = { viewModel.triggerSync(selectedSyncRange) },
                        enabled = !isSyncing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = com.example.ui.theme.GreenSync,
                            contentColor = Color.White,
                            disabledContainerColor = com.example.ui.theme.GreenSync.copy(alpha = 0.5f),
                            disabledContentColor = Color.White.copy(alpha = 0.7f)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("sync_now_button")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isSyncing) "Syncing..." else "Sync Now", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                if (!syncMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = syncMessage!!,
                        fontSize = 11.sp,
                        color = if (syncMessage!!.contains("failed", ignoreCase = true)) Color.Red else Teal700
                    )
                }
            }
        }

        // Top Apps Breakdown
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Smartphone, contentDescription = null, tint = Teal700, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("TOP APPS", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate900, letterSpacing = 0.8.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                if (topApps.isEmpty()) {
                    Text("No apps recorded yet", fontSize = 12.sp, color = Slate500, modifier = Modifier.padding(vertical = 12.dp))
                } else {
                    val maxSec = topApps.maxOfOrNull { it.activeSeconds }?.coerceAtLeast(1L) ?: 1L
                    topApps.forEach { appStat ->
                        Column(modifier = Modifier.padding(vertical = 5.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(appStat.applicationName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Slate900)
                                Text(MainViewModel.formatDuration(appStat.activeSeconds), fontSize = 12.sp, color = Slate600)
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = { (appStat.activeSeconds.toFloat() / maxSec.toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = Teal500,
                                trackColor = PageBackground
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.isBlank()

@Composable
private fun StatBox(label: String, value: String, sub: String, modifier: Modifier = Modifier) {
    Surface(
        color = PageBackground,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate500)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900, maxLines = 1)
            Text(sub, fontSize = 10.sp, color = Slate600, maxLines = 1)
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, badgeColor: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(badgeColor, CircleShape)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Slate500,
                letterSpacing = 1.1.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate900)
        }
    }
}
