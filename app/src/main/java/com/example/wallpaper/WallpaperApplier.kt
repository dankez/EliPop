package com.example.wallpaper

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.model.EditorSticker
import com.example.data.model.EditorTextPosition
import com.example.data.model.WallpaperItem
import com.example.data.model.WallpaperTarget
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class WallpaperApplier(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
) {
    suspend fun applyWallpaper(
        item: WallpaperItem,
        target: WallpaperTarget,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val bitmap =
                    loadBitmapForItem(item)
                        ?: error("Nepodarilo sa načítať obrázok tapety.")
                try {
                    setBitmapToWallpaperManager(bitmap, target)
                } finally {
                    if (!bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                }
            }
        }

    suspend fun applyBitmap(
        bitmap: Bitmap,
        target: WallpaperTarget,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                setBitmapToWallpaperManager(bitmap, target)
            }
        }

    suspend fun applyMatchingPair(item: WallpaperItem): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val crispBitmap =
                    loadBitmapForItem(item)
                        ?: error("Nepodarilo sa načítať obrázok tapety.")
                val softHomeBitmap =
                    renderEditedBitmap(
                        source = crispBitmap,
                        blurStrength = 0.65f,
                        dimAlpha = 0.16f,
                        overlayText = "",
                        useSerifFont = true,
                        textColorInt = Color.WHITE,
                        textPosition = EditorTextPosition.BOTTOM,
                        sticker = EditorSticker.NONE,
                    )
                try {
                    val manager = WallpaperManager.getInstance(context)
                    manager.setBitmap(crispBitmap, null, true, WallpaperManager.FLAG_LOCK)
                    manager.setBitmap(softHomeBitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                    Unit
                } finally {
                    if (!crispBitmap.isRecycled) crispBitmap.recycle()
                    if (!softHomeBitmap.isRecycled) softHomeBitmap.recycle()
                }
            }
        }

    suspend fun exportShareUri(item: WallpaperItem): Result<Uri> =
        withContext(Dispatchers.IO) {
            runCatching {
                val bitmap =
                    loadBitmapForItem(item)
                        ?: error("Nepodarilo sa pripraviť obrázok na zdieľanie.")
                try {
                    val shareDir = File(context.cacheDir, "shared_wallpapers").apply { mkdirs() }
                    val shareFile = File(shareDir, "pastelka_share_${item.id}.jpg")
                    shareFile.outputStream().use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                    }
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        shareFile,
                    )
                } finally {
                    if (!bitmap.isRecycled) bitmap.recycle()
                }
            }
        }

    suspend fun saveToDevicePictures(item: WallpaperItem): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    error("Ukladanie priamo do Galérie bez povolení vyžaduje Android 10+. Použi Zdieľať.")
                }
                val bitmap =
                    loadBitmapForItem(item)
                        ?: error("Nepodarilo sa načítať obrázok tapety.")
                try {
                    val fileName = "EliPop_${System.currentTimeMillis()}.jpg"
                    val values =
                        ContentValues().apply {
                            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                            put(
                                MediaStore.Images.Media.RELATIVE_PATH,
                                "${Environment.DIRECTORY_PICTURES}/EliPop",
                            )
                            put(MediaStore.Images.Media.IS_PENDING, 1)
                        }
                    val resolver = context.contentResolver
                    val uri =
                        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                            ?: error("Nepodarilo sa vytvoriť súbor v Galérii.")
                    resolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    } ?: error("Nepodarilo sa zapísať obrázok.")
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    Unit
                } finally {
                    if (!bitmap.isRecycled) bitmap.recycle()
                }
            }
        }

    suspend fun loadBitmapForItem(item: WallpaperItem): Bitmap? =
        withContext(Dispatchers.IO) {
            val metrics = context.resources.displayMetrics
            val targetWidth = metrics.widthPixels.coerceAtLeast(1080)
            val targetHeight = metrics.heightPixels.coerceAtLeast(1920)

            if (!item.localFilePath.isNullOrBlank()) {
                val file = File(item.localFilePath)
                if (file.exists()) {
                    return@withContext decodeSampledFile(file, targetWidth, targetHeight)
                }
            }

            if (!item.drawableName.isNullOrBlank()) {
                val resId =
                    context.resources.getIdentifier(
                        item.drawableName,
                        "drawable",
                        context.packageName,
                    )
                if (resId != 0) {
                    return@withContext decodeSampledResource(resId, targetWidth, targetHeight)
                }
            }

            val remoteUrl = item.fullImageUrl ?: item.previewUrl ?: return@withContext null
            val cacheDir = File(context.cacheDir, "wallpaper_downloads").apply { mkdirs() }
            val cachedFile = File(cacheDir, "${item.id}.jpg")
            if (!cachedFile.exists() || cachedFile.length() == 0L) {
                val request = Request.Builder().url(remoteUrl).build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        error("Chyba sťahovania tapety (${response.code})")
                    }
                    val body = response.body ?: error("Prázdna odpoveď servera")
                    cachedFile.outputStream().use { output ->
                        body.byteStream().use { input -> input.copyTo(output) }
                    }
                }
            }
            decodeSampledFile(cachedFile, targetWidth, targetHeight)
        }

    fun renderEditedBitmap(
        source: Bitmap,
        blurStrength: Float,
        dimAlpha: Float,
        overlayText: String,
        useSerifFont: Boolean,
        textColorInt: Int,
        textPosition: EditorTextPosition,
        sticker: EditorSticker,
    ): Bitmap {
        val base =
            if (blurStrength > 0.04f) {
                fastDownscaleBlur(source, blurStrength)
            } else {
                source.copy(Bitmap.Config.ARGB_8888, true)
            }

        val canvas = Canvas(base)
        val w = base.width.toFloat()
        val h = base.height.toFloat()

        if (dimAlpha > 0.01f) {
            val dimPaint =
                Paint().apply {
                    color = Color.argb((dimAlpha.coerceIn(0f, 0.7f) * 255).toInt(), 15, 10, 18)
                }
            canvas.drawRect(0f, 0f, w, h, dimPaint)
        }

        val trimmedText = overlayText.trim()
        if (trimmedText.isNotEmpty() || sticker != EditorSticker.NONE) {
            val textPaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textColorInt
                    textAlign = Paint.Align.CENTER
                    textSize = (w * 0.082f).coerceIn(38f, 110f)
                    typeface =
                        if (useSerifFont) {
                            Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
                        } else {
                            Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                        }
                    setShadowLayer(12f, 0f, 4f, Color.argb(140, 0, 0, 0))
                }

            val yBase =
                when (textPosition) {
                    EditorTextPosition.TOP -> h * 0.22f
                    EditorTextPosition.CENTER -> h * 0.50f
                    EditorTextPosition.BOTTOM -> h * 0.78f
                }

            if (sticker != EditorSticker.NONE) {
                val stickerPaint =
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = textColorInt
                        textAlign = Paint.Align.CENTER
                        textSize = (w * 0.055f).coerceIn(28f, 76f)
                        setShadowLayer(8f, 0f, 3f, Color.argb(120, 0, 0, 0))
                    }
                val stickerY = if (trimmedText.isNotEmpty()) yBase - textPaint.textSize * 0.95f else yBase
                canvas.drawText(sticker.symbol, w / 2f, stickerY, stickerPaint)
            }

            if (trimmedText.isNotEmpty()) {
                canvas.drawText(trimmedText, w / 2f, yBase, textPaint)
            }
        }

        return base
    }

    private fun fastDownscaleBlur(source: Bitmap, strength: Float): Bitmap {
        val factor = (1f - strength.coerceIn(0f, 1f) * 0.88f).coerceIn(0.08f, 0.9f)
        val smallW = (source.width * factor * 0.25f).toInt().coerceAtLeast(16)
        val smallH = (source.height * factor * 0.25f).toInt().coerceAtLeast(24)
        val downscaled = Bitmap.createScaledBitmap(source, smallW, smallH, true)
        val upscaled = Bitmap.createScaledBitmap(downscaled, source.width, source.height, true)
        if (downscaled != upscaled && !downscaled.isRecycled) {
            downscaled.recycle()
        }
        return upscaled.copy(Bitmap.Config.ARGB_8888, true).also {
            if (it != upscaled && !upscaled.isRecycled) upscaled.recycle()
        }
    }

    private fun setBitmapToWallpaperManager(bitmap: Bitmap, target: WallpaperTarget) {
        val manager = WallpaperManager.getInstance(context)
        val flags =
            when (target) {
                WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
                WallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
                WallpaperTarget.BOTH ->
                    WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
            }
        manager.setBitmap(bitmap, null, true, flags)
    }

    private fun decodeSampledFile(
        file: File,
        reqWidth: Int,
        reqHeight: Int,
    ): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val options =
            BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds, reqWidth, reqHeight)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
        return BitmapFactory.decodeFile(file.absolutePath, options)
    }

    private fun decodeSampledResource(
        resId: Int,
        reqWidth: Int,
        reqHeight: Int,
    ): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, resId, bounds)
        val options =
            BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds, reqWidth, reqHeight)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
        return BitmapFactory.decodeResource(context.resources, resId, options)
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int,
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }
}
