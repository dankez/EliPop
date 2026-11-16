package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.WallpaperTarget
import com.example.data.repository.AiStylePreset
import com.example.ui.AiGenUiState

private val PROMPT_IDEAS =
    listOf(
        "Mačiatko s ružovou mašľou v sakurách",
        "Čierne trblietavé motýle na vode",
        "Chibi dievča so zajačikom v pasteli",
        "Perly, ruže a hodvábne stuhy",
    )

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiStudioScreen(
    aiState: AiGenUiState,
    defaultTarget: WallpaperTarget,
    onGenerate: (prompt: String, style: AiStylePreset) -> Unit,
    onStylizePhoto: (android.content.Context, android.net.Uri, AiStylePreset) -> Unit,
    onSaveAiBitmap: (applyImmediately: Boolean, target: WallpaperTarget) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var prompt by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf(AiStylePreset.KAWAII) }

    val photoStylizeLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
            onResult = { uri ->
                if (uri != null) {
                    onStylizePhoto(context, uri, selectedStyle)
                }
            },
        )

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
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoFixHigh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "AI Štúdio tapiet",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Text(
                        text =
                            "Opíš si vysnívanú dievčenskú tapetu alebo premeň svoju fotku na kawaii či pastelové umelecké dielo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    )
                }
            }
        }

        // Style Presets
        item {
            Surface(
                modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "1. Vyber si estetický štýl",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AiStylePreset.entries.forEach { preset ->
                            FilterChip(
                                selected = selectedStyle == preset,
                                onClick = { selectedStyle = preset },
                                label = { Text(preset.displayName) },
                                modifier = Modifier.testTag("ai_style_${preset.name}"),
                            )
                        }
                    }
                    Text(
                        text = selectedStyle.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "2. Opíš motív (alebo vyber inšpiráciu)",
                        style = MaterialTheme.typography.titleMedium,
                    )

                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        label = { Text("Čo má byť na tapete?") },
                        placeholder = { Text("napr. Biele mačiatko s ružovou stuhou…") },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth().testTag("ai_prompt_input"),
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        PROMPT_IDEAS.forEach { idea ->
                            AssistChip(
                                onClick = { prompt = idea },
                                label = { Text(idea, maxLines = 1) },
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = { onGenerate(prompt, selectedStyle) },
                            enabled = aiState !is AiGenUiState.Loading,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.weight(1.2f).height(50.dp).testTag("ai_generate_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Vytvoriť AI tapetu", maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = {
                                photoStylizeLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                            enabled = aiState !is AiGenUiState.Loading,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.weight(1f).height(50.dp).testTag("ai_stylize_photo_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Z mojej fotky", maxLines = 1)
                        }
                    }
                }
            }
        }

        // Result / Loading / Error Section
        item {
            when (aiState) {
                is AiGenUiState.Idle -> Unit

                is AiGenUiState.Loading -> {
                    Surface(
                        modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                                            ),
                                        ),
                                    )
                                    .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = "AI tvorí tvoju unikátnu tapetu…",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = "Generovanie vysokokvalitného 9:16 pozadia zvyčajne trvá 20–50 sekúnd.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }

                is AiGenUiState.Error -> {
                    Surface(
                        modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
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
                                    imageVector = Icons.Filled.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                Text(
                                    text = "Nepodarilo sa vytvoriť AI obrázok",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                            Text(
                                text = aiState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            OutlinedButton(
                                onClick = onClearError,
                                shape = RoundedCornerShape(50),
                            ) {
                                Text("Zavrieť upozornenie")
                            }
                        }
                    }
                }

                is AiGenUiState.Success -> {
                    Surface(
                        modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border =
                            BorderStroke(
                                1.5.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            ),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Text(
                                text = aiState.title,
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Image(
                                bitmap = aiState.bitmap.asImageBitmap(),
                                contentDescription = aiState.title,
                                contentScale = ContentScale.Crop,
                                modifier =
                                    Modifier
                                        .widthIn(max = 260.dp)
                                        .fillMaxWidth()
                                        .aspectRatio(9f / 16f)
                                        .clip(RoundedCornerShape(24.dp))
                                        .clickable { onSaveAiBitmap(false, defaultTarget) },
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                OutlinedButton(
                                    onClick = { onSaveAiBitmap(false, defaultTarget) },
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.weight(1f).height(48.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SaveAlt,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Do knižnice")
                                }
                                Button(
                                    onClick = { onSaveAiBitmap(true, defaultTarget) },
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.weight(1.2f).height(48.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Wallpaper,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Nastaviť tapetu")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
