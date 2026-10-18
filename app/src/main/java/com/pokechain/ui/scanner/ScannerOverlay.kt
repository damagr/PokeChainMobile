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
import android.widget.FrameLayout
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
 *  - ANCHO FIJO por modo (valor + Copiar + acciones caben siempre; sin
 *    redimensiones entre estados ni al arrastrar) y hint siempre reservado
 *  - Botones: Copiar (deshabilitado sin resultado), ⇄ (forma, si aplica),
 *    ▶/↺ (comparten hueco), "–" (colapsar), ✕ (detener)
 *  - Se oculta cuando PokeChain está en primer plano
 */
class ScannerOverlay(
    context: Context,
    private val language: AppLanguage,
    private val onCopy: () -> Unit,
    private val onStop: () -> Unit,
    private val onTap: () -> Unit,
    private val onReset: () -> Unit,
    private val onStartScan: () -> Unit,
    private val onCycleForm: () -> Unit = {},
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
        // Plegado: pastilla siempre tocable aunque el valor esté vacío
        minimumWidth = (48 * context.resources.displayMetrics.density).toInt()
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
        // Cifras tabulares: el número no "baila" al cambiar de dígitos
        fontFeatureSettings = "tnum"
        gravity = Gravity.CENTER_HORIZONTAL
        setOnTouchListener(dragOrTap { onTap() })
    }

    private val hintView = TextView(context).apply {
        setTextColor(Color.argb(255, 200, 200, 210))
        textSize = 11f
        setSingleLine(true)
        ellipsize = android.text.TextUtils.TruncateAt.END
        gravity = Gravity.CENTER_HORIZONTAL
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

    /**
     * Toggle de forma (⇄ n/m): genérico para cualquier variante (Alola, Galar,
     * Hisui, Mega, clases de tamaño…). Solo visible en modo SHOWCASE cuando la
     * especie confirmada tiene más de una forma. Toca → siguiente forma.
     */
    private val formView = TextView(context).apply {
        setTextColor(Color.WHITE)
        textSize = 12f
        setPadding(12, 6, 12, 6)
        background = GradientDrawable().apply {
            setColor(Color.argb(255, 80, 62, 105))
            cornerRadius = 18f
        }
        visibility = View.GONE
        setOnClickListener { onCycleForm() }
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

    // Botón de inicio (▶): mismo hueco que ↺ (slot reservado, sin saltos)
    private val startView = TextView(context).apply {
        setTextColor(Color.argb(255, 130, 200, 255))
        textSize = 15f
        setPadding(12, 4, 12, 4)
        background = GradientDrawable().apply {
            setColor(Color.argb(255, 55, 70, 110))
            cornerRadius = 18f
        }
        text = "▶"
        visibility = View.INVISIBLE
        setOnClickListener { onStartScan() }
    }

    /** Hueco compartido ▶/↺: ancho constante, uno visible cada vez. */
    private val actionSlot = FrameLayout(context).apply {
        addView(resetView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(startView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    init {
        // La columna de texto expande (ancho del chip fijado por modo en
        // applyChipWidth); los botones mantienen separación consistente.
        val density = context.resources.displayMetrics.density
        fun actionParams() = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { marginStart = (6 * density).toInt() }
        val textParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        root.addView(textColumn, textParams)
        root.addView(copyView, actionParams())
        root.addView(formView, actionParams())
        root.addView(actionSlot, actionParams())
        root.addView(collapseView, actionParams())
        root.addView(stopView, actionParams())
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
            applyChipWidth()
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

    /** Ancho fijo del chip según el modo (colapsado: contenido). Sin saltos de tamaño. */
    private fun applyChipWidth() {
        val density = root.context.resources.displayMetrics.density
        params.width = if (collapsed) {
            WindowManager.LayoutParams.WRAP_CONTENT
        } else {
            val widthDp = when (ScannerState.mode) {
                ScannerMode.IV -> IV_CHIP_WIDTH_DP        // "⁹⁹⁹" + Copiar + acciones
                ScannerMode.SHOWCASE -> SHOWCASE_CHIP_WIDTH_DP // "1056/1178" + ⇄ + Copiar + acciones
            }
            (widthDp * density).toInt()
        }
        if (shown) runCatching { wm.updateViewLayout(root, params) }
    }

    private fun render() {
        root.post {
            if (!shown || ScannerState.appInForeground.value) return@post
            val result = lastResult
            val stage = ScannerState.stage.value
            val reading = ScannerState.reading.value
            applyChipWidth()

            // ── Valor según etapa y modo ─────────────────────────
            val scanning = ScannerState.scanningEnabled.value
            when (stage) {
                ScanStage.LOOKING_POKEMON -> valueView.text = if (scanning) "…" else ""
                ScanStage.POKEMON_READ -> valueView.text = when (ScannerState.mode) {
                    ScannerMode.SHOWCASE -> result?.showcaseTotal?.let { "$it/1178" } ?: "…"
                    ScannerMode.IV -> if (reading) "…" else "✓"
                }
                ScanStage.IV_READ -> valueView.text = when (ScannerState.mode) {
                    ScannerMode.SHOWCASE -> result?.showcaseTotal?.let { "$it/1178" } ?: "…"
                    ScannerMode.IV -> result?.perfection?.let { IvFormat.superscript(it.roundToInt()) } ?: "…"
                }
            }

            // ── Hint de etapa (línea siempre reservada: altura estable) ──
            // needsCard manda: en Exhibiciones faltan peso/altura y hay que
            // volver a la ficha; también si el total quedó sin calcular.
            val stageHint = when (stage) {
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
            val needsCardHint = ScannerState.needsCard.value ||
                (stage == ScanStage.IV_READ && ScannerState.mode == ScannerMode.SHOWCASE &&
                 result?.showcaseTotal == null)
            val hint = if (needsCardHint) Strings.ivScanBackToCard(language) else stageHint
            hintView.text = hint.ifBlank { " " }
            hintView.visibility = if (collapsed) View.GONE else View.VISIBLE

            // ── Botones: huecos reservados (VISIBLE/INVISIBLE), sin redimensionar ──
            val ready = stage == ScanStage.IV_READ && lastResult != null
            val hasValue = valueView.text.isNotBlank() && valueView.text != "…" && valueView.text != "✓"
            val awaitingStart = !scanning && stage == ScanStage.LOOKING_POKEMON && lastResult == null
            val canCopy = hasValue || ready
            copyView.visibility = if (collapsed) View.GONE else View.VISIBLE
            copyView.isEnabled = canCopy
            copyView.alpha = if (canCopy) 1f else 0.45f
            resetView.visibility = if (!collapsed && scanning) View.VISIBLE else View.INVISIBLE
            startView.visibility = if (!collapsed && awaitingStart) View.VISIBLE else View.INVISIBLE
            collapseView.visibility = if (collapsed) View.GONE else View.VISIBLE
            stopView.visibility = if (collapsed) View.GONE else View.VISIBLE
            // Toggle de forma: reservado (INVISIBLE) en SHOWCASE; fuera en IV
            val forms = ScannerState.formKeys.value
            val showForm = ScannerState.mode == ScannerMode.SHOWCASE &&
                forms.size > 1 && stage != ScanStage.LOOKING_POKEMON
            formView.text = "⇄ ${ScannerState.formIndex.value + 1}/${forms.size}"
            formView.visibility = when {
                collapsed -> View.GONE
                ScannerState.mode != ScannerMode.SHOWCASE -> View.GONE
                showForm -> View.VISIBLE
                else -> View.INVISIBLE
            }
            if (collapsed) {
                // Punto pequeño: tap para expandir. Sin valor visible el chip
                // plegado muestra el glyph de expandir (nunca una pastilla muerta)
                if (valueView.text.isBlank()) valueView.text = "▸"
                valueView.setOnTouchListener(dragOrTap { setCollapsed(false) })
            } else {
                valueView.setOnTouchListener(dragOrTap { onTap() })
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

    private companion object {
        /** Ancho fijo (dp) del chip expandido, por modo (incluye márgenes de 6dp entre botones). */
        const val IV_CHIP_WIDTH_DP = 248f
        const val SHOWCASE_CHIP_WIDTH_DP = 296f
    }
}