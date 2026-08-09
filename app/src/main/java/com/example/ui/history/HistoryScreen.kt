package com.example.ui.history

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.data.model.AppCategory
import com.example.data.model.SyncRange
import com.example.data.model.UsageSession
import com.example.ui.theme.CardBackground
import com.example.ui.theme.PageBackground
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900
import com.example.ui.theme.StatusActiveGreen
import com.example.ui.theme.StatusPausedOrange
import com.example.ui.theme.Teal700
import com.example.ui.viewmodel.MainViewModel

@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedRange by viewModel.selectedRange.collectAsState()
    val searchQuery by viewModel.historySearchQuery.collectAsState()
    val categoryFilter by viewModel.historyCategoryFilter.collectAsState()
    val sessions by viewModel.filteredSessions.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        // Top Control Card: Range filter + Export CSV + Export JSON
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("history_header_card")
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ACTIVITY HISTORY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500, letterSpacing = 1.2.sp)
                    Row {
                        OutlinedButton(
                            onClick = { viewModel.exportCsv(context) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("export_csv_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CSV", fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = { viewModel.exportJson(context) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Teal700,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("export_json_button")
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("JSON", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Range Filter Chips
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
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Teal700,
                                selectedLabelColor = Color.White,
                                containerColor = PageBackground,
                                labelColor = Slate600
                            ),
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                }
            }
        }

        // Search & Category Dropdown
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.historySearchQuery.value = it },
                    placeholder = { Text("Filter app or package...", fontSize = 11.sp, color = Slate500) },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Slate900),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Teal700, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate900,
                        unfocusedTextColor = Slate900,
                        focusedPlaceholderColor = Slate500,
                        unfocusedPlaceholderColor = Slate500,
                        focusedContainerColor = com.example.ui.theme.InputBackground,
                        unfocusedContainerColor = com.example.ui.theme.InputBackground,
                        focusedBorderColor = Teal700,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.weight(1f).height(52.dp).testTag("history_search_input")
                )

                Spacer(modifier = Modifier.width(8.dp))

                var catDropdownExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { catDropdownExpanded = true },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = null, tint = Teal700, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(categoryFilter, fontSize = 12.sp, color = Slate900, fontWeight = FontWeight.SemiBold)
                    }
                    DropdownMenu(
                        expanded = catDropdownExpanded,
                        onDismissRequest = { catDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Categories") },
                            onClick = {
                                viewModel.historyCategoryFilter.value = "All"
                                catDropdownExpanded = false
                            }
                        )
                        AppCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.displayName) },
                                onClick = {
                                    viewModel.historyCategoryFilter.value = cat.displayName
                                    catDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Sessions List / Table
        if (sessions.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No usage sessions found for selected filters.", color = Slate500, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
                modifier = Modifier.fillMaxWidth().weight(1f).testTag("sessions_lazy_column"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sessions, key = { it.id }) { session ->
                    SessionItemCard(session = session)
                }
            }
        }
    }
}

@Composable
private fun SessionItemCard(session: UsageSession) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(session.applicationName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate900)
                    Text(session.processName, fontSize = 11.sp, color = Slate500)
                }

                Surface(
                    color = PageBackground,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = session.category,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Teal700,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(if (session.isOpen) StatusActiveGreen else StatusPausedOrange, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (session.isOpen) "Open · ${session.lastState}" else session.lastState,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate600
                    )
                }

                Text(
                    text = "Active: ${MainViewModel.formatDuration(session.activeDurationSeconds)} | Idle: ${MainViewModel.formatDuration(session.idleDurationSeconds)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate900
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Started: ${session.startedAt.take(19).replace('T', ' ')}",
                    fontSize = 10.sp,
                    color = Slate500
                )
                Text(
                    text = if (session.isSynced) "Synced" else "Pending Sync",
                    fontSize = 10.sp,
                    color = if (session.isSynced) StatusActiveGreen else StatusPausedOrange,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
