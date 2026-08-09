package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.StatusActiveGreen
import com.example.ui.theme.StatusPausedOrange

@Composable
fun HeaderBar(
    isTrackingActive: Boolean,
    isSyncing: Boolean,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val pulseDot by animateColorAsState(
        targetValue = if (isTrackingActive) StatusActiveGreen else StatusPausedOrange,
        label = "headerPulse"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = scheme.surfaceContainerLowest,
        shadowElevation = 0.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "LyfStack",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "LyfStack",
                            style = MaterialTheme.typography.titleLarge,
                            color = scheme.onSurface
                        )
                        Text(
                            text = "Android agent",
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = scheme.surfaceContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(pulseDot, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = if (isTrackingActive) "Live" else "Paused",
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.onSurface
                            )
                        }
                    }

                    val infiniteTransition = rememberInfiniteTransition(label = "SyncRotation")
                    val rotationAngle by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 900, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "SyncRotationAngle"
                    )

                    FilledIconButton(
                        onClick = onSyncNow,
                        enabled = !isSyncing,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = scheme.surfaceContainerHigh,
                            contentColor = scheme.onSurface,
                            disabledContainerColor = scheme.surfaceContainer,
                            disabledContentColor = scheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Sync,
                            contentDescription = "Sync Now",
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(if (isSyncing) rotationAngle else 0f)
                        )
                    }
                }
            }
            HorizontalDivider(color = scheme.outlineVariant)
        }
    }
}
