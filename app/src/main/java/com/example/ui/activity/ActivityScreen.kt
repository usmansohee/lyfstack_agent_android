package com.example.ui.activity

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.db.CategoryStat
import com.example.data.db.TopAppStat
import com.example.data.model.SyncRange
import com.example.data.repository.AgentSettings
import com.example.data.repository.RangeStats
import com.example.service.TrackingStatus
import com.example.ui.components.LyfCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.GreenSync
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.StatusActiveGreen
import com.example.ui.theme.StatusPausedOrange
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
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (!trackingStatus.hasUsagePermission) {
            Surface(
                modifier = Modifier.fillMaxWidth().testTag("permission_banner"),
                shape = MaterialTheme.shapes.medium,
                color = Color(0xFFFEF2F2),
                contentColor = Color(0xFF991B1B)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Usage access required", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Grant permission so LyfStack can track foreground apps.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF7F1D1D)
                        )
                    }
                    Button(
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = MaterialTheme.shapes.small,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("Grant")
                    }
                }
            }
        }

        LyfCard(modifier = Modifier.testTag("top_status_card")) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (trackingStatus.isTrackingActive) "Tracking active" else "Tracking paused",
                        style = MaterialTheme.typography.titleMedium
                    )
                    StatusPill(
                        text = if (trackingStatus.isTrackingActive) "Live sampling" else "Paused",
                        active = trackingStatus.isTrackingActive
                    )
                }

                FilledTonalButton(
                    onClick = { viewModel.toggleTrackingPause() },
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (trackingStatus.isTrackingActive) {
                            StatusPausedOrange.copy(alpha = 0.18f)
                        } else {
                            scheme.primaryContainer
                        },
                        contentColor = if (trackingStatus.isTrackingActive) {
                            StatusPausedOrange
                        } else {
                            scheme.onPrimaryContainer
                        }
                    ),
                    modifier = Modifier.testTag("toggle_tracking_button")
                ) {
                    Icon(
                        imageVector = if (trackingStatus.isTrackingActive) {
                            Icons.Rounded.Pause
                        } else {
                            Icons.Rounded.PlayArrow
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (trackingStatus.isTrackingActive) "Pause" else "Resume")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatBox(
                    label = "Now",
                    value = trackingStatus.currentAppName.ifBlank { "None" },
                    sub = trackingStatus.currentCategory.ifBlank { "—" },
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    label = "Idle",
                    value = MainViewModel.formatDuration(rangeStats.idleSeconds),
                    sub = "${rangeStats.sessionCount} sessions",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        LyfCard(contentPadding = 12.dp) {
            var rangeMenuExpanded by remember { mutableStateOf(false) }
            val summaryRanges = listOf(
                SyncRange.TODAY,
                SyncRange.WEEK,
                SyncRange.MONTH,
                SyncRange.YEAR,
                SyncRange.ALL
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Summary range",
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onSurfaceVariant
                )
                Box {
                    OutlinedButton(
                        onClick = { rangeMenuExpanded = true },
                        shape = MaterialTheme.shapes.small,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("summary_range_dropdown")
                    ) {
                        Text(
                            selectedRange.shortLabel,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = rangeMenuExpanded,
                        onDismissRequest = { rangeMenuExpanded = false }
                    ) {
                        summaryRanges.forEach { range ->
                            DropdownMenuItem(
                                text = { Text(range.shortLabel) },
                                onClick = {
                                    viewModel.setRange(range)
                                    rangeMenuExpanded = false
                                },
                                modifier = Modifier.testTag("range_chip_${range.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "Active",
                value = MainViewModel.formatDuration(rangeStats.activeSeconds),
                accent = StatusActiveGreen,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Idle",
                value = MainViewModel.formatDuration(rangeStats.idleSeconds),
                accent = StatusPausedOrange,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "Tracked",
                value = MainViewModel.formatDuration(rangeStats.totalTrackedSeconds),
                accent = Slate500,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Focus",
                value = "${rangeStats.focusPercentage}%",
                accent = Slate700,
                modifier = Modifier.weight(1f)
            )
        }

        Surface(
            color = scheme.surfaceContainer,
            contentColor = scheme.onSurfaceVariant,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "${rangeStats.sessionCount} sessions · ${rangeStats.uniqueAppsCount} apps · ${rangeStats.pendingSyncCount} pending sync",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }

        LyfCard(modifier = Modifier.testTag("manual_sync_card")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Sync, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Manual sync", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                var rangeDropdownExpanded by remember { mutableStateOf(false) }
                var selectedSyncRange by remember { mutableStateOf(SyncRange.SINCE_LAST) }

                Box(modifier = Modifier.weight(1.35f)) {
                    OutlinedButton(
                        onClick = { rangeDropdownExpanded = true },
                        shape = MaterialTheme.shapes.small,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("sync_range_dropdown")
                    ) {
                        Text(
                            selectedSyncRange.shortLabel,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = rangeDropdownExpanded,
                        onDismissRequest = { rangeDropdownExpanded = false }
                    ) {
                        SyncRange.entries.filter { it != SyncRange.CUSTOM }.forEach { range ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        range.displayName,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                onClick = {
                                    selectedSyncRange = range
                                    rangeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = { viewModel.triggerSync(selectedSyncRange) },
                    enabled = !isSyncing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenSync,
                        contentColor = Color.White,
                        disabledContainerColor = GreenSync.copy(alpha = 0.45f)
                    ),
                    shape = MaterialTheme.shapes.small,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .weight(0.9f)
                        .height(44.dp)
                        .testTag("sync_now_button")
                ) {
                    Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (isSyncing) "Syncing…" else "Sync",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1
                    )
                }
            }

            if (!syncMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = syncMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (syncMessage.contains("failed", ignoreCase = true)) {
                        scheme.error
                    } else {
                        scheme.primary
                    }
                )
            }
        }

        LyfCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Smartphone, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Top apps", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (topApps.isEmpty()) {
                Text(
                    "No apps recorded yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant
                )
            } else {
                val maxSec = topApps.maxOfOrNull { it.activeSeconds }?.coerceAtLeast(1L) ?: 1L
                topApps.forEach { appStat ->
                    val progress by animateFloatAsState(
                        targetValue = (appStat.activeSeconds.toFloat() / maxSec.toFloat()).coerceIn(0f, 1f),
                        animationSpec = tween(500),
                        label = "appProgress"
                    )
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                appStat.applicationName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                MainViewModel.formatDuration(appStat.activeSeconds),
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(MaterialTheme.shapes.extraLarge),
                            color = scheme.primary,
                            trackColor = scheme.surfaceContainerHighest
                        )
                    }
                }
            }
        }

        if (categoryStats.isNotEmpty()) {
            LyfCard {
                Text("By category", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(10.dp))
                categoryStats.take(6).forEach { stat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stat.category, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            MainViewModel.formatDuration(stat.activeSeconds),
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(
    label: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                sub,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(accent, CircleShape)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}
