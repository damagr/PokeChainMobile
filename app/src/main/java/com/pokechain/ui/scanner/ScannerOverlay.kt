package com.pokechain.ui.scanner

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.pokechain.data.iv.IvFormat
import com.pokechain.data.models.AppLanguage
import com.pokechain.data.models.Strings
import com.pokechain.data.scanner.ScanResult
import com.pokechain.data.scanner.ScanStage
import com.pokechain.data.scanner.ScannerMode
import com.pokechain.data.scanner.ScannerState
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Chip flotante sobre Pokémon GO (modelo Calcy IV):
 *  - Arrastrable con el dedo (posición persiste en la sesión)
 *  - Colapsable a un punto pequeño (botón "–")
 *  - Muestra el progreso del flujo por etapas y el resultado por modo:
 *      IV:        "⁹³" al completar
 *      SHOWCASE:  "XXXX/1178" (sin IVs) -> "XXXX/1178" (con IVs)
 *  - Botones: Copiar, ↺ (reiniciar flujo), "–" (colapsar), ✕ (detener)
 *  - Se oculta cuando PokeChain está en primer plano
 */
class ScannerOverlay(
    context: Context,
    private val language: AppLanguage,
    private val onCopy: () -> Unit,
    private val onStop: () -> Unit,
    private val onTap: () -> Unit,
    private val onReset: () -> Unit,
) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var shown = false
    private var collapsed = false
    private var lastResult: ScanResult? = null

    private var dragStartX = 0f
    private var dragStartY = 0f
    private var dragStartPX = 0
    private var dragStartPY = 0
    private var dragging = false

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = ScannerState.chipX
        y = ScannerState.chipY
    }

    private val root = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(24, 12, 24, 12)
        // Ancho mínimo para que el número no se comprima/corte al aparecer
        setMinimumWidth((140f * context.resources.displayMetrics.density).toInt())
        background = GradientDrawable().apply {
            setColor(Color.argb(205, 20, 20, 26))
            cornerRadius = 30f
        }
    }

    // Columna de texto: valor (arriba) + hint de etapa (abajo)
    private val valueView = TextView(context).apply {
        setTextColor(Color.WHITE)
        textSize = 20f
        setPadding(0, 0, 14, 0)
        setSingleLine(true)
        setEllipsize(null)
        setOnTouchListener(dragOrTap { onTap() })
    }

    private val hintView = TextView(context).apply {
        setTextColor(Color.argb(255, 200, 200, 210))
        textSize = 11f
    }

    private val textColumn = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_VERTICAL
        addView(valueView)
        addView(hintView)
    }

    private val copyView = TextView(context).apply {
        setTextColor(Color.argb(255, 130, 200, 255))
        textSize = 14f
        setPadding(16, 8, 16, 8)
        background = GradientDrawable().apply {
            setColor(Color.argb(255, 55, 70, 110))
            cornerRadius = 18f
        }
        text = Strings.ivScanCopy(language)
        visibility = View.GONE
        setOnClickListener { onCopy() }
    }

    private val resetView = TextView(context).apply {
        setTextColor(Color.WHITE)
        textSize = 14f
        setPadding(12, 4, 4, 4)
        setOnClickListener { onReset() }
    }

    private val collapseView = TextView(context).apply {
        setTextColor(Color.WHITE)
        textSize = 16f
        setPadding(12, 4, 8, 4)
        setOnClickListener { setCollapsed(true) }
    }

    private val stopView = TextView(context).apply {
        setTextColor(Color.argb(255, 255, 120, 120))
        textSize = 15f
        setPadding(12, 4, 4, 4)
        setOnClickListener { onStop() }
    }

    init {
        // La columna de texto expande para que el número no se corte visualmente;
        // los botones (Copiar, ↺, –, ✕) mantienen su tamaño natural.
        val textParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        root.addView(textColumn, textParams)
        root.addView(copyView)
        root.addView(resetView)
        root.addView(collapseView)
        root.addView(stopView)
    }

    /** Arrastrar con desambiguación tap (movimiento < 24px = tap). */
    private fun dragOrTap(onTap: () -> Unit): (View, MotionEvent) -> Boolean = { v: View, ev: MotionEvent ->
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragStartX = ev.rawX; dragStartY = ev.rawY
                dragStartPX = params.x; dragStartPY = params.y
                dragging = false
                true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.rawX - dragStartX
                val dy = ev.rawY - dragStartY
                if (dragging || abs(dx) > 24 || abs(dy) > 24) {
                    dragging = true
                    params.x = dragStartPX + dx.toInt()
                    params.y = dragStartPY + dy.toInt()
                    ScannerState.chipX = params.x
                    ScannerState.chipY = params.y
                    runCatching { wm.updateViewLayout(root, params) }
                    true
                } else false
            }
            MotionEvent.ACTION_UP -> {
                val wasDragging = dragging
                dragging = false
                if (!wasDragging) onTap()
                true
            }
            else -> false
        }
    }

    private fun setCollapsed(c: Boolean) {
        collapsed = c
        render()
    }

    fun show() {
        if (!shown) {
            wm.addView(root, params)
            shown = true
        }
        render()
    }

    fun hide() {
        if (shown) {
            wm.removeView(root)
            shown = false
        }
    }

    /** Visible solo cuando PoGo está en primer plano (oculto dentro de PokeChain). */
    fun setVisible(visible: Boolean) {
        if (visible && ScannerState.isRunning.value && !shown) show()
        if (!visible && shown) hide()
    }

    fun update(result: ScanResult?) {
        lastResult = result
        root.post { render() }
    }

    private fun render() {
        root.post {
            if (!shown || ScannerState.appInForeground.value) return@post
            val result = lastResult
            val stage = ScannerState.stage.value
            val reading = ScannerState.reading.value

            // ── Valor según etapa y modo ─────────────────────────
            when (stage) {
                ScanStage.LOOKING_POKEMON -> valueView.text = "…"
                ScanStage.POKEMON_READ -> valueView.text = when (ScannerState.mode) {
                    ScannerMode.SHOWCASE -> result?.showcaseTotal?.let { "$it/1178" } ?: "…"
                    ScannerMode.IV -> if (reading) "…" else "✓"
                }
                ScanStage.IV_READ -> valueView.text = when (ScannerState.mode) {
                    ScannerMode.SHOWCASE -> result?.showcaseTotal?.let { "$it/1178" } ?: "…"
                    ScannerMode.IV -> result?.perfection?.let { IvFormat.superscript(it.roundToInt()) } ?: "…"
                }
            }

            // ── Hint de etapa ────────────────────────────────────
            hintView.text = when (stage) {
                ScanStage.LOOKING_POKEMON -> when (ScannerState.mode) {
                    ScannerMode.SHOWCASE -> Strings.ivScanHintShowcase(language)
                    ScannerMode.IV -> Strings.ivScanHint(language)
                }
                ScanStage.POKEMON_READ -> when (ScannerState.mode) {
                    ScannerMode.SHOWCASE -> Strings.ivScanOpenAppraisal(language)
                    ScannerMode.IV -> if (reading) Strings.ivScanReading(language)
                                      else Strings.ivScanOpenAppraisal(language)
                }
                ScanStage.IV_READ -> when (ScannerState.mode) {
                    ScannerMode.SHOWCASE -> if (reading) Strings.ivScanReading(language) else ""
                    ScannerMode.IV -> ""
                }
            }

            // ── Visibilidad de botones ───────────────────────────
            val ready = stage == ScanStage.IV_READ && lastResult != null
            val hasValue = valueView.text.isNotBlank() && valueView.text != "…" && valueView.text != "✓"
            copyView.visibility = if ((hasValue || ready) && !collapsed) View.VISIBLE else View.GONE
            resetView.visibility = if (!collapsed) View.VISIBLE else View.GONE
            collapseView.visibility = if (!collapsed) View.VISIBLE else View.GONE
            stopView.visibility = if (!collapsed) View.VISIBLE else View.GONE
            if (collapsed) {
                hintView.visibility = View.GONE
                // Punto pequeño: tap para expandir
                valueView.setOnTouchListener(dragOrTap { setCollapsed(false) })
            } else {
                hintView.visibility = if (hintView.text.isBlank()) View.GONE else View.VISIBLE
            }
        }
    }

    /** Bounds actuales del chip en pantalla (para enmascarar en el análisis). */
    fun boundsOnScreen(): Rect? {
        if (!shown) return null
        val loc = IntArray(2)
        root.getLocationOnScreen(loc)
        return Rect(loc[0], loc[1], loc[0] + root.width, loc[1] + root.height)
    }

    /** Mensaje transitorio (ej. "¡Copiado!") que vuelve al estado anterior. */
    fun flash(message: String) {
        root.post {
            hintView.text = message
            root.postDelayed({ render() }, 1200L)
        }
    }
}