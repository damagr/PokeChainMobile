package com.pokechain.data.dialgadex

import com.pokechain.data.models.PvEFilterParams
import com.pokechain.data.models.PvERankingEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

@Serializable
private data class RawPkm(
    val id: Int,
    val name: String,
    val form: String = "Normal",
    val types: List<String>,
    val stats: RawStats,
    val shadow: Boolean = false,
    val released: Boolean = false,
    @kotlinx.serialization.SerialName("raid_tier") val raidTier: Int? = null,
    val fm: List<String>? = null,
    val cm: List<String>? = null,
    @kotlinx.serialization.SerialName("elite_fm") val eliteFm: List<String>? = null,
    @kotlinx.serialization.SerialName("elite_cm") val eliteCm: List<String>? = null,
    @kotlinx.serialization.SerialName("fm_add") val fmAdd: List<String>? = null,
    @kotlinx.serialization.SerialName("fm_rem") val fmRem: List<String>? = null,
    @kotlinx.serialization.SerialName("cm_add") val cmAdd: List<String>? = null,
    @kotlinx.serialization.SerialName("cm_rem") val cmRem: List<String>? = null,
    @kotlinx.serialization.SerialName("class") val pkmClass: String? = null,
)

@Serializable
private data class RawStats(
    @kotlinx.serialization.SerialName("baseStamina") val baseStamina: Int,
    @kotlinx.serialization.SerialName("baseAttack") val baseAttack: Int,
    @kotlinx.serialization.SerialName("baseDefense") val baseDefense: Int,
)

@Serializable
private data class RawFm(
    val id: Int,
    val name: String,
    val type: String,
    val power: Double = 0.0,
    val duration: Int = 500,
    @kotlinx.serialization.SerialName("energy_delta") val energyDelta: Int = 0,
)

@Serializable
private data class RawCm(
    val id: Int,
    val name: String,
    val type: String,
    val power: Double = 0.0,
    val duration: Int = 3000,
    @kotlinx.serialization.SerialName("energy_delta") val energyDelta: Int = -33,
)

class PvECalculator {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun compute(filters: PvEFilterParams): List<PvERankingEntry> = withContext(Dispatchers.IO) {
        val pkmList = json.decodeFromString<List<RawPkm>>(fetchString(
            "https://raw.githubusercontent.com/mgrann03/pokemon-resources/main/pogo_pkm.min.json"
        ))
        val fmMap = json.decodeFromString<List<RawFm>>(fetchString(
            "https://raw.githubusercontent.com/mgrann03/pokemon-resources/main/pogo_fm.json"
        )).associateBy { it.name }

        val cmMap = json.decodeFromString<List<RawCm>>(fetchString(
            "https://raw.githubusercontent.com/mgrann03/pokemon-resources/main/pogo_cm.json"
        )).associateBy { it.name }

        val cpm40 = 0.7317
        val results = mutableListOf<PvERankingEntry>()

        for (pkm in pkmList) {
            if (!pkm.released && !filters.unreleased) continue
            if (pkm.pkmClass != null && !filters.legendary) continue
            val form = pkm.form
            if ((form == "Mega" || form == "MegaY" || form == "MegaZ") && !filters.mega) continue
            if (form != "Normal" && form != "Alola" && form != "Galarian" && form != "Hisuian"
                && pkm.pkmClass == null && form != "Origin" && form != "Altered"
            ) continue

            val isShadow = pkm.shadow
            if (isShadow && !filters.includeShadow) continue

            var atk = (pkm.stats.baseAttack + 15) * cpm40
            if (isShadow) atk *= 1.2

            val fms = pkm.fm ?: continue
            val cms = pkm.cm ?: continue
            if (fms.isEmpty() || cms.isEmpty()) continue

            var bestScore = 0.0
            var bestFm: String? = null
            var bestCm: String? = null

            for (fmName in fms) {
                val fm = fmMap[fmName] ?: continue
                val fmPower = maxOf(1.0, fm.power)
                val fmDur = maxOf(0.5, fm.duration / 1000.0)
                var fmDps = fmPower / fmDur
                if (pkm.types.contains(fm.type)) fmDps *= 1.2

                for (cmName in cms) {
                    val cm = cmMap[cmName] ?: continue
                    val cmPower = maxOf(1.0, cm.power)
                    val cmDur = maxOf(0.5, cm.duration / 1000.0)
                    var cmDps = cmPower / cmDur
                    if (pkm.types.contains(cm.type)) cmDps *= 1.2

                    val score = atk * sqrt(fmDps * cmDps)
                    if (score > bestScore) {
                        bestScore = score
                        bestFm = fmName
                        bestCm = cmName
                    }
                }
            }

            if (bestFm != null) {
                results.add(PvERankingEntry(
                    rat = bestScore, dps = bestScore / atk, tdo = bestScore * 50,
                    id = pkm.id, name = pkm.name, form = pkm.form,
                    shadow = isShadow, level = 40,
                    fm = bestFm, fmType = fmMap[bestFm]?.type,
                    cm = bestCm, cmType = cmMap[bestCm]?.type,
                ))
            }
        }

        results.sortByDescending { it.rat }
        results.take(filters.count)
    }

    private fun fetchString(url: String): String {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        return response.body?.string() ?: throw Exception("Empty response from $url")
    }
}
