package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.WallpaperItem
import java.io.File

@Composable
fun WallpaperAsyncImage(
    item: WallpaperItem,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    fullQuality: Boolean = false,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current

    val model: Any? =
        remember(item.id, item.localFilePath, item.drawableName, item.previewUrl, item.fullImageUrl, fullQuality) {
            when {
                !item.localFilePath.isNullOrBlank() -> File(item.localFilePath)
                !item.drawableName.isNullOrBlank() -> {
                    val resId =
                        context.resources.getIdentifier(
                            item.drawableName,
                            "drawable",
                            context.packageName,
                        )
                    if (resId != 0) resId else null
                }
                fullQuality -> item.fullImageUrl ?: item.previewUrl
                else -> item.previewUrl ?: item.fullImageUrl
            }
        }

    val imageRequest =
        remember(context, model) {
            model?.let {
                ImageRequest.Builder(context)
                    .data(it)
                    .crossfade(true)
                    .build()
            }
        }

    val placeholderGradient =
        Brush.linearGradient(
            colors =
                listOf(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                ),
        )

    Box(modifier = modifier.background(placeholderGradient)) {
        if (imageRequest != null) {
            AsyncImage(
                model = imageRequest,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
