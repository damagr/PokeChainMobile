package com.pokechain.data.dialgadex

import android.content.Context
import com.pokechain.data.models.PvEFilterParams
import com.pokechain.data.models.PvERankingEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import okhttp3.OkHttpClient
import okhttp3.Request
import org.mozilla.javascript.Context as RhinoContext
import org.mozilla.javascript.ScriptableObject
import java.io.File
import java.util.concurrent.TimeUnit

class PvEJSEngine(private val appContext: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val cacheTtlMs = 24 * 60 * 60 * 1000L
    private val json = Json { ignoreUnknownKeys = true }

    private val jsScript: String by lazy {
        appContext.assets.open("dialgadex/dialgadex_engine.js")
            .bufferedReader().readText()
    }

    private fun cacheFile(url: String) = File(appContext.cacheDir, "pve_${url.hashCode()}.json")

    private fun fetchWithCache(url: String): String {
        val file = cacheFile(url)
        val now = System.currentTimeMillis()
        if (file.exists() && now - file.lastModified() < cacheTtlMs) {
            return file.readText()
        }
        val data = fetchString(url)
        file.writeText(data)
        return data
    }

    suspend fun compute(filters: PvEFilterParams): List<PvERankingEntry> = withContext(Dispatchers.IO) {
        val pkmJson = fetchWithCache(
            "https://raw.githubusercontent.com/mgrann03/pokemon-resources/main/pogo_pkm.min.json"
        )
        val fmJson = fetchWithCache(
            "https://raw.githubusercontent.com/mgrann03/pokemon-resources/main/pogo_fm.json"
        )
        val cmJson = fetchWithCache(
            "https://raw.githubusercontent.com/mgrann03/pokemon-resources/main/pogo_cm.json"
        )

        val rhino = RhinoContext.enter().apply {
            optimizationLevel = -1
        }
        try {
            val scope = rhino.initStandardObjects()

            val maxCount = filters.count.coerceAtMost(500)
            val includeShadow = filters.includeShadow
            val includeMega = filters.mega
            val includeLegendary = filters.legendary
            val includeUnreleased = filters.unreleased
            val mixed = true

            rhino.evaluateString(scope, jsScript, "dialgadex_engine.js", 1, null)

            val call = "computeAll(${jsonEncode(pkmJson)}, ${jsonEncode(fmJson)}, ${jsonEncode(cmJson)}, " +
                    "$maxCount, $includeShadow, $includeMega, $includeLegendary, $includeUnreleased, $mixed)"

            val resultJson = rhino.evaluateString(scope, call, "call", 1, null)?.toString()
                ?: return@withContext emptyList()

            val list = json.decodeFromString<List<PvERankingEntryJson>>(resultJson)
            list.map { it.toEntry() }
        } finally {
            RhinoContext.exit()
        }
    }

    private fun jsonEncode(s: String): String {
        val escaped = s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }

    private fun fetchString(url: String): String {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        return response.body?.string() ?: throw Exception("Empty response from $url")
    }
}

@kotlinx.serialization.Serializable
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
    val fmIsElite: Boolean = false,
    val fmType: String? = null,
    val cm: String? = null,
    val cmIsElite: Boolean = false,
    val cmType: String? = null,
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
