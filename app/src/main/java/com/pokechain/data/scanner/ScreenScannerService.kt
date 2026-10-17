package com.pokechain.data.scanner

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.pokechain.data.models.Strings
import com.pokechain.ui.scanner.ScannerOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Servicio en primer plano que captura la pantalla vía MediaProjection,
 * analiza los frames (Pantalla del Pokémon / Valoración) y muestra el
 * resultado en un chip flotante con el % de perfección copiable.
 */
class ScreenScannerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var projection: MediaProjection? = null
    private var virtualDisplay: android.hardware.display.VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var handlerThread: HandlerThread? = null
    private var overlay: ScannerOverlay? = null
    private lateinit var analyzer: FrameAnalyzer
    private lateinit var showcaseProvider: com.pokechain.data.showcase.ShowcaseDataProvider
    private var lastProcessedMs = 0L
    private var screenW = 0
    private var screenH = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (ScannerState.isRunning.value) return START_NOT_STICKY
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Int.MIN_VALUE) ?: Int.MIN_VALUE
        @Suppress("DEPRECATION")
        val data: Intent? = if (Build.VERSION.SDK_INT >= 33)
            intent?.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        else intent?.getParcelableExtra(EXTRA_DATA)
        if (resultCode == Int.MIN_VALUE || data == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        analyzer = FrameAnalyzer(applicationContext)
        showcaseProvider = com.pokechain.data.showcase.ShowcaseDataProvider(applicationContext)

        // Notificación + FGS (Android 14+: startForeground ANTES de getMediaProjection)
        startForegroundInternal()

        val pm = getSystemService(MediaProjectionManager::class.java)
        try {
            projection = pm.getMediaProjection(resultCode, data)
        } catch (e: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }
        projection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() = stopSelf()
        }, Handler(Looper.getMainLooper()))

        overlay = ScannerOverlay(
            this,
            ScannerState.language,
            onCopy = ::copyResult,
            onStop = { stopSelf() },
            onTap = ::openApp,
            onReset = { ScannerState.resetFlow() },
        )
        overlay?.show()

        setupCapture()
        ScannerState.resetFlow()
        ScannerState.isRunning.value = true

        // El chip solo se muestra sobre Pokémon GO (oculto dentro de PokeChain)
        scope.launch {
            ScannerState.appInForeground.collect { fg -> overlay?.setVisible(!fg) }
        }
        return START_NOT_STICKY
    }

    // ── Máquina de estados del flujo de escaneo ────────────────────

    private var candidateIdentity: Triple<Int?, Any?, Any?>? = null
    private var candidateIvs: Triple<Int, Int, Int>? = null
    private var confirmCount = 0

    private suspend fun processFrame(bmp: Bitmap, mask: android.graphics.Rect?) {
        when (ScannerState.stage.value) {
            ScanStage.LOOKING_POKEMON -> processLooking(bmp, mask)
            ScanStage.POKEMON_READ -> processPokemonRead(bmp, mask)
            ScanStage.IV_READ -> processIvRead(bmp, mask)
        }
    }

    /** Etapa 1: buscar y confirmar la página del Pokémon (nombre/peso/altura/PC). */
    private suspend fun processLooking(bmp: Bitmap, mask: android.graphics.Rect?) {
        val frame = analyzer.analyzeLooking(bmp, mask)

        // Modo IV: solo hace falta el nombre (pantalla principal). Modo SHOWCASE: nombre + altura + peso.
        val reading = when (ScannerState.mode) {
            ScannerMode.IV -> frame.speciesName != null
            ScannerMode.SHOWCASE -> frame.speciesName != null && frame.weightKg != null && frame.heightM != null
        }
        ScannerState.reading.value = reading
        if (!reading) {
            candidateIdentity = null
            confirmCount = 0
            overlay?.update(ScannerState.lastResult.value)
            return
        }

        // Confirmación: 2 frames consecutivos con la misma identidad
        val identity = when (ScannerState.mode) {
            ScannerMode.IV -> Triple(frame.dex, frame.speciesName, frame.cp)
            ScannerMode.SHOWCASE -> Triple(frame.dex, frame.weightKg, frame.heightM)
        }
        if (identity == candidateIdentity) confirmCount++ else { candidateIdentity = identity; confirmCount = 1 }

        // Publicar los datos leídos (el chip muestra el progreso)
        val merged = (ScannerState.lastResult.value ?: ScanResult()).mergeWith(frame)
        ScannerState.lastResult.value = merged
        overlay?.update(merged)

        if (confirmCount >= 2) {
            ScannerState.stage.value = ScanStage.POKEMON_READ
            ScannerState.reading.value = false
            confirmCount = 0
            candidateIdentity = null
        }
    }

    /** Etapa 2: esperar y confirmar las barras de la Valoración (IVs). */
    private suspend fun processPokemonRead(bmp: Bitmap, mask: android.graphics.Rect?) {
        val ivs = analyzer.readBars(bmp, mask)
        ScannerState.reading.value = ivs != null
        if (ivs == null) {
            // La Valoración no está abierta (aún): mantener los datos del Pokémon
            overlay?.update(ScannerState.lastResult.value)
            return
        }

        // Confirmación: 2 frames consecutivos con los mismos IVs
        if (ivs == candidateIvs) confirmCount++ else { candidateIvs = ivs; confirmCount = 1 }

        if (confirmCount >= 2) {
            val frame = ScanResult(ivAtk = ivs.first, ivDef = ivs.second, ivSta = ivs.third)
            val merged = (ScannerState.lastResult.value ?: ScanResult()).mergeWith(frame)
            val published = if (ScannerState.mode == ScannerMode.SHOWCASE)
                merged.copy(showcaseTotal = showcaseTotal(merged)) else merged
            ScannerState.lastResult.value = published
            overlay?.update(published)
            ScannerState.stage.value = ScanStage.IV_READ
            ScannerState.reading.value = false
            confirmCount = 0
            candidateIvs = null
        } else {
            overlay?.update(ScannerState.lastResult.value)
        }
    }

    /** Etapa 3 (COMPLETO): solo vigilar si la Valoración sigue en pantalla. */
    private suspend fun processIvRead(bmp: Bitmap, mask: android.graphics.Rect?) {
        val present = analyzer.barsPresence(bmp, mask)
        if (!present) {
            // El usuario navegó a otro Pokémon: reset automático del flujo
            ScannerState.resetFlow()
            ScannerState.reading.value = false
            overlay?.update(null)
        }
        // Sin OCR: la pantalla estable no cuesta nada
    }

    private fun startForegroundInternal() {
        val channelId = "iv_scanner"
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(channelId, "IV Scanner", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("PokeChain")
            .setContentText(Strings.ivScanHint(ScannerState.language))
            .setOngoing(true)
            .build()
        val type = if (Build.VERSION.SDK_INT >= 29)
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION else 0
        startForeground(NOTIF_ID, notification, type)
    }

    private fun setupCapture() {
        val wm = getSystemService(WindowManager::class.java)
        val bounds = if (Build.VERSION.SDK_INT >= 30)
            wm.currentWindowMetrics.bounds
        else android.graphics.Rect(
            0, 0, resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels
        )
        val w = bounds.width()
        val h = bounds.height()
        screenW = w
        screenH = h
        val density: Int = resources.displayMetrics.densityDpi

        handlerThread = HandlerThread("ScannerFrames").apply { start() }
        val frameHandler = Handler(handlerThread!!.looper)

        imageReader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2).apply {
            setOnImageAvailableListener({ reader ->
                val now = SystemClock.elapsedRealtime()
                if (now - lastProcessedMs < FRAME_INTERVAL_MS) {
                    reader.acquireLatestImage()?.close()
                    return@setOnImageAvailableListener
                }
                lastProcessedMs = now
                val image: Image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                val bmp = try { image.toBitmapDownscaled(MAX_SCAN_WIDTH) } catch (_: Exception) { null } finally { image.close() }
                if (bmp == null) return@setOnImageAvailableListener
                scope.launch {
                    try {
                        val mask = overlay?.boundsOnScreen()?.let { r ->
                            // Escala uniforme para X e Y: el bitmap se redibuja a MAX_SCAN_WIDTH
                            // manteniendo proporción, así que ambas ejes usan el mismo factor.
                            val scale = MAX_SCAN_WIDTH.toFloat() / screenW
                            android.graphics.Rect(
                                (r.left * scale).toInt(), (r.top * scale).toInt(),
                                (r.right * scale).toInt(), (r.bottom * scale).toInt()
                            )
                        }
                        processFrame(bmp, mask)
                        bmp.recycle()
                    } catch (_: Exception) {
                        bmp.recycle()
                    }
                }
            }, frameHandler)
        }

        virtualDisplay = projection?.createVirtualDisplay(
            "PokeChainScanner", w, h, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface, null, frameHandler
        )
    }

    /** Puntuación de Exhibición (XXXX/1178): especie + peso + altura (+IVs si leídos). */
    private fun showcaseTotal(result: ScanResult): Int? {
        val dex = result.dex ?: return null
        val height = result.heightM ?: return null
        val weight = result.weightKg ?: return null
        // Usar getSizeByMeasurements para especies no uniformes (Pumpkaboo/Gourgeist)
        // con varios tamaños; elige la forma que mejor coincida con lo medido.
        val entry = showcaseProvider.getSizeByMeasurements(dex, height, weight) ?: return null
        val calc = com.pokechain.data.showcase.ShowcaseCalculator.calculate(
            size = entry.size,
            inputHeight = height,
            inputWeight = weight,
            atkIv = result.ivAtk,
            defIv = result.ivDef,
            hpIv = result.ivSta
        )
        return calc.total
    }

    private fun copyResult() {
        val result = ScannerState.lastResult.value
        val text = when (ScannerState.mode) {
            ScannerMode.SHOWCASE -> result?.showcaseTotal?.let { "$it/1178" }
            ScannerMode.IV -> result?.perfection?.let { it.roundToInt() }?.let {
                com.pokechain.data.iv.IvFormat.superscript(it)
            }
        } ?: return
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("pokechain", text))
        overlay?.flash(Strings.copied(ScannerState.language))
    }

    private fun openApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun onDestroy() {
        try { virtualDisplay?.release() } catch (_: Exception) {}
        try { imageReader?.close() } catch (_: Exception) {}
        try { projection?.stop() } catch (_: Exception) {}
        overlay?.hide()
        handlerThread?.quitSafely()
        ScannerState.isRunning.value = false
        scope.cancel()
        super.onDestroy()
    }

    // ── Conversión Image -> Bitmap (con rowStride + downscale) ─────

    private fun Image.toBitmapDownscaled(maxWidth: Int): Bitmap {
        val plane = planes[0]
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val rowPadding = rowStride - pixelStride * width

        buffer.rewind()
        val paddedWidth = width + rowPadding / pixelStride
        val full = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
        full.copyPixelsFromBuffer(buffer)

        val cropped = if (paddedWidth != width) {
            Bitmap.createBitmap(full, 0, 0, width, height).also { full.recycle() }
        } else full

        val scale = minOf(1f, MAX_SCAN_WIDTH.toFloat() / cropped.width)
        return if (scale < 1f) {
            val scaled = Bitmap.createScaledBitmap(
                cropped, MAX_SCAN_WIDTH, (cropped.height * scale).toInt(), true
            )
            cropped.recycle()
            scaled
        } else cropped
    }

    companion object {
        private const val EXTRA_RESULT_CODE = "resultCode"
        private const val EXTRA_DATA = "data"
        private const val NOTIF_ID = 42001
        private const val FRAME_INTERVAL_MS = 400L // ~2,5 fps
        private const val MAX_SCAN_WIDTH = 720

        fun start(context: Context, resultCode: Int, data: Intent) {
            val intent = Intent(context, ScreenScannerService::class.java).apply {
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_DATA, data)
            }
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }
    }
}