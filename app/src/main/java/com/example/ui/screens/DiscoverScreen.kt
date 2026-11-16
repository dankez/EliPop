package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.WallpaperCategory
import com.example.data.model.WallpaperColorFilter
import com.example.data.model.WallpaperItem
import com.example.ui.PastelkaUiState
import com.example.ui.components.HeroBannerCard
import com.example.ui.components.WallpaperCard

@Composable
fun DiscoverScreen(
    uiState: PastelkaUiState,
    columnsCount: Int,
    onSelectCategory: (WallpaperCategory) -> Unit,
    onSelectColorFilter: (WallpaperColorFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchOnline: () -> Unit,
    onRefreshOnline: (Boolean) -> Unit,
    onOpenPreview: (WallpaperItem) -> Unit,
    onToggleFavorite: (WallpaperItem) -> Unit,
    onRotateNow: () -> Unit,
    onPickFromGallery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columnsCount),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            HeroBannerCard(
                currentWallpaper = uiState.currentWallpaper,
                preferences = uiState.preferences,
                isApplying = uiState.isApplyingWallpaper,
                onRotateNow = onRotateNow,
                onOpenCurrent = onOpenPreview,
            )
        }

        // Search bar + Color filters + Category chips
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = "Hľadať (napr. butterfly, kawaii, ribbon…)",
                            maxLines = 1,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Hľadať",
                        )
                    },
                    trailingIcon = {
                        Row {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = "Vymazať hľadanie",
                                    )
                                }
                            }
                            IconButton(
                                onClick = onSearchOnline,
                                modifier = Modifier.testTag("online_search_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Hľadať v online banke",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearchOnline() }),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedBorderColor =
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        ),
                    modifier = Modifier.fillMaxWidth().testTag("discover_search_input"),
                )

                // Color Swatch Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 8.dp),
                ) {
                    items(WallpaperColorFilter.entries) { colorFilter ->
                        val selected = uiState.selectedColorFilter == colorFilter
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectColorFilter(colorFilter) },
                            leadingIcon = {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(colorFilter.swatchColor),
                                )
                            },
                            label = {
                                Text(
                                    text = colorFilter.displayName,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            },
                            colors =
                                FilterChipDefaults.filterChipColors(
                                    selectedContainerColor =
                                        MaterialTheme.colorScheme.secondaryContainer,
                                ),
                            border =
                                if (selected) {
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                } else {
                                    null
                                },
                            modifier = Modifier.testTag("color_chip_${colorFilter.name}"),
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Kategórie pozadí",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    if (uiState.isRefreshingOnline) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                            Text(
                                text = "Načítavam…",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 8.dp),
                ) {
                    items(WallpaperCategory.entries) { category ->
                        val selected = uiState.selectedCategory == category
                        val icon =
                            when (category) {
                                WallpaperCategory.ALL -> Icons.Filled.AutoAwesome
                                WallpaperCategory.KAWAII -> Icons.Filled.Face
                                WallpaperCategory.BLACK_GIRLY -> Icons.Filled.DarkMode
                                WallpaperCategory.COQUETTE -> Icons.Filled.Favorite
                                WallpaperCategory.PETS -> Icons.Filled.Pets
                                WallpaperCategory.NATURE -> Icons.Filled.Park
                                WallpaperCategory.FLOWERS -> Icons.Filled.LocalFlorist
                                WallpaperCategory.PASTEL -> Icons.Filled.Palette
                                WallpaperCategory.CUSTOM -> Icons.Filled.PhotoLibrary
                            }
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectCategory(category) },
                            leadingIcon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            label = {
                                Text(
                                    text = category.displayName,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                            colors =
                                FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                                ),
                            modifier = Modifier.testTag("category_chip_${category.name}"),
                        )
                    }
                }
            }
        }

        if (uiState.filteredWallpapers.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyCategoryState(
                    category = uiState.selectedCategory,
                    hasSearchOrColor =
                        uiState.searchQuery.isNotBlank() ||
                            uiState.selectedColorFilter != WallpaperColorFilter.ALL,
                    onPickFromGallery = onPickFromGallery,
                    onRefreshOnline = { onRefreshOnline(false) },
                )
            }
        } else {
            items(
                items = uiState.filteredWallpapers,
                key = { it.id },
            ) { wallpaper ->
                WallpaperCard(
                    item = wallpaper,
                    isCurrent = uiState.currentWallpaper?.id == wallpaper.id,
                    onClick = { onOpenPreview(wallpaper) },
                    onToggleFavorite = { onToggleFavorite(wallpaper) },
                )
            }

            if (uiState.selectedCategory != WallpaperCategory.CUSTOM) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        OutlinedButton(
                            onClick = { onRefreshOnline(true) },
                            enabled = !uiState.isRefreshingOnline,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("load_more_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(R.string.action_refresh_bank))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCategoryState(
    category: WallpaperCategory,
    hasSearchOrColor: Boolean,
    onPickFromGallery: () -> Unit,
    onRefreshOnline: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector =
                    if (category == WallpaperCategory.CUSTOM) {
                        Icons.Filled.AddPhotoAlternate
                    } else {
                        Icons.Filled.AutoAwesome
                    },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp),
            )
            Text(
                text =
                    when {
                        hasSearchOrColor -> "Žiadne lokálne zhody pre tento filter"
                        category == WallpaperCategory.CUSTOM -> "Zatiaľ nemáš vlastné fotky"
                        else -> "V tejto kategórii ešte nič nie je"
                    },
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                text =
                    when {
                        hasSearchOrColor ->
                            "Ťukni na tlačidlo nižšie a stiahni nové pozadia priamo z online banky pre zvolené slovo alebo farbu."
                        category == WallpaperCategory.CUSTOM ->
                            "Pridaj si obľúbené fotografie priamo z galérie telefónu a nastav ich ako tapetu alebo do striedania."
                        else -> "Skús načítať čerstvé pozadia z našej bezplatnej banky."
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (category == WallpaperCategory.CUSTOM && !hasSearchOrColor) {
                Button(
                    onClick = onPickFromGallery,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("empty_upload_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_add_from_gallery))
                }
            } else {
                Button(
                    onClick = onRefreshOnline,
                    shape = RoundedCornerShape(50),
                ) {
                    Text(stringResource(R.string.action_refresh_bank))
                }
            }
        }
    }
}
