package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.activity.ActivityScreen
import com.example.ui.components.HeaderBar
import com.example.ui.components.SplashScreen
import com.example.ui.device.DeviceScreen
import com.example.ui.history.HistoryScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.viewmodel.MainViewModel

data class NavTabItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTabItem by remember { mutableIntStateOf(0) }
    var showSplashScreen by remember { mutableStateOf(true) }

    val trackingStatus by viewModel.trackingStatus.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val selectedRange by viewModel.selectedRange.collectAsState()
    val rangeStats by viewModel.rangeStats.collectAsState()
    val topApps by viewModel.topApps.collectAsState()
    val categoryStats by viewModel.categoryStats.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val wsState by viewModel.wsConnectionState.collectAsState()
    val categoryOverrides by viewModel.categoryOverrides.collectAsState()

    val navTabs = listOf(
        NavTabItem("Activity", Icons.Filled.Assessment, Icons.Outlined.Assessment, "tab_activity"),
        NavTabItem("History", Icons.Filled.History, Icons.Outlined.History, "tab_history"),
        NavTabItem("Device", Icons.Filled.Smartphone, Icons.Outlined.Smartphone, "tab_device"),
        NavTabItem("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "tab_settings")
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            HeaderBar(
                isTrackingActive = trackingStatus.isTrackingActive,
                isSyncing = isSyncing,
                onSyncNow = { viewModel.triggerSync() }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                tonalElevation = 2.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    tonalElevation = 0.dp,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    navTabs.forEachIndexed { index, item ->
                        val selected = selectedTabItem == index
                        NavigationBarItem(
                            selected = selected,
                            onClick = { selectedTabItem = index },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = selectedTabItem,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { it / 4 } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it / 4 } + fadeOut())
                    } else {
                        (slideInHorizontally { -it / 4 } + fadeIn()) togetherWith
                            (slideOutHorizontally { it / 4 } + fadeOut())
                    }
                },
                label = "tabContent"
            ) { tab ->
                when (tab) {
                    0 -> ActivityScreen(
                        viewModel = viewModel,
                        trackingStatus = trackingStatus,
                        rangeStats = rangeStats,
                        topApps = topApps,
                        categoryStats = categoryStats,
                        settings = settings,
                        selectedRange = selectedRange,
                        isSyncing = isSyncing,
                        syncMessage = syncMessage
                    )
                    1 -> HistoryScreen(viewModel = viewModel)
                    2 -> DeviceScreen(
                        viewModel = viewModel,
                        settings = settings,
                        wsState = wsState
                    )
                    3 -> SettingsScreen(
                        viewModel = viewModel,
                        settings = settings,
                        wsState = wsState,
                        overrides = categoryOverrides
                    )
                }
            }

            if (showSplashScreen) {
                SplashScreen(onDismiss = { showSplashScreen = false })
            }
        }
    }
}
