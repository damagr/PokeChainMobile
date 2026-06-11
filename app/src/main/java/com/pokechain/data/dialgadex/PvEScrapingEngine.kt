package com.pokechain.data.dialgadex

import android.content.Context
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import com.pokechain.data.models.PvEFilterParams
import com.pokechain.data.models.PvERankingEntry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class PvEScrapingEngine(private val appContext: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun compute(filters: PvEFilterParams): List<PvERankingEntry> = withContext(Dispatchers.Main) {
        val deferred = CompletableDeferred<String>()
        val webView = WebView(appContext)

        try {
            webView.apply {
                layoutParams = ViewGroup.LayoutParams(1, 1)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT

                addJavascriptInterface(JsBridge(deferred), "Android")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        val count = filters.count.coerceAtMost(500)

                        view.evaluateJavascript("""
                            (function() {
                                var checkReady = setInterval(function() {
                                    if (typeof GetStrongestOfOneType === 'function' && 
                                        typeof pkm_data !== 'undefined' && pkm_data.length > 0) {
                                        clearInterval(checkReady);
                                        try {
                                            settings_type_affinity = true;
                                            settings_team_size_normal = 6;
                                            settings_team_size_mega = 6;
                                            settings_relobbytime = 10;
                                            settings_metric = "eDPS";
                                            settings_pve_turns = true;
                                            settings_newdps = true;

                                            var params = {
                                                type: "Any",
                                                elite: true,
                                                mixed: true,
                                                offtype: true,
                                                suboptimal: false,
                                                level: 40,
                                                real_damage: false,
                                                shadow: ${filters.includeShadow},
                                                mega: ${filters.mega},
                                                legendary: ${filters.legendary},
                                                unreleased: ${filters.unreleased}
                                            };
                                            var results = GetStrongestOfOneType(params);
                                            var sliced = results.slice(0, $count);
                                            Android.onResult('SUCCESS:' + JSON.stringify(sliced));
                                        } catch(e) {
                                            Android.onResult('ERROR:' + e.message);
                                        }
                                    }
                                }, 200);
                                setTimeout(function() {
                                    clearInterval(checkReady);
                                    Android.onResult('TIMEOUT');
                                }, 120000);
                            })();
                        """.trimIndent(), null)
                    }
                }

                loadUrl("https://dialgadex.com/?strongest&t=Any")
            }

            val raw = withTimeout(120_000) { deferred.await() }

            when {
                raw == "TIMEOUT" || raw.startsWith("ERROR:") -> emptyList()
                raw.startsWith("SUCCESS:") -> parseResults(raw.removePrefix("SUCCESS:"))
                else -> parseResults(raw)
            }
        } catch (e: Exception) {
            throw e
        } finally {
            webView.destroy()
        }
    }

    private fun parseResults(jsonStr: String): List<PvERankingEntry> {
        if (jsonStr.isBlank()) return emptyList()
        val list = json.decodeFromString<List<PvERankingEntryJson>>(jsonStr)
        return list.map { it.toEntry() }
    }

    private class JsBridge(private val deferred: CompletableDeferred<String>) {
        @JavascriptInterface
        fun onResult(result: String) {
            if (!deferred.isCompleted) {
                deferred.complete(result)
            }
        }
    }

    @Serializable
    private data class PvERankingEntryJson(
        val rat: Double,
        val dps: Double,
        val tdo: Double,
        val id: Int,
        val name: String,
        val form: String,
        val shadow: Boolean = false,
        val level: Int = 40,
        val fm: String? = null,
        @SerialName("fm_is_elite") val fmIsElite: Boolean = false,
        @SerialName("fm_type") val fmType: String? = null,
        val cm: String? = null,
        @SerialName("cm_is_elite") val cmIsElite: Boolean = false,
        @SerialName("cm_type") val cmType: String? = null,
        val tier: String? = null,
        val pct: Double? = null,
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
