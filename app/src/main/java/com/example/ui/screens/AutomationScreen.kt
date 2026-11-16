package com.example.ui.screens

import android.Manifest
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.LiveEffectType
import com.example.data.model.RotationMode
import com.example.data.model.RotationPool
import com.example.data.model.WallpaperCategory
import com.example.data.model.WallpaperTarget
import com.example.service.PastelkaLiveWallpaperService
import com.example.ui.PastelkaUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AutomationScreen(
    uiState: PastelkaUiState,
    onSelectRotationMode: (RotationMode) -> Unit,
    onSelectTarget: (WallpaperTarget) -> Unit,
    onSelectPool: (RotationPool) -> Unit,
    onSelectPoolCategory: (WallpaperCategory) -> Unit,
    onSelectLiveEffect: (LiveEffectType) -> Unit,
    onRotateNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefs = uiState.preferences

    val notificationLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { _ -> onSelectRotationMode(RotationMode.ON_UNLOCK) },
        )

    val poolCount =
        remember(uiState.allWallpapers, prefs.rotationPool, prefs.rotationCategory) {
            when (prefs.rotationPool) {
                RotationPool.FAVORITES -> uiState.favoriteWallpapers.size
                RotationPool.CUSTOM_ONLY -> uiState.customWallpapers.size
                RotationPool.CATEGORY ->
                    uiState.allWallpapers.count { it.category == prefs.rotationCategory }
                RotationPool.ALL_SAVED -> uiState.allWallpapers.size
            }
        }

    val formattedTime =
        remember(prefs.lastRotatedAt) {
            if (prefs.lastRotatedAt <= 0L) {
                "Zatiaľ žiadna"
            } else {
                SimpleDateFormat("d. MMM • HH:mm", Locale.forLanguageTag("sk")).format(
                    Date(prefs.lastRotatedAt),
                )
            }
        }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Surface(
                modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "Stav automatiky",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Text(
                        text =
                            "${prefs.rotationMode.displayName} • ${prefs.rotationTarget.displayName}\n" +
                                "Obrázky v zásobníku: $poolCount • Posledná zmena: $formattedTime",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    )
                    Button(
                        onClick = onRotateNow,
                        enabled = !uiState.isApplyingWallpaper,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth().testTag("automation_rotate_now_button"),
                    ) {
                        if (uiState.isApplyingWallpaper) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Shuffle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_rotate_now))
                    }
                }
            }
        }

        // Live Wallpaper Effects Card
        item {
            SectionCard(title = "Živá tapeta (padajúce efekty)") {
                Text(
                    text =
                        "Pridaj na svoju aktívnu tapetu jemné animované sakurové lupene, trblietky alebo srdiečka.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LiveEffectType.entries.forEach { effect ->
                        FilterChip(
                            selected = prefs.liveEffectType == effect,
                            onClick = { onSelectLiveEffect(effect) },
                            label = { Text(effect.displayName) },
                            modifier = Modifier.testTag("live_effect_${effect.name}"),
                        )
                    }
                }
                OutlinedButton(
                    onClick = {
                        val intent =
                            Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                                putExtra(
                                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                    ComponentName(context, PastelkaLiveWallpaperService::class.java),
                                )
                            }
                        runCatching { context.startActivity(intent) }.onFailure {
                            runCatching {
                                context.startActivity(
                                    Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER),
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth().testTag("activate_live_wallpaper_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Wallpaper,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Aktivovať živú tapetu v systéme")
                }
            }
        }

        // 1. Target screen choice
        item {
            SectionCard(title = "Kam meniť tapetu") {
                WallpaperTarget.entries.forEach { target ->
                    val selected = prefs.rotationTarget == target
                    val icon =
                        when (target) {
                            WallpaperTarget.HOME -> Icons.Filled.Home
                            WallpaperTarget.LOCK -> Icons.Filled.Lock
                            WallpaperTarget.BOTH -> Icons.Filled.Smartphone
                        }
                    OptionRow(
                        title = target.displayName,
                        subtitle = target.description,
                        selected = selected,
                        leadingIcon = icon,
                        onClick = { onSelectTarget(target) },
                        testTag = "automation_target_${target.name}",
                    )
                }
            }
        }

        // 2. Rotation cadence
        item {
            SectionCard(title = "Kedy meniť tapetu") {
                RotationMode.entries.forEach { mode ->
                    val selected = prefs.rotationMode == mode
                    val icon =
                        when (mode) {
                            RotationMode.STATIC -> Icons.Filled.Lock
                            RotationMode.ON_UNLOCK -> Icons.Filled.LockOpen
                            else -> Icons.Filled.Schedule
                        }
                    OptionRow(
                        title = mode.displayName,
                        subtitle = mode.description,
                        selected = selected,
                        leadingIcon = icon,
                        onClick = {
                            if (mode == RotationMode.ON_UNLOCK &&
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                            ) {
                                val granted =
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS,
                                    ) == PackageManager.PERMISSION_GRANTED
                                if (!granted) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onSelectRotationMode(mode)
                                }
                            } else {
                                onSelectRotationMode(mode)
                            }
                        },
                        testTag = "rotation_mode_${mode.name}",
                    )
                }
            }
        }

        // 3. Rotation source pool
        item {
            SectionCard(title = "Odkiaľ vyberať obrázky") {
                RotationPool.entries.forEach { pool ->
                    val selected = prefs.rotationPool == pool
                    OptionRow(
                        title = pool.displayName,
                        subtitle = pool.description,
                        selected = selected,
                        leadingIcon = Icons.Filled.AutoAwesome,
                        onClick = { onSelectPool(pool) },
                        testTag = "rotation_pool_${pool.name}",
                    )
                }

                AnimatedVisibility(visible = prefs.rotationPool == RotationPool.CATEGORY) {
                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Vyber kategóriu na striedanie:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            WallpaperCategory.entries
                                .filter { it != WallpaperCategory.ALL }
                                .forEach { cat ->
                                    FilterChip(
                                        selected = prefs.rotationCategory == cat,
                                        onClick = { onSelectPoolCategory(cat) },
                                        label = { Text(cat.displayName) },
                                    )
                                }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            content()
        }
    }
}

@Composable
private fun OptionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    testTag: String,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        color =
            if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        border =
            if (selected) {
                BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
            } else {
                null
            },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint =
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                modifier = Modifier.size(22.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}
