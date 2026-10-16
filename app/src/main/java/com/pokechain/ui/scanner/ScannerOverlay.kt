package com.pokechain.ui.scanner

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.pokechain.data.iv.IvFormat
import com.pokechain.data.models.AppLanguage
import com.pokechain.data.models.Strings
import com.pokechain.data.scanner.ScanResult
import com.pokechain.data.scanner.ScannerMode
import com.pokechain.data.scanner.ScannerState
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Chip flotante sobre Pokémon GO (modelo Calcy IV):
 *  - Arrastrable con el dedo (la posición persiste en la sesión)
 *  - Colapsable a un punto pequeño (tap en "–") para no tapar nada
 *  - Muestra según ScannerState.mode: "⁹³" (IV) o "987/1178" (Exhibición)
 *  - Translúcido y en la parte superior por defecto (no tapa teclado ni
 *    peso/altura de PoGo)
 *
 * El chip se oculta cuando PokeChain está en primer plano (setVisible).
 */
class ScannerOverlay(
    context: Context,
    private val language: AppLanguage,
    private val onCopy: () -> Unit,
    private val onStop: () -> Unit,
    private val onTap: () -> Unit,
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
        background = GradientDrawable().apply {
            setColor(Color.argb(205, 20, 20, 26))
            cornerRadius = 30f
        }
    }

    private val messageView = TextView(context).apply {
        setTextColor(Color.WHITE)
        textSize = 14f
        setPadding(0, 0, 14, 0)
        // Arrastrar desde el cuerpo; tap corto = abrir la app
        setOnTouchListener(dragOrTap { onTap() })
    }

    private val copyView = TextView(context).apply {
        setTextColor(Color.argb(255, 130, 200, 255))
        textSize = 14f
        setPadding(16, 8, 16, 8)
        background = GradientDrawable().apply {
            setColor(Color.argb(255, 55, 70, 110))
            cornerRadius = 18f
        }
        visibility = View.GONE
        setOnClickListener { onCopy() }
    }

    private val collapseView = TextView(context).apply {
        setTextColor(Color.WHITE)
        textSize = 16f
        setPadding(16, 4, 8, 4)
        setOnClickListener { setCollapsed(true) }
    }

    private val stopView = TextView(context).apply {
        setTextColor(Color.argb(255, 255, 120, 120))
        textSize = 15f
        setPadding(12, 4, 4, 4)
        setOnClickListener { onStop() }
    }

    init {
        root.addView(messageView)
        root.addView(copyView)
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
            val showChip = shown && !ScannerState.appInForeground.value
            if (!showChip) return@post
            val pct = lastResult?.perfection?.let { it.roundToInt() }
            val total = lastResult?.showcaseTotal
            when (ScannerState.mode) {
                ScannerMode.SHOWCASE -> {
                    if (total != null) {
                        messageView.text = "$total/1178"
                        messageView.textSize = 20f
                    } else {
                        messageView.text = Strings.ivScanHintShowcase(language)
                        messageView.textSize = 13f
                    }
                }
                ScannerMode.IV -> {
                    if (pct != null) {
                        messageView.text = IvFormat.superscript(pct)
                        messageView.textSize = 22f
                    } else {
                        messageView.text = Strings.ivScanHint(language)
                        messageView.textSize = 13f
                    }
                }
            }
            copyView.visibility = if ((pct != null || total != null) && !collapsed) View.VISIBLE else View.GONE
            collapseView.text = if (collapsed) "□" else "–"
            collapseView.visibility = if (collapsed) View.GONE else View.VISIBLE
            stopView.text = if (collapsed) "□" else "✕"
            if (collapsed) {
                stopView.visibility = View.GONE
                copyView.visibility = View.GONE
                // punto pequeño: tap para expandir
                messageView.setOnTouchListener(dragOrTap { setCollapsed(false) })
            } else {
                stopView.visibility = View.VISIBLE
                messageView.setOnTouchListener(dragOrTap { onTap() })
            }
        }
    }

    /** Mensaje transitorio (ej. "¡Copiado!") que vuelve al estado anterior. */
    fun flash(message: String) {
        root.post {
            messageView.text = message
            root.postDelayed({ render() }, 1200L)
        }
    }

    /** Bounds actuales del chip en pantalla (para enmascarar en el análisis). */
    fun boundsOnScreen(): Rect? {
        if (!shown) return null
        val loc = IntArray(2)
        root.getLocationOnScreen(loc)
        return Rect(loc[0], loc[1], loc[0] + root.width, loc[1] + root.height)
    }
}