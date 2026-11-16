package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.LiveEffectType
import com.example.data.model.WallpaperItem
import com.example.data.model.WallpaperTarget
import kotlin.math.sin
import kotlinx.coroutines.delay

private enum class MockupMode(val label: String) {
    CLEAN("Čistý"),
    LOCK("Zamknutá"),
    HOME("Plocha"),
    LIVE("Živý efekt"),
}

@Composable
fun WallpaperPreviewDialog(
    item: WallpaperItem,
    initialTarget: WallpaperTarget,
    liveEffectType: LiveEffectType,
    isApplying: Boolean,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDeleteUserImage: () -> Unit,
    onOpenEditor: () -> Unit,
    onSaveToGallery: () -> Unit,
    onShare: () -> Unit,
    onApplyMatchingPair: () -> Unit,
    onApplyWallpaper: (WallpaperTarget) -> Unit,
) {
    var selectedTarget by remember(item.id, initialTarget) { mutableStateOf(initialTarget) }
    var mockupMode by remember { mutableStateOf(MockupMode.LOCK) }
    var animTick by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(mockupMode) {
        if (mockupMode == MockupMode.LIVE) {
            while (true) {
                animTick += 0.04f
                delay(32L)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BackHandler(onBack = onDismiss)

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black),
        ) {
            WallpaperAsyncImage(
                item = item,
                contentDescription = item.title,
                fullQuality = true,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            // Live particle preview overlay (Petals / Sparkles / Hearts)
            if (mockupMode == MockupMode.LIVE) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val count = 24
                    for (i in 0 until count) {
                        val seed = i * 1.37f
                        val xFrac = ((seed * 0.29f) % 1f + sin(animTick + seed) * 0.03f)
                        val yFrac = ((seed * 0.17f + animTick * (0.08f + (i % 4) * 0.02f)) % 1.05f)
                        val cx = size.width * xFrac.coerceIn(0f, 1f)
                        val cy = size.height * yFrac
                        val color =
                            when (liveEffectType) {
                                LiveEffectType.SPARKLES -> Color(0xFFFFFAF0).copy(alpha = 0.85f)
                                LiveEffectType.HEARTS -> Color(0xFFFF80AB).copy(alpha = 0.8f)
                                else -> Color(0xFFFFC1DC).copy(alpha = 0.85f)
                            }
                        drawCircle(
                            color = color,
                            radius = (6f + (i % 5) * 2.5f),
                            center = Offset(cx, cy),
                        )
                    }
                }
            }

            // CutePop-inspired realistic Lock Screen preview overlay (pastel clock + bottom shortcuts)
            AnimatedVisibility(
                visible = mockupMode == MockupMode.LOCK,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier =
                            Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(top = 82.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = Color(0xFFFFD8E8),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "09:41",
                            style =
                                TextStyle(
                                    fontSize = 72.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFFFE4F0),
                                    shadow =
                                        Shadow(
                                            color = Color.Black.copy(alpha = 0.45f),
                                            blurRadius = 14f,
                                        ),
                                ),
                        )
                        Text(
                            text = "Pondelok, 16. november",
                            style =
                                MaterialTheme.typography.titleMedium.copy(
                                    shadow =
                                        Shadow(
                                            color = Color.Black.copy(alpha = 0.4f),
                                            blurRadius = 8f,
                                        ),
                                ),
                            color = Color.White,
                        )
                    }

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(top = 340.dp, start = 28.dp, end = 28.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.35f),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.FlashlightOn,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.35f),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.CameraAlt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Realistic Home Screen icon grid preview overlay
            AnimatedVisibility(
                visible = mockupMode == MockupMode.HOME,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 128.dp),
            ) {
                Column(
                    modifier =
                        Modifier
                            .widthIn(max = 360.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    repeat(3) { rowIdx ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            repeat(4) { colIdx ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    Surface(
                                        modifier = Modifier.size(52.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color.White.copy(alpha = 0.36f),
                                        border =
                                            BorderStroke(
                                                1.dp,
                                                Color.White.copy(alpha = 0.4f),
                                            ),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.Apps,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.92f),
                                                modifier = Modifier.size(22.dp),
                                            )
                                        }
                                    }
                                    Text(
                                        text = "App ${rowIdx * 4 + colIdx + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.92f),
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Top bar overlay
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent),
                            ),
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier =
                            Modifier
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.35f))
                                .testTag("preview_close_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zavrieť náhľad",
                            tint = Color.White,
                        )
                    }
                    Column {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                        )
                        Text(
                            text = "${item.category.displayName} • ${item.sourceLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1,
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onSaveToGallery,
                        modifier =
                            Modifier
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.35f))
                                .testTag("preview_save_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = "Stiahnuť do telefónu",
                            tint = Color.White,
                        )
                    }

                    IconButton(
                        onClick = onShare,
                        modifier =
                            Modifier
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.35f))
                                .testTag("preview_share_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Zdieľať tapetu",
                            tint = Color.White,
                        )
                    }

                    if (item.isUserUploaded) {
                        IconButton(
                            onClick = onDeleteUserImage,
                            modifier =
                                Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.35f))
                                    .testTag("preview_delete_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Odstrániť vlastný obrázok",
                                tint = Color.White,
                            )
                        }
                    }

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier =
                            Modifier
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.35f))
                                .testTag("preview_favorite_button"),
                    ) {
                        Icon(
                            imageVector =
                                if (item.isFavorite) {
                                    Icons.Filled.Favorite
                                } else {
                                    Icons.Outlined.FavoriteBorder
                                },
                            contentDescription = "Obľúbená tapeta",
                            tint = if (item.isFavorite) Color(0xFFFF6E9B) else Color.White,
                        )
                    }
                }
            }

            // Bottom control sheet
            Surface(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                        .padding(14.dp)
                        .navigationBarsPadding(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Mockup mode switcher
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        MockupMode.entries.forEach { mode ->
                            FilterChip(
                                selected = mockupMode == mode,
                                onClick = { mockupMode = mode },
                                label = {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors =
                                    FilterChipDefaults.filterChipColors(
                                        selectedContainerColor =
                                            MaterialTheme.colorScheme.secondaryContainer,
                                    ),
                            )
                        }
                    }

                    // DIY Editor & Matching Pair actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = onOpenEditor,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.weight(1f).testTag("preview_diy_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("DIY úprava", maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = onApplyMatchingPair,
                            enabled = !isApplying,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.weight(1f).testTag("preview_pair_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoFixHigh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Zladený pár", maxLines = 1)
                        }
                    }

                    // Target screen choice
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        WallpaperTarget.entries.forEach { target ->
                            val selected = selectedTarget == target
                            val icon =
                                when (target) {
                                    WallpaperTarget.HOME -> Icons.Filled.Home
                                    WallpaperTarget.LOCK -> Icons.Filled.Lock
                                    WallpaperTarget.BOTH -> Icons.Filled.Smartphone
                                }
                            FilterChip(
                                selected = selected,
                                onClick = { selectedTarget = target },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (selected) Icons.Filled.Check else icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                                label = {
                                    Text(
                                        text =
                                            when (target) {
                                                WallpaperTarget.HOME -> "Plocha"
                                                WallpaperTarget.LOCK -> "Zamknutá"
                                                WallpaperTarget.BOTH -> "Obe"
                                            },
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                    )
                                },
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .testTag("target_chip_${target.name}"),
                            )
                        }
                    }

                    Button(
                        onClick = { onApplyWallpaper(selectedTarget) },
                        enabled = !isApplying,
                        shape = RoundedCornerShape(50),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("apply_wallpaper_button"),
                    ) {
                        if (isApplying) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Nastavujem tapetu…")
                        } else {
                            Text(
                                text =
                                    "${stringResource(R.string.action_set_wallpaper)} (${selectedTarget.displayName})",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}
