package com.pokechain.ui.scanner

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.pokechain.data.iv.IvFormat
import com.pokechain.data.models.AppLanguage
import com.pokechain.data.models.Strings
import com.pokechain.data.scanner.ScanResult
import kotlin.math.roundToInt

/**
 * Chip flotante sobre Pokémon GO: muestra el % de perfección escaneado,
 * botón de copiar (formato superíndice) y botón de detener.
 * Tocar el cuerpo del chip abre la app con el detalle.
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
    private var lastResult: ScanResult? = null

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        y = 260
    }

    private val root = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(28, 16, 28, 16)
        background = GradientDrawable().apply {
            setColor(Color.argb(230, 25, 25, 30))
            cornerRadius = 28f
        }
    }

    private val messageView = TextView(context).apply {
        setTextColor(Color.WHITE)
        textSize = 14f
        setPadding(0, 0, 18, 0)
        // Tocar el cuerpo abre la app con el detalle
        setOnClickListener { onTap() }
    }

    private val copyView = TextView(context).apply {
        setTextColor(Color.argb(255, 130, 200, 255))
        textSize = 15f
        setPadding(18, 10, 18, 10)
        background = GradientDrawable().apply {
            setColor(Color.argb(255, 55, 70, 110))
            cornerRadius = 20f
        }
        visibility = android.view.View.GONE
        setOnClickListener { onCopy() }
    }

    private val stopView = TextView(context).apply {
        setTextColor(Color.argb(255, 255, 120, 120))
        textSize = 15f
        setPadding(14, 10, 4, 10)
        setOnClickListener { onStop() }
    }

    init {
        root.addView(messageView)
        root.addView(copyView)
        root.addView(stopView)
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

    fun update(result: ScanResult?) {
        lastResult = result
        root.post { render() }
    }

    private fun render() {
        when (com.pokechain.data.scanner.ScannerState.mode) {
            com.pokechain.data.scanner.ScannerMode.SHOWCASE -> {
                val total = lastResult?.showcaseTotal
                if (total != null) {
                    // Formato de exhibición: "987/1178" — sin i18n
                    messageView.text = "$total/1178"
                    messageView.textSize = 20f
                    copyView.visibility = android.view.View.VISIBLE
                    stopView.text = "✕"
                } else {
                    messageView.text = Strings.ivScanHintShowcase(language)
                    messageView.textSize = 14f
                    copyView.visibility = android.view.View.GONE
                    stopView.text = "✕"
                }
            }
            com.pokechain.data.scanner.ScannerMode.IV -> {
                val pct = lastResult?.perfection?.let { it.roundToInt() }
                if (pct != null) {
                    // Formato superíndice sin nombre (ej. "⁹³") — sin i18n
                    messageView.text = IvFormat.superscript(pct)
                    messageView.textSize = 24f
                    copyView.visibility = android.view.View.VISIBLE
                    stopView.text = "✕"
                } else {
                    messageView.text = Strings.ivScanHint(language)
                    messageView.textSize = 14f
                    copyView.visibility = android.view.View.GONE
                    stopView.text = "✕"
                }
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
}