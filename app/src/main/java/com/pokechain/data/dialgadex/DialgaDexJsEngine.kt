package com.pokechain.data.dialgadex

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.pokechain.data.models.PvEFilterParams
import com.pokechain.data.models.PvERankingEntry
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DialgaDexJsEngine(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    private data class RawPvEResult(
        val rat: Double,
        val dps: Double,
        val tdo: Double,
        val id: Int,
        val name: String,
        val form: String,
        val shadow: Boolean = false,
        val level: Int = 40,
        val fm: String? = null,
        @kotlinx.serialization.SerialName("fm_is_elite") val fmIsElite: Boolean = false,
        @kotlinx.serialization.SerialName("fm_type") val fmType: String? = null,
        val cm: String? = null,
        @kotlinx.serialization.SerialName("cm_is_elite") val cmIsElite: Boolean = false,
        @kotlinx.serialization.SerialName("cm_type") val cmType: String? = null,
        val tier: String? = null,
        val pct: Double? = null,
    )

    private class JsResultCallback(
        private val webView: WebView,
        private val json: Json,
        private val filters: PvEFilterParams,
        private val cont: CancellableContinuation<List<PvERankingEntry>>,
    ) {
        @JavascriptInterface
        fun onComplete(jsonData: String) {
            webView.destroy()
            if (!cont.isActive) return
            try {
                val raw = json.decodeFromString<List<RawPvEResult>>(jsonData)
                val filtered = raw
                    .filter { matchesFilters(it, filters) }
                    .map { it.toEntry() }
                cont.resume(filtered)
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }

        @JavascriptInterface
        fun onError(message: String) {
            webView.destroy()
            if (cont.isActive) {
                cont.resumeWithException(Exception("JS Error: $message"))
            }
        }

        private fun matchesFilters(raw: RawPvEResult, filters: PvEFilterParams): Boolean {
            if (!filters.includeShadow && raw.shadow) return false
            return true
        }

        private fun RawPvEResult.toEntry() = PvERankingEntry(
            rat = rat, dps = dps, tdo = tdo,
            id = id, name = name, form = form,
            shadow = shadow, level = level,
            fm = fm, fmIsElite = fmIsElite, fmType = fmType,
            cm = cm, cmIsElite = cmIsElite, cmType = cmType,
            tier = tier, pct = pct,
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun fetchPvERankings(filters: PvEFilterParams): List<PvERankingEntry> {
        val pkmJson = fetchString(
            "https://raw.githubusercontent.com/mgrann03/pokemon-resources/main/pogo_pkm.min.json"
        )
        val fmJson = fetchString(
            "https://raw.githubusercontent.com/mgrann03/pokemon-resources/main/pogo_fm.json"
        )
        val cmJson = fetchString(
            "https://raw.githubusercontent.com/mgrann03/pokemon-resources/main/pogo_cm.json"
        )

        val jsPreamble = readAsset("dialgadex/pokemon_enums.js") +
            readAsset("dialgadex/pokemon_utils.js") +
            readAsset("dialgadex/calc.js")

        val js = buildString {
            append(jsPreamble)
            appendLine("jb_pkm = $pkmJson;")
            appendLine("jb_fm = $fmJson;")
            appendLine("jb_cm = $cmJson;")
            appendLine("jb_max_id = Math.max(...jb_pkm.map(p => p.id));")
            appendLine("settings_metric = 'eDPS';")
            appendLine("settings_default_level = [40];")
            appendLine("settings_party_size = 1;")
            appendLine("settings_type_affinity = false;")
            appendLine("settings_relobbytime = 10;")
            appendLine("settings_team_size_normal = 6;")
            appendLine("settings_team_size_mega = 1;")
            appendLine("settings_xl_budget = false;")
            appendLine("settings_pve_turns = true;")
            appendLine("settings_newdps = true;")
            appendLine("settings_speculative = false;")
            appendLine("settings_compare = 'top';")
            appendLine("settings_tiermethod = 'absolute';")
            appendLine("settings_metric_exp = 0.5;")
            append(
                """
                var params = {
                    type:'Any',
                    shadow:${filters.includeShadow},
                    legendary:${filters.legendary},
                    mega:${filters.mega},
                    unreleased:${filters.unreleased},
                    elite:true, mixed:false, offtype:false,
                    versus:false, real_damage:false, suboptimal:false
                };
                window.onerror = function(msg, url, line) {
                    AndroidBridge.onError(msg + ' at ' + url + ':' + line);
                    return true;
                };
                (async function() {
                    try {
                        var results = await GetStrongestOfOneType(params);
                        var slice = results.slice(0, ${filters.count});
                        AndroidBridge.onComplete(JSON.stringify(slice));
                    } catch(e) {
                        AndroidBridge.onError(e.message + '\\n' + e.stack);
                    }
                })();
                """.trimIndent()
            )
        }

        return withTimeout(60_000L) {
            suspendCancellableCoroutine { cont ->
                Handler(Looper.getMainLooper()).post {
                    try {
                        val webView = WebView(context)
                        webView.settings.javaScriptEnabled = true
                        webView.settings.allowFileAccess = false
                        webView.settings.allowContentAccess = false

                        val callback = JsResultCallback(webView, json, filters, cont)
                        webView.addJavascriptInterface(callback, "AndroidBridge")

                        cont.invokeOnCancellation {
                            Handler(Looper.getMainLooper()).post {
                                webView.destroy()
                            }
                        }

                        webView.evaluateJavascript(js, null)
                    } catch (e: Exception) {
                        if (cont.isActive) cont.resumeWithException(e)
                    }
                }
            }
        }
    }

    private suspend fun fetchString(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        response.body?.string() ?: throw Exception("Empty response from $url")
    }

    private fun readAsset(path: String): String {
        return context.assets.open(path).bufferedReader().use { it.readText() }
    }
}
