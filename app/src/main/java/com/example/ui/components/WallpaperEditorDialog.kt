package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.EditorSticker
import com.example.data.model.EditorTextPosition
import com.example.data.model.WallpaperItem
import com.example.data.model.WallpaperTarget
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.theme.PlusJakartaSansFontFamily

private val TEXT_SWATCHES =
    listOf(
        Color.White,
        Color(0xFFFFB0CB),
        Color(0xFFFFE082),
        Color(0xFF231820),
    )

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WallpaperEditorDialog(
    item: WallpaperItem,
    defaultTarget: WallpaperTarget,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        blurStrength: Float,
        dimAlpha: Float,
        overlayText: String,
        useSerifFont: Boolean,
        textColorInt: Int,
        textPosition: EditorTextPosition,
        sticker: EditorSticker,
        applyTarget: WallpaperTarget?,
    ) -> Unit,
) {
    var blurStrength by remember { mutableFloatStateOf(0f) }
    var dimAlpha by remember { mutableFloatStateOf(0f) }
    var overlayText by remember { mutableStateOf("") }
    var useSerifFont by remember { mutableStateOf(true) }
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    var textPosition by remember { mutableStateOf(EditorTextPosition.BOTTOM) }
    var sticker by remember { mutableStateOf(EditorSticker.SPARKLES) }

    val textColor = TEXT_SWATCHES[selectedColorIndex]

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BackHandler(onBack = onDismiss)

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("editor_close"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zavrieť editor",
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DIY Štúdio úprav",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = "Pridaj meno, rozmazanie pod ikony alebo nálepku",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Live Preview Phone Frame
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier = Modifier.widthIn(max = 260.dp).fillMaxSize(),
                        shape = RoundedCornerShape(32.dp),
                        border = BorderStroke(3.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        shadowElevation = 8.dp,
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            WallpaperAsyncImage(
                                item = item,
                                contentDescription = item.title,
                                fullQuality = true,
                                contentScale = ContentScale.Crop,
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .blur((blurStrength * 22f).dp),
                            )

                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF0F0A12).copy(alpha = dimAlpha * 0.7f)),
                            )

                            if (overlayText.isNotBlank() || sticker != EditorSticker.NONE) {
                                val alignment =
                                    when (textPosition) {
                                        EditorTextPosition.TOP -> Alignment.TopCenter
                                        EditorTextPosition.CENTER -> Alignment.Center
                                        EditorTextPosition.BOTTOM -> Alignment.BottomCenter
                                    }
                                Column(
                                    modifier =
                                        Modifier
                                            .align(alignment)
                                            .padding(horizontal = 16.dp, vertical = 42.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    if (sticker != EditorSticker.NONE) {
                                        Text(
                                            text = sticker.symbol,
                                            color = textColor,
                                            fontSize = 18.sp,
                                        )
                                    }
                                    if (overlayText.isNotBlank()) {
                                        Text(
                                            text = overlayText,
                                            textAlign = TextAlign.Center,
                                            style =
                                                TextStyle(
                                                    color = textColor,
                                                    fontSize = 26.sp,
                                                    fontFamily =
                                                        if (useSerifFont) {
                                                            PlayfairDisplayFontFamily
                                                        } else {
                                                            PlusJakartaSansFontFamily
                                                        },
                                                    fontWeight = FontWeight.Bold,
                                                    fontStyle =
                                                        if (useSerifFont) {
                                                            FontStyle.Italic
                                                        } else {
                                                            FontStyle.Normal
                                                        },
                                                    shadow =
                                                        Shadow(
                                                            color = Color.Black.copy(alpha = 0.55f),
                                                            blurRadius = 10f,
                                                        ),
                                                ),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Controls Panel
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 4.dp,
                ) {
                    Column(
                        modifier =
                            Modifier
                                .widthIn(max = 600.dp)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedTextField(
                            value = overlayText,
                            onValueChange = { if (it.length <= 28) overlayText = it },
                            label = { Text("Vlastné meno alebo citát") },
                            placeholder = { Text("napr. Ninka ♡ alebo Shine Bright") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().testTag("editor_text_input"),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = useSerifFont,
                                    onClick = { useSerifFont = true },
                                    label = { Text("Romantické") },
                                )
                                FilterChip(
                                    selected = !useSerifFont,
                                    onClick = { useSerifFont = false },
                                    label = { Text("Moderné") },
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TEXT_SWATCHES.forEachIndexed { idx, swatch ->
                                    val selected = selectedColorIndex == idx
                                    Surface(
                                        modifier =
                                            Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .clickable { selectedColorIndex = idx },
                                        shape = CircleShape,
                                        color = swatch,
                                        border =
                                            BorderStroke(
                                                if (selected) 2.5.dp else 1.dp,
                                                if (selected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.outline
                                                },
                                            ),
                                    ) {
                                        if (selected) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Filled.Check,
                                                    contentDescription = null,
                                                    tint =
                                                        if (swatch == Color(0xFF231820)) {
                                                            Color.White
                                                        } else {
                                                            Color(0xFF231820)
                                                        },
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            EditorTextPosition.entries.forEach { pos ->
                                FilterChip(
                                    selected = textPosition == pos,
                                    onClick = { textPosition = pos },
                                    label = { Text(pos.displayName) },
                                )
                            }
                            EditorSticker.entries.forEach { st ->
                                FilterChip(
                                    selected = sticker == st,
                                    onClick = { sticker = st },
                                    label = { Text(st.displayName) },
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Rozmazanie pod ikony",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                                Slider(
                                    value = blurStrength,
                                    onValueChange = { blurStrength = it },
                                    modifier = Modifier.testTag("editor_blur_slider"),
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Jemné stmavenie",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                                Slider(
                                    value = dimAlpha,
                                    onValueChange = { dimAlpha = it },
                                    modifier = Modifier.testTag("editor_dim_slider"),
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onSave(
                                        blurStrength,
                                        dimAlpha,
                                        overlayText,
                                        useSerifFont,
                                        textColor.toArgb(),
                                        textPosition,
                                        sticker,
                                        null,
                                    )
                                },
                                enabled = !isSaving,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.weight(1f).height(48.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SaveAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Uložiť")
                            }

                            Button(
                                onClick = {
                                    onSave(
                                        blurStrength,
                                        dimAlpha,
                                        overlayText,
                                        useSerifFont,
                                        textColor.toArgb(),
                                        textPosition,
                                        sticker,
                                        defaultTarget,
                                    )
                                },
                                enabled = !isSaving,
                                shape = RoundedCornerShape(50),
                                modifier =
                                    Modifier
                                        .weight(1.3f)
                                        .height(48.dp)
                                        .testTag("editor_save_apply_button"),
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Text("Uložiť a nastaviť")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
