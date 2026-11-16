package com.example.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.model.RotationPool
import com.example.data.model.ThemeMode
import com.example.ui.components.WallpaperEditorDialog
import com.example.ui.components.WallpaperPreviewDialog
import com.example.ui.screens.AiStudioScreen
import com.example.ui.screens.AutomationScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.SettingsScreen
import kotlinx.coroutines.flow.SharedFlow

const val ROUTE_DISCOVER = "discover"
const val ROUTE_FAVORITES = "favorites"
const val ROUTE_AI_STUDIO = "ai_studio"
const val ROUTE_AUTOMATION = "automation"
const val ROUTE_SETTINGS = "settings"

private data class NavDestinationItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val NAV_ITEMS =
    listOf(
        NavDestinationItem(
            route = ROUTE_DISCOVER,
            labelRes = R.string.nav_discover,
            selectedIcon = Icons.Filled.PhotoLibrary,
            unselectedIcon = Icons.Outlined.PhotoLibrary,
        ),
        NavDestinationItem(
            route = ROUTE_FAVORITES,
            labelRes = R.string.nav_favorites,
            selectedIcon = Icons.Filled.Favorite,
            unselectedIcon = Icons.Outlined.FavoriteBorder,
        ),
        NavDestinationItem(
            route = ROUTE_AI_STUDIO,
            labelRes = R.string.nav_ai_studio,
            selectedIcon = Icons.Filled.AutoFixHigh,
            unselectedIcon = Icons.Outlined.AutoFixHigh,
        ),
        NavDestinationItem(
            route = ROUTE_AUTOMATION,
            labelRes = R.string.nav_automation,
            selectedIcon = Icons.Filled.AutoAwesome,
            unselectedIcon = Icons.Outlined.AutoAwesome,
        ),
        NavDestinationItem(
            route = ROUTE_SETTINGS,
            labelRes = R.string.nav_settings,
            selectedIcon = Icons.Filled.Palette,
            unselectedIcon = Icons.Outlined.Palette,
        ),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastelkaRoot(
    uiState: PastelkaUiState,
    messages: SharedFlow<String>,
    viewModel: PastelkaViewModel,
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_DISCOVER
    val snackbarHostState = remember { SnackbarHostState() }

    val galleryPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10),
            onResult = { uris ->
                if (uris.isNotEmpty()) {
                    viewModel.importGalleryUris(uris)
                }
            },
        )

    val launchGalleryPicker = {
        galleryPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    LaunchedEffect(messages) {
        messages.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    val navigateToRoute: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp
        val columnsCount =
            when {
                maxWidth >= 840.dp -> 4
                maxWidth >= 600.dp -> 3
                else -> 2
            }

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                text = stringResource(R.string.hero_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                val nextTheme =
                                    if (uiState.preferences.themeMode == ThemeMode.DARK) {
                                        ThemeMode.LIGHT
                                    } else {
                                        ThemeMode.DARK
                                    }
                                viewModel.setThemeMode(nextTheme)
                            },
                            modifier =
                                Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("topbar_theme_toggle"),
                        ) {
                            Icon(
                                imageVector =
                                    if (uiState.preferences.themeMode == ThemeMode.DARK) {
                                        Icons.Filled.LightMode
                                    } else {
                                        Icons.Filled.DarkMode
                                    },
                                contentDescription = "Prepnúť tmavý režim",
                            )
                        }

                        IconButton(
                            onClick = { viewModel.refreshOnlineBank(loadNextPage = true) },
                            enabled = !uiState.isRefreshingOnline,
                            modifier =
                                Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("topbar_refresh_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Načítať nové tapety",
                            )
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                        ),
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        NAV_ITEMS.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToRoute(item.route) },
                                icon = {
                                    Icon(
                                        imageVector =
                                            if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes),
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(item.labelRes),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                modifier = Modifier.testTag("nav_${item.route}"),
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (currentRoute == ROUTE_DISCOVER) {
                    ExtendedFloatingActionButton(
                        onClick = launchGalleryPicker,
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.AddPhotoAlternate,
                                contentDescription = null,
                            )
                        },
                        text = { Text(stringResource(R.string.action_add_from_gallery)) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_add_gallery"),
                    )
                }
            },
        ) { innerPadding ->
            Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                if (useRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        Spacer(modifier = Modifier.weight(0.2f))
                        NAV_ITEMS.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToRoute(item.route) },
                                icon = {
                                    Icon(
                                        imageVector =
                                            if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes),
                                    )
                                },
                                label = { Text(stringResource(item.labelRes)) },
                                modifier = Modifier.testTag("rail_${item.route}"),
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_DISCOVER,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                ) {
                    composable(ROUTE_DISCOVER) {
                        DiscoverScreen(
                            uiState = uiState,
                            columnsCount = columnsCount,
                            onSelectCategory = viewModel::selectCategory,
                            onSelectColorFilter = viewModel::selectColorFilter,
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            onSearchOnline = viewModel::searchOnlineNow,
                            onRefreshOnline = { next -> viewModel.refreshOnlineBank(loadNextPage = next) },
                            onOpenPreview = viewModel::openPreview,
                            onToggleFavorite = viewModel::toggleFavorite,
                            onRotateNow = viewModel::rotateNow,
                            onPickFromGallery = launchGalleryPicker,
                        )
                    }

                    composable(ROUTE_FAVORITES) {
                        FavoritesScreen(
                            uiState = uiState,
                            columnsCount = columnsCount,
                            onOpenPreview = viewModel::openPreview,
                            onToggleFavorite = viewModel::toggleFavorite,
                            onUseFavoritesForRotation = {
                                viewModel.setRotationPool(RotationPool.FAVORITES)
                                navigateToRoute(ROUTE_AUTOMATION)
                            },
                        )
                    }

                    composable(ROUTE_AI_STUDIO) {
                        AiStudioScreen(
                            aiState = uiState.aiGenUiState,
                            defaultTarget = uiState.preferences.rotationTarget,
                            onGenerate = viewModel::generateAiWallpaper,
                            onStylizePhoto = viewModel::stylizePhotoFromUri,
                            onSaveAiBitmap = viewModel::saveAiBitmapToLibrary,
                            onClearError = viewModel::clearAiState,
                        )
                    }

                    composable(ROUTE_AUTOMATION) {
                        AutomationScreen(
                            uiState = uiState,
                            onSelectRotationMode = viewModel::setRotationMode,
                            onSelectTarget = viewModel::setRotationTarget,
                            onSelectPool = viewModel::setRotationPool,
                            onSelectPoolCategory = viewModel::setRotationCategory,
                            onSelectLiveEffect = viewModel::setLiveEffectType,
                            onRotateNow = viewModel::rotateNow,
                        )
                    }

                    composable(ROUTE_SETTINGS) {
                        SettingsScreen(
                            uiState = uiState,
                            onSelectThemeMode = viewModel::setThemeMode,
                            onToggleDynamicColor = viewModel::setUseDynamicColor,
                            onPickFromGallery = launchGalleryPicker,
                            onRefreshOnlineBank = { viewModel.refreshOnlineBank(loadNextPage = true) },
                        )
                    }
                }
            }
        }

        val previewItem = uiState.selectedPreviewWallpaper
        if (previewItem != null) {
            WallpaperPreviewDialog(
                item = previewItem,
                initialTarget = uiState.preferences.rotationTarget,
                liveEffectType = uiState.preferences.liveEffectType,
                isApplying = uiState.isApplyingWallpaper,
                onDismiss = viewModel::closePreview,
                onToggleFavorite = { viewModel.toggleFavorite(previewItem) },
                onDeleteUserImage = { viewModel.deleteUserWallpaper(previewItem) },
                onOpenEditor = { viewModel.openEditor(previewItem) },
                onSaveToGallery = { viewModel.saveToDevicePictures(previewItem) },
                onShare = {
                    viewModel.shareWallpaper(previewItem) { shareUri ->
                        val sendIntent =
                            Intent(Intent.ACTION_SEND).apply {
                                type = "image/jpeg"
                                putExtra(Intent.EXTRA_STREAM, shareUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                        context.startActivity(
                            Intent.createChooser(sendIntent, "Zdieľať tapetu z EliPop"),
                        )
                    }
                },
                onApplyMatchingPair = { viewModel.applyMatchingPair(previewItem) },
                onApplyWallpaper = { target -> viewModel.applyWallpaper(previewItem, target) },
            )
        }

        val editingItem = uiState.editingWallpaper
        if (editingItem != null) {
            WallpaperEditorDialog(
                item = editingItem,
                defaultTarget = uiState.preferences.rotationTarget,
                isSaving = uiState.isApplyingWallpaper,
                onDismiss = viewModel::closeEditor,
                onSave = { blur, dim, text, serif, colorInt, pos, sticker, applyTarget ->
                    viewModel.saveAndOptionallyApplyEdited(
                        item = editingItem,
                        blurStrength = blur,
                        dimAlpha = dim,
                        overlayText = text,
                        useSerifFont = serif,
                        textColorInt = colorInt,
                        textPosition = pos,
                        sticker = sticker,
                        applyTarget = applyTarget,
                    )
                },
            )
        }
    }
}
