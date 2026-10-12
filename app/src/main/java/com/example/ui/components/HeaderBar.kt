package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DlnaDevice
import com.example.data.model.TransportState
import com.example.ui.Screen
import com.example.ui.theme.CastCyan
import com.example.ui.theme.CastGreenActive
import com.example.ui.theme.DeepVioletBg
import com.example.ui.theme.JellyViolet
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.WarningAmber

@Composable
fun HeaderBar(
    currentScreen: Screen,
    connectedDevice: DlnaDevice?,
    playbackState: TransportState,
    isLoggedIn: Boolean,
    onBackClick: () -> Unit,
    onCastPillClick: () -> Unit,
    onOpenRemoteClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DeepVioletBg,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Navigation & Branding
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (currentScreen != Screen.SETUP && currentScreen != Screen.LIBRARIES) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .testTag("header_back_button")
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(JellyViolet, CastCyan)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = "JellyCast Logo",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "JellyCast",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when (currentScreen) {
                                Screen.SETUP -> "Server Setup"
                                Screen.LIBRARIES -> "Media Libraries"
                                Screen.MEDIA_LIST -> "Library Catalog"
                                Screen.SEASONS -> "Select Season"
                                Screen.EPISODES -> "Select Episode"
                                Screen.MEDIA_DETAIL -> "Media Overview"
                                Screen.REMOTE_CONTROL -> "DLNA Remote"
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Right: Action buttons & Remote shortcut
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    if (playbackState == TransportState.PLAYING || playbackState == TransportState.PAUSED) {
                        if (currentScreen != Screen.REMOTE_CONTROL) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(JellyViolet.copy(alpha = 0.2f))
                                    .border(1.dp, JellyViolet, RoundedCornerShape(20.dp))
                                    .clickable { onOpenRemoteClick() }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("header_remote_shortcut")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Sensors,
                                        contentDescription = "Active Cast",
                                        tint = JellyViolet,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Remote",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = JellyViolet
                                    )
                                }
                            }
                        }
                    }

                    if (isLoggedIn) {
                        IconButton(
                            onClick = onLogoutClick,
                            modifier = Modifier
                                .testTag("header_logout_button")
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Switch Server or Log Out",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // DLNA Connection Pill (Requirement 8 - clear connection state indicator)
            if (isLoggedIn) {
                val isConnected = connectedDevice != null
                val pillBgColor by animateColorAsState(
                    targetValue = if (isConnected) CastGreenActive.copy(alpha = 0.15f) else WarningAmber.copy(alpha = 0.12f),
                    label = "pillBg"
                )
                val pillBorderColor by animateColorAsState(
                    targetValue = if (isConnected) CastGreenActive.copy(alpha = 0.6f) else WarningAmber.copy(alpha = 0.4f),
                    label = "pillBorder"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(pillBgColor)
                        .border(1.dp, pillBorderColor, RoundedCornerShape(12.dp))
                        .clickable { onCastPillClick() }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                        .testTag("dlna_connection_status_pill")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Pulsing dot or warning icon
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .scale(if (isConnected) pulseScale else 1f)
                                    .clip(CircleShape)
                                    .background(if (isConnected) CastGreenActive else WarningAmber)
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            Icon(
                                imageVector = if (isConnected) Icons.Default.CastConnected else Icons.Default.Cast,
                                contentDescription = "Cast Status",
                                tint = if (isConnected) CastGreenActive else WarningAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            Column {
                                Text(
                                    text = if (isConnected) {
                                        "Connected: ${connectedDevice?.friendlyName}"
                                    } else {
                                        "No DLNA Device Connected"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isConnected) Color(0xFFE2E8F0) else WarningAmber,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isConnected) {
                                        "Ready to cast • Tap to switch"
                                    } else {
                                        "Tap to scan local network for TVs"
                                    },
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Right action badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isConnected) CastGreenActive.copy(alpha = 0.2f) else SurfaceElevated)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isConnected) "CHANGE" else "CONNECT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isConnected) CastGreenActive else WarningAmber
                            )
                        }
                    }
                }
            }
        }
    }
}
