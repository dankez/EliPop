package com.example.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.example.PastelkaApp
import com.example.R
import com.example.data.model.LiveEffectType
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PastelkaLiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = PastelkaEngine()

    private inner class PastelkaEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private var prefsJob: Job? = null

        private var width = 1080
        private var height = 1920
        private var visible = false

        @Volatile private var backgroundBitmap: Bitmap? = null
        @Volatile private var currentEffect: LiveEffectType = LiveEffectType.PETALS

        private val particles = List(26) { Particle.random() }

        private val drawRunnable =
            object : Runnable {
                override fun run() {
                    drawFrame()
                    if (visible) {
                        handler.postDelayed(this, 33L)
                    }
                }
            }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            val app = applicationContext as? PastelkaApp ?: return
            prefsJob =
                engineScope.launch {
                    app.wallpaperRepository.ensureSeeded()
                    app.preferencesRepository.preferencesFlow.collectLatest { prefs ->
                        currentEffect = prefs.liveEffectType
                        val item =
                            prefs.currentWallpaperId?.let { app.wallpaperRepository.getById(it) }
                                ?: app.wallpaperRepository.pickNextRotationCandidate(prefs)
                        val loaded =
                            if (item != null) {
                                app.wallpaperApplier.loadBitmapForItem(item)
                            } else {
                                BitmapFactory.decodeResource(resources, R.drawable.img_wall_pets)
                            }
                        if (loaded != null) {
                            backgroundBitmap = loaded
                        }
                    }
                }
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder?,
            format: Int,
            width: Int,
            height: Int,
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            this.width = width.coerceAtLeast(1)
            this.height = height.coerceAtLeast(1)
            drawFrame()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            handler.removeCallbacks(drawRunnable)
            if (visible) {
                handler.post(drawRunnable)
            }
        }

        override fun onDestroy() {
            visible = false
            handler.removeCallbacks(drawRunnable)
            prefsJob?.cancel()
            engineScope.cancel()
            backgroundBitmap = null
            super.onDestroy()
        }

        private fun drawFrame() {
            val holder = surfaceHolder ?: return
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    render(canvas)
                }
            } catch (_: Exception) {
            } finally {
                if (canvas != null) {
                    runCatching { holder.unlockCanvasAndPost(canvas) }
                }
            }
        }

        private fun render(canvas: Canvas) {
            val bmp = backgroundBitmap
            if (bmp != null && !bmp.isRecycled) {
                val scale = maxOf(width.toFloat() / bmp.width, height.toFloat() / bmp.height)
                val scaledW = bmp.width * scale
                val scaledH = bmp.height * scale
                val left = (width - scaledW) / 2f
                val top = (height - scaledH) / 2f
                val destRect = RectF(left, top, left + scaledW, top + scaledH)
                canvas.drawBitmap(bmp, null, destRect, null)
            } else {
                canvas.drawColor(Color.rgb(252, 228, 236))
            }

            val effect = currentEffect
            if (effect == LiveEffectType.NONE) return

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            particles.forEach { p ->
                p.step(effect)
                val px = p.x * width
                val py = p.y * height
                val size = p.size * (width / 1080f)

                when (effect) {
                    LiveEffectType.PETALS -> {
                        paint.color = Color.argb(p.alpha, 255, 194, 218)
                        canvas.save()
                        canvas.translate(px, py)
                        canvas.rotate(p.angle)
                        canvas.drawOval(-size, -size * 0.55f, size, size * 0.55f, paint)
                        canvas.restore()
                    }

                    LiveEffectType.SPARKLES -> {
                        val pulse = (0.45f + 0.55f * sin(p.phase)).coerceIn(0.2f, 1f)
                        paint.color = Color.argb((p.alpha * pulse).toInt(), 255, 250, 240)
                        canvas.save()
                        canvas.translate(px, py)
                        canvas.drawLine(-size, 0f, size, 0f, paint.apply { strokeWidth = 2.5f })
                        canvas.drawLine(0f, -size, 0f, size, paint)
                        canvas.drawCircle(0f, 0f, size * 0.25f, paint)
                        canvas.restore()
                    }

                    LiveEffectType.HEARTS -> {
                        paint.color = Color.argb(p.alpha, 255, 138, 178)
                        drawHeart(canvas, px, py, size * 1.15f, paint)
                    }

                    LiveEffectType.NONE -> Unit
                }
            }
        }

        private fun drawHeart(
            canvas: Canvas,
            cx: Float,
            cy: Float,
            size: Float,
            paint: Paint,
        ) {
            val path =
                Path().apply {
                    moveTo(cx, cy + size * 0.45f)
                    cubicTo(
                        cx - size * 1.1f,
                        cy - size * 0.15f,
                        cx - size * 0.55f,
                        cy - size * 0.95f,
                        cx,
                        cy - size * 0.35f,
                    )
                    cubicTo(
                        cx + size * 0.55f,
                        cy - size * 0.95f,
                        cx + size * 1.1f,
                        cy - size * 0.15f,
                        cx,
                        cy + size * 0.45f,
                    )
                    close()
                }
            canvas.drawPath(path, paint)
        }
    }

    private data class Particle(
        var x: Float,
        var y: Float,
        var speedY: Float,
        var drift: Float,
        var size: Float,
        var alpha: Int,
        var angle: Float,
        var phase: Float,
    ) {
        fun step(effect: LiveEffectType) {
            phase += 0.08f
            angle += 1.2f
            when (effect) {
                LiveEffectType.PETALS -> {
                    y += speedY
                    x += sin(phase) * drift
                    if (y > 1.06f) {
                        y = -0.06f
                        x = Random.nextFloat()
                    }
                }

                LiveEffectType.HEARTS -> {
                    y -= speedY * 0.85f
                    x += cos(phase) * drift * 0.7f
                    if (y < -0.06f) {
                        y = 1.06f
                        x = Random.nextFloat()
                    }
                }

                LiveEffectType.SPARKLES -> {
                    y += speedY * 0.25f
                    if (y > 1.04f) {
                        y = -0.04f
                        x = Random.nextFloat()
                    }
                }

                LiveEffectType.NONE -> Unit
            }
        }

        companion object {
            fun random(): Particle =
                Particle(
                    x = Random.nextFloat(),
                    y = Random.nextFloat(),
                    speedY = 0.0022f + Random.nextFloat() * 0.0035f,
                    drift = 0.0012f + Random.nextFloat() * 0.002f,
                    size = 10f + Random.nextFloat() * 14f,
                    alpha = 140 + Random.nextInt(100),
                    angle = Random.nextFloat() * 360f,
                    phase = Random.nextFloat() * 6.28f,
                )
        }
    }
}
