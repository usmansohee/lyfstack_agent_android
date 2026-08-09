package com.example.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.AppCategory
import com.example.data.model.CategoryOverride
import com.example.data.model.WsConnectionState
import com.example.data.repository.AgentSettings
import com.example.service.TrackingService
import com.example.ui.theme.StatusActiveGreen
import com.example.ui.theme.StatusPausedOrange
import com.example.ui.theme.lyfTextFieldColors
import com.example.ui.viewmodel.MainViewModel

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    } else {
        true
    }
}

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    settings: AgentSettings,
    wsState: WsConnectionState,
    overrides: List<CategoryOverride>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollState = rememberScrollState()

    var autoSyncEnabled by remember(settings.autoSyncEnabled) { mutableStateOf(settings.autoSyncEnabled) }
    var syncIntervalMinutes by remember(settings.autoSyncIntervalMinutes) { mutableStateOf(settings.autoSyncIntervalMinutes) }
    var syncEndpointUrl by remember(settings.syncEndpointUrl) { mutableStateOf(settings.syncEndpointUrl) }

    var wsEnabled by remember(settings.wsEnabled) { mutableStateOf(settings.wsEnabled) }
    var wsUrl by remember(settings.wsUrl) { mutableStateOf(settings.wsUrl) }
    var wsToken by remember(settings.wsToken) { mutableStateOf(settings.wsToken) }

    var ignoreListRaw by remember(settings.ignoreListRaw) { mutableStateOf(settings.ignoreListRaw) }

    var newPkgName by remember { mutableStateOf("") }
    var newCatSelected by remember { mutableStateOf(AppCategory.WORK.displayName) }

    var hasUsageAccess by remember { mutableStateOf(TrackingService.checkUsagePermission(context)) }
    var isBatteryUnrestricted by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsageAccess = TrackingService.checkUsagePermission(context)
                isBatteryUnrestricted = isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val scheme = MaterialTheme.colorScheme
    val textFieldColors = lyfTextFieldColors()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // Device Summary Strip
        Surface(
            color = scheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().testTag("device_summary_strip")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
                    color = scheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("First Sync: ${MainViewModel.formatTimestamp(settings.firstSyncAt)}", color = scheme.onSurfaceVariant, fontSize = 11.sp)
                    Text("Last Sync: ${MainViewModel.formatTimestamp(settings.lastSyncAt)}", color = scheme.onSurfaceVariant, fontSize = 11.sp)
                }
            }
        }

        // Permissions & Background Guidance Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = scheme.surfaceContainerHighest, modifier = Modifier.size(28.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PERMISSIONS & BATTERY GUIDANCE", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = scheme.onSurface, letterSpacing = 1.1.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Usage Access
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Usage Access (Required)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = scheme.onSurface)
                        Text("Grants app usage statistics tracking", fontSize = 11.sp, color = scheme.onSurfaceVariant)
                    }
                    if (hasUsageAccess) {
                        Surface(
                            color = StatusActiveGreen.copy(alpha = 0.15f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusActiveGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Granted", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StatusActiveGreen)
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.testTag("open_usage_settings_button")
                        ) {
                            Text("Grant", fontSize = 12.sp, color = scheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Battery Unrestricted
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Unrestricted Battery & Autostart", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = scheme.onSurface)
                        Text(
                            if (isBatteryUnrestricted) "Allowed: Unrestricted background execution active"
                            else "Restricted: Click Configure to allow continuous tracking",
                            fontSize = 11.sp,
                            color = if (isBatteryUnrestricted) StatusActiveGreen else StatusPausedOrange
                        )
                    }
                    if (isBatteryUnrestricted) {
                        Surface(
                            color = StatusActiveGreen.copy(alpha = 0.15f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusActiveGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Allowed", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StatusActiveGreen)
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                try { context.startActivity(intent) } catch (e: Exception) {
                                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusPausedOrange, contentColor = Color.White),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text("Allow", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // Auto Sync Options Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = scheme.surfaceContainerHighest, modifier = Modifier.size(28.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Sync, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("HTTPS AUTO SYNC OPTIONS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = scheme.onSurface, letterSpacing = 1.1.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto Sync Enabled", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = scheme.onSurface)
                    Switch(
                        checked = autoSyncEnabled,
                        onCheckedChange = { autoSyncEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = scheme.onPrimary, checkedTrackColor = scheme.primary),
                        modifier = Modifier.testTag("auto_sync_switch")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Interval Dropdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sync Interval", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = scheme.onSurface)
                    var intervalExpanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { intervalExpanded = true },
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("$syncIntervalMinutes minutes", fontSize = 12.sp, color = scheme.onSurface, fontWeight = FontWeight.SemiBold)
                        }
                        DropdownMenu(
                            expanded = intervalExpanded,
                            onDismissRequest = { intervalExpanded = false }
                        ) {
                            listOf(5, 10, 15, 20, 30).forEach { mins ->
                                DropdownMenuItem(
                                    text = { Text("$mins minutes") },
                                    onClick = {
                                        syncIntervalMinutes = mins
                                        intervalExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // HTTPS POST Endpoint URL
                Text("Sync Endpoint (HTTPS POST URL)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = scheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = syncEndpointUrl,
                    onValueChange = { syncEndpointUrl = it },
                    placeholder = { Text("https://api.lyfstack.app/api/v1/device-activity/sync", fontSize = 11.sp, color = scheme.onSurfaceVariant) },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = scheme.onSurface),
                    shape = MaterialTheme.shapes.small,
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth().testTag("sync_endpoint_input")
                )
            }
        }

        // WebSocket Control Connection Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = scheme.surfaceContainerHighest, modifier = Modifier.size(28.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Router, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("DEVICE CONNECTION (WebSocket)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = scheme.onSurface, letterSpacing = 1.1.sp)
                    }

                    // Status Pill
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = when (wsState) {
                            WsConnectionState.ONLINE -> StatusActiveGreen.copy(alpha = 0.15f)
                            WsConnectionState.CONNECTING, WsConnectionState.RECONNECTING -> StatusPausedOrange.copy(alpha = 0.15f)
                            WsConnectionState.OFF -> scheme.surfaceContainerHighest
                        }
                    ) {
                        Text(
                            text = wsState.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (wsState) {
                                WsConnectionState.ONLINE -> StatusActiveGreen
                                WsConnectionState.CONNECTING, WsConnectionState.RECONNECTING -> StatusPausedOrange
                                WsConnectionState.OFF -> scheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable WebSocket Control Connection", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = scheme.onSurface)
                    Switch(
                        checked = wsEnabled,
                        onCheckedChange = { wsEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = scheme.onPrimary, checkedTrackColor = scheme.primary),
                        modifier = Modifier.testTag("ws_enabled_switch")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("WebSocket URL (wss://)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = scheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = wsUrl,
                    onValueChange = { wsUrl = it },
                    placeholder = { Text("wss://api.lyfstack.app/device-connection", fontSize = 11.sp, color = scheme.onSurfaceVariant) },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = scheme.onSurface),
                    shape = MaterialTheme.shapes.small,
                    enabled = wsEnabled,
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth().testTag("ws_url_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("WebSocket Auth Token (Optional)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = scheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = wsToken,
                    onValueChange = { wsToken = it },
                    placeholder = { Text("Token...", fontSize = 11.sp, color = scheme.onSurfaceVariant) },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = scheme.onSurface),
                    shape = MaterialTheme.shapes.small,
                    enabled = wsEnabled,
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth().testTag("ws_token_input")
                )
            }
        }

        // Ignore List (Multiline Packages) Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = scheme.surfaceContainerHighest, modifier = Modifier.size(28.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.List, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("IGNORE LIST (PACKAGES TO EXCLUDE)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = scheme.onSurface, letterSpacing = 1.1.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("Enter package names (one per line) to exclude from usage tracking.", fontSize = 11.sp, color = scheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = ignoreListRaw,
                    onValueChange = { ignoreListRaw = it },
                    minLines = 3,
                    maxLines = 6,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = scheme.onSurface),
                    shape = MaterialTheme.shapes.small,
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth().testTag("ignore_list_input")
                )
            }
        }

        // Manual Category Overrides Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = scheme.surfaceContainerHighest, modifier = Modifier.size(28.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Category, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CATEGORY OVERRIDES (PACKAGE → CATEGORY)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = scheme.onSurface, letterSpacing = 1.1.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Add override row with matched heights
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newPkgName,
                        onValueChange = { newPkgName = it },
                        placeholder = { Text("com.example.app", fontSize = 11.sp, color = scheme.onSurfaceVariant) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = scheme.onSurface),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        colors = textFieldColors,
                        modifier = Modifier.weight(1.5f).height(48.dp).testTag("new_pkg_override_input")
                    )

                    var catMenuExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { catMenuExpanded = true },
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text(newCatSelected, fontSize = 11.sp, maxLines = 1, color = scheme.onSurface)
                        }
                        DropdownMenu(
                            expanded = catMenuExpanded,
                            onDismissRequest = { catMenuExpanded = false }
                        ) {
                            AppCategory.entries.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.displayName) },
                                    onClick = {
                                        newCatSelected = cat.displayName
                                        catMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            if (newPkgName.isNotBlank()) {
                                viewModel.addCategoryOverride(newPkgName, newCatSelected)
                                newPkgName = ""
                            }
                        },
                        modifier = Modifier.size(48.dp).testTag("add_override_button")
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = scheme.primary,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Add, contentDescription = "Add Override", tint = scheme.onPrimary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (overrides.isEmpty()) {
                    Text("No manual category overrides configured.", fontSize = 12.sp, color = scheme.onSurfaceVariant)
                } else {
                    overrides.forEach { override ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(override.packageName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                                Text("Category: ${override.category}", fontSize = 11.sp, color = scheme.primary)
                            }
                            IconButton(
                                onClick = { viewModel.removeCategoryOverride(override.packageName) }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // Save Settings Button
        Button(
            onClick = {
                viewModel.saveSettings(
                    autoSyncEnabled = autoSyncEnabled,
                    intervalMinutes = syncIntervalMinutes,
                    syncUrl = syncEndpointUrl,
                    wsEnabled = wsEnabled,
                    wsUrl = wsUrl,
                    wsToken = wsToken,
                    ignoreListRaw = ignoreListRaw
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = scheme.primary, contentColor = scheme.onPrimary),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_settings_button")
        ) {
            Icon(Icons.Default.Save, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save All Settings", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = scheme.onPrimary)
        }
    }
}
