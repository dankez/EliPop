package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val TAG = "FirebaseAiWallpaperRepo"

enum class AiStylePreset(
    val displayName: String,
    val subtitle: String,
    val promptHints: String,
) {
    KAWAII(
        "Kawaii Anime",
        "Jemné pastelové chibi ilustrácie",
        "cute kawaii anime chibi illustration, soft pastel colors, dreamy sparkling eyes, gentle shading, vertical mobile phone wallpaper",
    ),
    COQUETTE(
        "Coquette Mašličky",
        "Ružové stuhy, perly a kvety",
        "aesthetic coquette style, silk pink ribbons, delicate pearls, blush roses, soft creamy background, feminine mobile wallpaper",
    ),
    BLACK_GLITTER(
        "Black Girly",
        "Tmavé trblietavé motýle pre AMOLED",
        "dark aesthetic black girly wallpaper, glowing silver glitter butterflies, obsidian glossy backdrop, subtle rose sparkles, AMOLED friendly vertical phone wallpaper",
    ),
    PASTEL_DREAM(
        "Pastelový sen",
        "Cukrové oblaky a hviezdičky",
        "ethereal pastel cotton candy sky, lilac and blush pink clouds, twinkling tiny stars, crescent moon glow, vertical mobile wallpaper",
    ),
}

data class AiGenerationResult(
    val bitmap: Bitmap,
    val title: String,
)

interface AiWallpaperRepository {
    suspend fun generateWallpaper(
        prompt: String,
        style: AiStylePreset,
    ): Result<AiGenerationResult>

    suspend fun stylizeUserPhoto(
        sourceBitmap: Bitmap,
        style: AiStylePreset,
    ): Result<AiGenerationResult>
}

class FirebaseAiWallpaperRepository : AiWallpaperRepository {

    private val imageModel by lazy {
        val config =
            generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
                imageConfig =
                    ImageConfig.builder()
                        .setAspectRatio(AspectRatio.PORTRAIT_9x16)
                        .setImageSize(ImageSize.SIZE_1K)
                        .build()
            }
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = config,
        )
    }

    override suspend fun generateWallpaper(
        prompt: String,
        style: AiStylePreset,
    ): Result<AiGenerationResult> =
        withContext(Dispatchers.IO) {
            try {
                val userIdea = prompt.trim().ifEmpty { style.subtitle }
                val fullPrompt =
                    "Create a high-quality vertical 9:16 phone lockscreen wallpaper with NO text, NO clock, and NO watermarks. " +
                        "Subject: $userIdea. Style: ${style.promptHints}."

                val response = withTimeout(60_000L) { imageModel.generateContent(fullPrompt) }
                val bitmap =
                    response.candidates
                        .firstOrNull()
                        ?.content
                        ?.parts
                        ?.filterIsInstance<ImagePart>()
                        ?.firstOrNull()
                        ?.image
                        ?: return@withContext Result.failure(
                            IllegalStateException("AI model nevrátil žiadny obrázok. Skús upraviť popis."),
                        )

                Result.success(
                    AiGenerationResult(
                        bitmap = bitmap,
                        title =
                            prompt.trim().take(28).ifBlank { "AI ${style.displayName}" },
                    ),
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI wallpaper generation timed out", e)
                Result.failure(
                    IllegalStateException("Generovanie tapety vypršalo po 60 sekundách. Skús to znova."),
                )
            } catch (e: QuotaExceededException) {
                Log.e(TAG, "AI quota exceeded", e)
                Result.failure(
                    IllegalStateException("Limit AI požiadaviek bol prekročený. Skús to neskôr."),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI wallpaper generation failed", e)
                Result.failure(
                    IllegalStateException(
                        e.localizedMessage ?: "Nepodarilo sa vygenerovať AI tapetu.",
                    ),
                )
            }
        }

    override suspend fun stylizeUserPhoto(
        sourceBitmap: Bitmap,
        style: AiStylePreset,
    ): Result<AiGenerationResult> =
        withContext(Dispatchers.IO) {
            try {
                val scaledInput = scaleDownForPayload(sourceBitmap, maxDimension = 1024)
                val instruction =
                    "Transform this photo into a ${style.promptHints} mobile phone wallpaper. " +
                        "Preserve the main subject and composition, make it soft, charming, and vertical 9:16 with no text or watermarks."

                val response =
                    withTimeout(60_000L) {
                        imageModel.generateContent(
                            content {
                                image(scaledInput)
                                text(instruction)
                            },
                        )
                    }

                val bitmap =
                    response.candidates
                        .firstOrNull()
                        ?.content
                        ?.parts
                        ?.filterIsInstance<ImagePart>()
                        ?.firstOrNull()
                        ?.image
                        ?: return@withContext Result.failure(
                            IllegalStateException("AI model nevrátil upravený obrázok."),
                        )

                Result.success(
                    AiGenerationResult(
                        bitmap = bitmap,
                        title = "AI ${style.displayName}",
                    ),
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI photo stylization timed out", e)
                Result.failure(
                    IllegalStateException("Premena fotky vypršala po 60 sekundách. Skús to znova."),
                )
            } catch (e: QuotaExceededException) {
                Log.e(TAG, "AI quota exceeded", e)
                Result.failure(
                    IllegalStateException("Limit AI požiadaviek bol prekročený. Skús to neskôr."),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI photo stylization failed", e)
                Result.failure(
                    IllegalStateException(
                        e.localizedMessage ?: "Nepodarilo sa premeniť fotku pomocou AI.",
                    ),
                )
            }
        }

    private fun scaleDownForPayload(source: Bitmap, maxDimension: Int): Bitmap {
        val maxSide = maxOf(source.width, source.height)
        if (maxSide <= maxDimension) return source
        val ratio = maxDimension.toFloat() / maxSide.toFloat()
        val w = (source.width * ratio).toInt().coerceAtLeast(1)
        val h = (source.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, w, h, true)
    }
}
