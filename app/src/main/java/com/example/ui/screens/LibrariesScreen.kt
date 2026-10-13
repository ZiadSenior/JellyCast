package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DlnaDevice
import com.example.data.model.JellyfinLibrary
import com.example.ui.theme.CastCyan
import com.example.ui.theme.DeepVioletBg
import com.example.ui.theme.JellyIndigo
import com.example.ui.theme.JellyViolet
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun LibrariesScreen(
    libraries: List<JellyfinLibrary>,
    isLoading: Boolean,
    connectedDevice: DlnaDevice?,
    serverUrl: String,
    onSelectLibrary: (JellyfinLibrary) -> Unit,
    onRefresh: () -> Unit,
    onOpenCastScanner: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DeepVioletBg
    ) {
        if (isLoading && libraries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = JellyViolet)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Loading libraries from Jellyfin...", color = TextSecondary, fontSize = 13.sp)
                }
            }
            return@Surface
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("libraries_grid"),
            contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header notice if not connected to DLNA
            if (connectedDevice == null) {
                item(span = { GridItemSpan(2) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(WarningAmber.copy(alpha = 0.15f), SurfaceVariantDark)
                                )
                            )
                            .border(1.dp, WarningAmber.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .clickable { onOpenCastScanner() }
                            .padding(14.dp)
                            .testTag("libraries_dlna_warning_banner")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(WarningAmber.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cast,
                                        contentDescription = null,
                                        tint = WarningAmber,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Pair with a DLNA Player",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Tap here to connect to your TV for casting",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Scan",
                                tint = WarningAmber
                            )
                        }
                    }
                }
            }

            // Section Title
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Server Libraries",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Choose a collection to browse media",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("refresh_libraries_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Libraries",
                            tint = JellyViolet
                        )
                    }
                }
            }

            if (libraries.isEmpty() && !isLoading) {
                item(span = { GridItemSpan(2) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No libraries found on server",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(
                    items = libraries,
                    key = { it.id },
                    contentType = { "library_card" }
                ) { lib ->
                    LibraryCard(library = lib, onClick = { onSelectLibrary(lib) })
                }
            }
        }
    }
}

@Composable
private fun LibraryCard(
    library: JellyfinLibrary,
    onClick: () -> Unit
) {
    val (icon, gradientColors) = getLibraryVisuals(library.collectionType)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceDark)
            .border(1.dp, SurfaceElevated, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag("library_card_${library.id}")
    ) {
        // Decorative background gradient corner
        Box(
            modifier = Modifier
                .size(90.dp)
                .align(Alignment.BottomEnd)
                .clip(RoundedCornerShape(topStart = 80.dp))
                .background(Brush.linearGradient(gradientColors.map { it.copy(alpha = 0.25f) }))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(gradientColors)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = library.name,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Info
            Column {
                Text(
                    text = library.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
                Text(
                    text = library.collectionType.replaceFirstChar { it.uppercase() },
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

private fun getLibraryVisuals(collectionType: String): Pair<ImageVector, List<Color>> {
    return when (collectionType.lowercase()) {
        "movies" -> Icons.Default.Movie to listOf(JellyViolet, JellyIndigo)
        "tvshows" -> Icons.Default.Tv to listOf(CastCyan, JellyIndigo)
        "music" -> Icons.Default.MusicNote to listOf(Color(0xFFEC4899), JellyViolet)
        "homevideos" -> Icons.Default.Videocam to listOf(Color(0xFF10B981), CastCyan)
        else -> Icons.Default.Theaters to listOf(JellyViolet, CastCyan)
    }
}
