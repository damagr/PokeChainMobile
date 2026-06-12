package com.pokechain.data.dialgadex

import android.content.Context
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import com.pokechain.data.models.PvERankingEntry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class PvEScrapingEngine(private val appContext: Context) {

    private var webView: WebView? = null
    private var bridge: EngineBridge? = null
    private val json = Json { ignoreUnknownKeys = true }

    private var lastCount: Int? = null
    private var lastResults: List<PvERankingEntry> = emptyList()

    private class EngineBridge {
        @Volatile var pendingDeferred: CompletableDeferred<String>? = null

        @JavascriptInterface
        fun onResult(result: String) {
            pendingDeferred?.complete(result)
        }
    }

    suspend fun init() = withContext(Dispatchers.Main) {
        if (webView != null) return@withContext
        val b = EngineBridge()
        val deferred = CompletableDeferred<Boolean>()
        bridge = b

        val wv = WebView(appContext).apply {
            layoutParams = ViewGroup.LayoutParams(1, 1)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK
            addJavascriptInterface(b, "Android")
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    if (deferred.isCompleted) return
                    pollReady(view, deferred)
                }
            }
            loadUrl("https://dialgadex.com/?strongest&t=Any")
        }
        webView = wv
        withTimeout(30_000) { deferred.await() }
    }

    private fun pollReady(view: WebView, deferred: CompletableDeferred<Boolean>) {
        view.evaluateJavascript("""
            (function() {
                return typeof GetStrongestOfOneType === 'function' &&
                    typeof jb_pkm !== 'undefined' && jb_pkm.length > 0;
            })();
        """.trimIndent()) { result ->
            if (result == "true") {
                deferred.complete(true)
            } else {
                view.postDelayed({ pollReady(view, deferred) }, 300)
            }
        }
    }

    fun getCachedResults(count: Int): List<PvERankingEntry> {
        return if (count == lastCount) lastResults else emptyList()
    }

    suspend fun compute(count: Int): List<PvERankingEntry> = withContext(Dispatchers.Main) {
        if (count == lastCount && lastResults.isNotEmpty()) {
            return@withContext lastResults
        }
        init()
        val view = webView ?: return@withContext emptyList()
        val b = bridge ?: return@withContext emptyList()

        val deferred = CompletableDeferred<String>()
        b.pendingDeferred = deferred

        val n = count.coerceAtMost(300)

        view.evaluateJavascript("""
            (async function() {
                try {
                    settings_type_affinity = true;
                    settings_team_size_normal = 6;
                    settings_team_size_mega = 6;
                    settings_relobbytime = 10;
                    settings_metric = "eDPS";
                    settings_pve_turns = true;
                    settings_newdps = true;

                    var params = {
                        type: "Any", elite: true, mixed: true, offtype: true,
                        suboptimal: false, level: 40, real_damage: false,
                        shadow: false,
                        mega: true,
                        legendary: true,
                        unreleased: true
                    };
                    var results = await GetStrongestOfOneType(params);
                    var sliced = results.slice(0, $n);
                    Android.onResult('SUCCESS:' + JSON.stringify(sliced));
                } catch(e) {
                    Android.onResult('ERROR:' + (e.message || e));
                }
            })();
        """.trimIndent(), null)

        val raw = withTimeout(120_000) { deferred.await() }

        val parsed = when {
            raw == "TIMEOUT" || raw.startsWith("ERROR:") -> emptyList()
            raw.startsWith("SUCCESS:") -> parseResults(raw.removePrefix("SUCCESS:"))
            else -> parseResults(raw)
        }

        lastCount = count
        lastResults = parsed
        parsed
    }

    fun destroy() {
        webView?.destroy()
        webView = null
    }

    private fun parseResults(jsonStr: String): List<PvERankingEntry> {
        if (jsonStr.isBlank()) return emptyList()
        val list = json.decodeFromString<List<PvERankingEntryJson>>(jsonStr)
        return list.map { it.toEntry() }
    }

    @Serializable
    private data class PvERankingEntryJson(
        val rat: Double, val dps: Double, val tdo: Double,
        val id: Int, val name: String, val form: String,
        val shadow: Boolean = false, val level: Int = 40,
        val fm: String? = null,
        @SerialName("fm_is_elite") val fmIsElite: Boolean = false,
        @SerialName("fm_type") val fmType: String? = null,
        val cm: String? = null,
        @SerialName("cm_is_elite") val cmIsElite: Boolean = false,
        @SerialName("cm_type") val cmType: String? = null,
        val tier: String? = null, val pct: Double? = null,
    ) {
        fun toEntry() = PvERankingEntry(
            rat = rat, dps = dps, tdo = tdo,
            id = id, name = name, form = form,
            shadow = shadow, level = level,
            fm = fm, fmIsElite = fmIsElite, fmType = fmType,
            cm = cm, cmIsElite = cmIsElite, cmType = cmType,
            tier = tier, pct = pct,
        )
    }
}
