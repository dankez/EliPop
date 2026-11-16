package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.PastelkaApp
import com.example.data.local.PreferencesRepository
import com.example.data.local.UserPreferences
import com.example.data.model.EditorSticker
import com.example.data.model.EditorTextPosition
import com.example.data.model.LiveEffectType
import com.example.data.model.RotationMode
import com.example.data.model.RotationPool
import com.example.data.model.ThemeMode
import com.example.data.model.WallpaperCategory
import com.example.data.model.WallpaperColorFilter
import com.example.data.model.WallpaperItem
import com.example.data.model.WallpaperTarget
import com.example.data.repository.AiStylePreset
import com.example.data.repository.AiWallpaperRepository
import com.example.data.repository.WallpaperRepository
import com.example.wallpaper.RotationScheduler
import com.example.wallpaper.WallpaperApplier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface AiGenUiState {
    data object Idle : AiGenUiState
    data object Loading : AiGenUiState
    data class Success(val bitmap: Bitmap, val title: String) : AiGenUiState
    data class Error(val message: String) : AiGenUiState
}

data class PastelkaUiState(
    val allWallpapers: List<WallpaperItem> = emptyList(),
    val filteredWallpapers: List<WallpaperItem> = emptyList(),
    val favoriteWallpapers: List<WallpaperItem> = emptyList(),
    val customWallpapers: List<WallpaperItem> = emptyList(),
    val selectedCategory: WallpaperCategory = WallpaperCategory.ALL,
    val selectedColorFilter: WallpaperColorFilter = WallpaperColorFilter.ALL,
    val searchQuery: String = "",
    val preferences: UserPreferences = UserPreferences(),
    val currentWallpaper: WallpaperItem? = null,
    val selectedPreviewWallpaper: WallpaperItem? = null,
    val editingWallpaper: WallpaperItem? = null,
    val isRefreshingOnline: Boolean = false,
    val isApplyingWallpaper: Boolean = false,
    val aiGenUiState: AiGenUiState = AiGenUiState.Idle,
    val onlinePage: Int = 1,
)

private data class FilterState(
    val category: WallpaperCategory,
    val colorFilter: WallpaperColorFilter,
    val query: String,
    val page: Int,
)

private data class ModalAndStatusState(
    val preview: WallpaperItem?,
    val editing: WallpaperItem?,
    val isRefreshing: Boolean,
    val isApplying: Boolean,
    val aiState: AiGenUiState,
)

class PastelkaViewModel(
    private val wallpaperRepository: WallpaperRepository,
    private val preferencesRepository: PreferencesRepository,
    private val wallpaperApplier: WallpaperApplier,
    private val rotationScheduler: RotationScheduler,
    private val aiWallpaperRepository: AiWallpaperRepository,
) : ViewModel() {

    private val selectedCategory = MutableStateFlow(WallpaperCategory.ALL)
    private val selectedColorFilter = MutableStateFlow(WallpaperColorFilter.ALL)
    private val searchQuery = MutableStateFlow("")
    private val onlinePage = MutableStateFlow(1)

    private val selectedPreviewWallpaper = MutableStateFlow<WallpaperItem?>(null)
    private val editingWallpaper = MutableStateFlow<WallpaperItem?>(null)
    private val isRefreshingOnline = MutableStateFlow(false)
    private val isApplyingWallpaper = MutableStateFlow(false)
    private val aiGenUiState = MutableStateFlow<AiGenUiState>(AiGenUiState.Idle)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 6)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val filterFlow =
        combine(selectedCategory, selectedColorFilter, searchQuery, onlinePage) {
            cat,
            color,
            query,
            page ->
            FilterState(cat, color, query, page)
        }

    private val modalStatusFlow =
        combine(
            selectedPreviewWallpaper,
            editingWallpaper,
            isRefreshingOnline,
            isApplyingWallpaper,
            aiGenUiState,
        ) { preview, editing, refreshing, applying, ai ->
            ModalAndStatusState(preview, editing, refreshing, applying, ai)
        }

    val uiState: StateFlow<PastelkaUiState> =
        combine(
            wallpaperRepository.observeAllWallpapers(),
            preferencesRepository.preferencesFlow,
            filterFlow,
            modalStatusFlow,
        ) { wallpapers, prefs, filter, status ->
            val q = filter.query.trim().lowercase()
            val filtered =
                wallpapers.filter { item ->
                    val matchesCategory =
                        when (filter.category) {
                            WallpaperCategory.ALL -> true
                            WallpaperCategory.CUSTOM -> item.isUserUploaded
                            else -> item.category == filter.category
                        }
                    val matchesQuery =
                        q.isEmpty() ||
                            item.title.lowercase().contains(q) ||
                            item.category.displayName.lowercase().contains(q) ||
                            item.sourceLabel.lowercase().contains(q)
                    val matchesColor =
                        when (filter.colorFilter) {
                            WallpaperColorFilter.ALL -> true
                            WallpaperColorFilter.BLACK ->
                                item.category == WallpaperCategory.BLACK_GIRLY ||
                                    item.accentHex.equals("#000000", ignoreCase = true) ||
                                    item.accentHex.startsWith("#1") ||
                                    item.accentHex.startsWith("#2")
                            WallpaperColorFilter.PINK ->
                                item.accentHex.contains("ff", ignoreCase = true) ||
                                    item.accentHex.contains("f8", ignoreCase = true) ||
                                    item.accentHex.contains("f4", ignoreCase = true) ||
                                    item.category == WallpaperCategory.COQUETTE
                            WallpaperColorFilter.LAVENDER ->
                                item.accentHex.contains("e1", ignoreCase = true) ||
                                    item.accentHex.contains("ce", ignoreCase = true) ||
                                    item.accentHex.contains("cc", ignoreCase = true) ||
                                    item.category == WallpaperCategory.PASTEL
                            WallpaperColorFilter.PEACH ->
                                item.accentHex.contains("ffab", ignoreCase = true) ||
                                    item.accentHex.contains("ffcc", ignoreCase = true)
                            WallpaperColorFilter.MINT ->
                                item.accentHex.contains("80cb", ignoreCase = true) ||
                                    item.accentHex.contains("99ff", ignoreCase = true) ||
                                    item.category == WallpaperCategory.NATURE
                        }
                    matchesCategory && matchesQuery && matchesColor
                }

            val favorites = wallpapers.filter { it.isFavorite }
            val custom = wallpapers.filter { it.isUserUploaded }
            val current =
                wallpapers.firstOrNull { it.id == prefs.currentWallpaperId }
                    ?: wallpapers.firstOrNull()
            val refreshedPreview =
                status.preview?.let { prev -> wallpapers.firstOrNull { it.id == prev.id } }

            PastelkaUiState(
                allWallpapers = wallpapers,
                filteredWallpapers = filtered,
                favoriteWallpapers = favorites,
                customWallpapers = custom,
                selectedCategory = filter.category,
                selectedColorFilter = filter.colorFilter,
                searchQuery = filter.query,
                preferences = prefs,
                currentWallpaper = current,
                selectedPreviewWallpaper = refreshedPreview,
                editingWallpaper = status.editing,
                isRefreshingOnline = status.isRefreshing,
                isApplyingWallpaper = status.isApplying,
                aiGenUiState = status.aiState,
                onlinePage = filter.page,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PastelkaUiState(),
        )

    init {
        viewModelScope.launch {
            wallpaperRepository.ensureSeeded()
            refreshOnlineBank(silent = true)
        }
    }

    fun selectCategory(category: WallpaperCategory) {
        selectedCategory.value = category
        if (category.isFetchableOnline) {
            val countInCat = uiState.value.allWallpapers.count { it.category == category }
            if (countInCat < 4) {
                refreshOnlineBank(category = category, silent = true)
            }
        }
    }

    fun selectColorFilter(colorFilter: WallpaperColorFilter) {
        selectedColorFilter.value = colorFilter
        if (colorFilter != WallpaperColorFilter.ALL) {
            refreshOnlineBank(silent = true)
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun searchOnlineNow() {
        refreshOnlineBank(loadNextPage = false, silent = false)
    }

    fun refreshOnlineBank(
        category: WallpaperCategory = selectedCategory.value,
        loadNextPage: Boolean = false,
        silent: Boolean = false,
    ) {
        if (isRefreshingOnline.value) return
        val targetCat = if (category == WallpaperCategory.CUSTOM) WallpaperCategory.ALL else category
        val pageToFetch = if (loadNextPage) onlinePage.value + 1 else 1
        val query = searchQuery.value
        val colorHex = selectedColorFilter.value.wallhavenHex

        viewModelScope.launch {
            isRefreshingOnline.value = true
            val result =
                wallpaperRepository.refreshCategoryFromOnlineBank(
                    category = targetCat,
                    page = pageToFetch,
                    customQuery = query,
                    colorHex = colorHex,
                )
            isRefreshingOnline.value = false

            result
                .onSuccess { count ->
                    if (loadNextPage && count > 0) {
                        onlinePage.update { it + 1 }
                    }
                    if (!silent) {
                        if (count > 0) {
                            _messages.emit("Načítaných $count nových pozadí z voľnej banky")
                        } else {
                            _messages.emit("Knižnica je aktuálna")
                        }
                    }
                }
                .onFailure {
                    if (!silent) {
                        _messages.emit(
                            "Online banka je momentálne nedostupná, zobrazená offline kolekcia",
                        )
                    }
                }
        }
    }

    fun openPreview(item: WallpaperItem) {
        selectedPreviewWallpaper.value = item
    }

    fun closePreview() {
        selectedPreviewWallpaper.value = null
    }

    fun openEditor(item: WallpaperItem) {
        selectedPreviewWallpaper.value = null
        editingWallpaper.value = item
    }

    fun closeEditor() {
        editingWallpaper.value = null
    }

    fun toggleFavorite(item: WallpaperItem) {
        viewModelScope.launch {
            wallpaperRepository.toggleFavorite(item.id, item.isFavorite)
            val msg =
                if (!item.isFavorite) {
                    "Pridané medzi obľúbené"
                } else {
                    "Odstránené z obľúbených"
                }
            _messages.emit(msg)
        }
    }

    fun applyWallpaper(item: WallpaperItem, target: WallpaperTarget) {
        if (isApplyingWallpaper.value) return
        viewModelScope.launch {
            isApplyingWallpaper.value = true
            val result = wallpaperApplier.applyWallpaper(item, target)
            isApplyingWallpaper.value = false

            result
                .onSuccess {
                    preferencesRepository.setRotationTarget(target)
                    preferencesRepository.recordWallpaperApplied(
                        wallpaperId = item.id,
                        incrementRotationCount = false,
                    )
                    _messages.emit("Tapeta nastavená (${target.displayName})")
                    selectedPreviewWallpaper.value = null
                }
                .onFailure { err ->
                    _messages.emit(err.message ?: "Nepodarilo sa nastaviť tapetu")
                }
        }
    }

    fun applyMatchingPair(item: WallpaperItem) {
        if (isApplyingWallpaper.value) return
        viewModelScope.launch {
            isApplyingWallpaper.value = true
            val result = wallpaperApplier.applyMatchingPair(item)
            isApplyingWallpaper.value = false

            result
                .onSuccess {
                    preferencesRepository.setRotationTarget(WallpaperTarget.BOTH)
                    preferencesRepository.recordWallpaperApplied(
                        wallpaperId = item.id,
                        incrementRotationCount = false,
                    )
                    _messages.emit(
                        "Zladený pár nastavený: ostrá zamknutá + jemne rozmazaná domovská plocha",
                    )
                    selectedPreviewWallpaper.value = null
                }
                .onFailure { err ->
                    _messages.emit(err.message ?: "Nepodarilo sa nastaviť zladený pár")
                }
        }
    }

    fun saveAndOptionallyApplyEdited(
        item: WallpaperItem,
        blurStrength: Float,
        dimAlpha: Float,
        overlayText: String,
        useSerifFont: Boolean,
        textColorInt: Int,
        textPosition: EditorTextPosition,
        sticker: EditorSticker,
        applyTarget: WallpaperTarget?,
    ) {
        if (isApplyingWallpaper.value) return
        viewModelScope.launch {
            isApplyingWallpaper.value = true
            val sourceBitmap = wallpaperApplier.loadBitmapForItem(item)
            if (sourceBitmap == null) {
                isApplyingWallpaper.value = false
                _messages.emit("Nepodarilo sa načítať obrázok na úpravu")
                return@launch
            }

            val edited =
                withContext(Dispatchers.Default) {
                    wallpaperApplier.renderEditedBitmap(
                        source = sourceBitmap,
                        blurStrength = blurStrength,
                        dimAlpha = dimAlpha,
                        overlayText = overlayText,
                        useSerifFont = useSerifFont,
                        textColorInt = textColorInt,
                        textPosition = textPosition,
                        sticker = sticker,
                    )
                }
            if (!sourceBitmap.isRecycled) sourceBitmap.recycle()

            val customTitle =
                overlayText.trim().take(24).ifBlank { "${item.title} (DIY)" }
            val savedResult = wallpaperRepository.saveEditedBitmapToLibrary(edited, customTitle)

            if (applyTarget != null) {
                val applyResult = wallpaperApplier.applyBitmap(edited, applyTarget)
                if (!edited.isRecycled) edited.recycle()
                isApplyingWallpaper.value = false

                savedResult.onSuccess { savedItem ->
                    applyResult
                        .onSuccess {
                            preferencesRepository.setRotationTarget(applyTarget)
                            preferencesRepository.recordWallpaperApplied(savedItem.id, false)
                            editingWallpaper.value = null
                            _messages.emit("Upravená tapeta uložená a nastavená")
                        }
                        .onFailure { err ->
                            editingWallpaper.value = null
                            _messages.emit(err.message ?: "Uložené, ale nepodarilo sa nastaviť")
                        }
                }.onFailure {
                    isApplyingWallpaper.value = false
                    _messages.emit("Nepodarilo sa uložiť upravenú tapetu")
                }
            } else {
                if (!edited.isRecycled) edited.recycle()
                isApplyingWallpaper.value = false
                savedResult
                    .onSuccess {
                        editingWallpaper.value = null
                        selectedCategory.value = WallpaperCategory.CUSTOM
                        _messages.emit("Upravená tapeta uložená do Moje")
                    }
                    .onFailure {
                        _messages.emit("Nepodarilo sa uložiť upravenú tapetu")
                    }
            }
        }
    }

    fun saveToDevicePictures(item: WallpaperItem) {
        viewModelScope.launch {
            val result = wallpaperApplier.saveToDevicePictures(item)
            result
                .onSuccess { _messages.emit("Tapeta uložená do Galérie telefónu (Pictures/EliPop)") }
                .onFailure { err ->
                    _messages.emit(err.message ?: "Nepodarilo sa uložiť do Galérie")
                }
        }
    }

    fun shareWallpaper(item: WallpaperItem, onUriReady: (Uri) -> Unit) {
        viewModelScope.launch {
            val result = wallpaperApplier.exportShareUri(item)
            result
                .onSuccess { uri -> onUriReady(uri) }
                .onFailure { err ->
                    _messages.emit(err.message ?: "Nepodarilo sa pripraviť zdieľanie")
                }
        }
    }

    fun rotateNow() {
        if (isApplyingWallpaper.value) return
        viewModelScope.launch {
            isApplyingWallpaper.value = true
            val prefs = preferencesRepository.getPreferencesOnce()
            val candidate = wallpaperRepository.pickNextRotationCandidate(prefs)
            if (candidate == null) {
                isApplyingWallpaper.value = false
                _messages.emit("V zdroji striedania nie sú žiadne obrázky")
                return@launch
            }
            val result = wallpaperApplier.applyWallpaper(candidate, prefs.rotationTarget)
            isApplyingWallpaper.value = false

            result
                .onSuccess {
                    preferencesRepository.recordWallpaperApplied(
                        wallpaperId = candidate.id,
                        incrementRotationCount = true,
                    )
                    _messages.emit("Nová tapeta: ${candidate.title}")
                }
                .onFailure { err ->
                    _messages.emit(err.message ?: "Nepodarilo sa vymeniť tapetu")
                }
        }
    }

    fun importGalleryUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val result = wallpaperRepository.importUserWallpapers(uris)
            result
                .onSuccess { imported ->
                    if (imported.isNotEmpty()) {
                        selectedCategory.value = WallpaperCategory.CUSTOM
                        _messages.emit("Pridaných ${imported.size} vlastných obrázkov")
                    } else {
                        _messages.emit("Nepodarilo sa načítať vybrané obrázky")
                    }
                }
                .onFailure { _messages.emit("Chyba pri importe obrázkov z galérie") }
        }
    }

    fun deleteUserWallpaper(item: WallpaperItem) {
        viewModelScope.launch {
            wallpaperRepository.deleteWallpaper(item)
            if (selectedPreviewWallpaper.value?.id == item.id) {
                selectedPreviewWallpaper.value = null
            }
            _messages.emit("Vlastný obrázok odstránený")
        }
    }

    // AI Studio actions
    fun generateAiWallpaper(prompt: String, style: AiStylePreset) {
        if (aiGenUiState.value is AiGenUiState.Loading) return
        viewModelScope.launch {
            aiGenUiState.value = AiGenUiState.Loading
            val result = aiWallpaperRepository.generateWallpaper(prompt, style)
            result
                .onSuccess { generated ->
                    aiGenUiState.value = AiGenUiState.Success(generated.bitmap, generated.title)
                }
                .onFailure { err ->
                    aiGenUiState.value =
                        AiGenUiState.Error(err.message ?: "Nepodarilo sa vygenerovať AI tapetu.")
                }
        }
    }

    fun stylizePhotoFromUri(context: Context, uri: Uri, style: AiStylePreset) {
        if (aiGenUiState.value is AiGenUiState.Loading) return
        viewModelScope.launch {
            aiGenUiState.value = AiGenUiState.Loading
            val sourceBitmap =
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        BitmapFactory.decodeStream(input)
                    }
                }
            if (sourceBitmap == null) {
                aiGenUiState.value = AiGenUiState.Error("Nepodarilo sa načítať vybranú fotku.")
                return@launch
            }

            val result = aiWallpaperRepository.stylizeUserPhoto(sourceBitmap, style)
            if (!sourceBitmap.isRecycled) sourceBitmap.recycle()

            result
                .onSuccess { generated ->
                    aiGenUiState.value = AiGenUiState.Success(generated.bitmap, generated.title)
                }
                .onFailure { err ->
                    aiGenUiState.value =
                        AiGenUiState.Error(err.message ?: "Nepodarilo sa premeniť fotku.")
                }
        }
    }

    fun saveAiBitmapToLibrary(applyImmediately: Boolean, target: WallpaperTarget) {
        val current = aiGenUiState.value as? AiGenUiState.Success ?: return
        viewModelScope.launch {
            val savedResult =
                wallpaperRepository.saveEditedBitmapToLibrary(
                    bitmap = current.bitmap,
                    title = current.title,
                    sourceLabel = "AI Štúdio",
                )
            savedResult
                .onSuccess { savedItem ->
                    if (applyImmediately) {
                        applyWallpaper(savedItem, target)
                    } else {
                        _messages.emit("AI tapeta uložená do Moje")
                    }
                }
                .onFailure {
                    _messages.emit("Nepodarilo sa uložiť AI tapetu")
                }
        }
    }

    fun clearAiState() {
        aiGenUiState.value = AiGenUiState.Idle
    }

    fun setRotationMode(mode: RotationMode) {
        viewModelScope.launch {
            preferencesRepository.setRotationMode(mode)
            rotationScheduler.syncSchedule(mode)
            val msg =
                when (mode) {
                    RotationMode.STATIC -> "Zapnutý statický režim tapety"
                    RotationMode.ON_UNLOCK -> "Tapeta sa zmení pri každom odomknutí telefónu"
                    else -> "Automatická zmena nastavená: ${mode.displayName}"
                }
            _messages.emit(msg)
        }
    }

    fun setRotationTarget(target: WallpaperTarget) {
        viewModelScope.launch { preferencesRepository.setRotationTarget(target) }
    }

    fun setRotationPool(pool: RotationPool) {
        viewModelScope.launch { preferencesRepository.setRotationPool(pool) }
    }

    fun setRotationCategory(category: WallpaperCategory) {
        viewModelScope.launch { preferencesRepository.setRotationCategory(category) }
    }

    fun setLiveEffectType(effect: LiveEffectType) {
        viewModelScope.launch {
            preferencesRepository.setLiveEffectType(effect)
            _messages.emit("Živý efekt: ${effect.displayName}")
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setUseDynamicColor(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setUseDynamicColor(enabled) }
    }

    class Factory(private val app: PastelkaApp) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PastelkaViewModel(
                wallpaperRepository = app.wallpaperRepository,
                preferencesRepository = app.preferencesRepository,
                wallpaperApplier = app.wallpaperApplier,
                rotationScheduler = app.rotationScheduler,
                aiWallpaperRepository = app.aiWallpaperRepository,
            ) as T
        }
    }
}
