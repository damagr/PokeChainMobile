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
import kotlin.math.*

private data class PvEMove(
    val name: String,
    val type: String,
    val power: Double,
    val duration: Int,
    val energyDelta: Int,
    val damageWindowStart: Int = 0,
)

private data class EnemyY(val yNum: Double? = null, val cmNum: Double? = null)

private data class PvEMoveLists(
    val fm: List<String>,
    val cm: List<String>,
    val eliteFm: List<String>,
    val eliteCm: List<String>,
    val pureOnlyCm: List<String>,
    val shadowOnlyCm: List<String>,
)

class PvECalculator {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    private val estimatedYNumerator = 1340.0
    private val estimatedCmPower = 11670.0
    private val defaultLevel = 40
    private val partySize = 1
    private val relobbyTime = 10.0
    private val teamSizeNormal = 6
    private val teamSizeMega = 1
    private val pveTurns = true
    private val newDps = true
    private val enemyDef = 180.0

    private val typeEffectChart = mapOf(
        "Normal" to Triple(listOf("Ghost"), listOf("Rock", "Steel"), emptyList()),
        "Fire" to Triple(emptyList(), listOf("Dragon", "Fire", "Rock", "Water"), listOf("Bug", "Grass", "Ice", "Steel")),
        "Water" to Triple(emptyList(), listOf("Dragon", "Grass", "Water"), listOf("Fire", "Ground", "Rock")),
        "Grass" to Triple(emptyList(), listOf("Bug", "Dragon", "Fire", "Flying", "Grass", "Poison", "Steel"), listOf("Ground", "Rock", "Water")),
        "Electric" to Triple(listOf("Ground"), listOf("Dragon", "Electric", "Grass"), listOf("Flying", "Water")),
        "Ice" to Triple(emptyList(), listOf("Fire", "Ice", "Steel", "Water"), listOf("Dragon", "Flying", "Grass", "Ground")),
        "Fighting" to Triple(listOf("Ghost"), listOf("Bug", "Fairy", "Flying", "Poison", "Psychic"), listOf("Dark", "Ice", "Normal", "Rock", "Steel")),
        "Poison" to Triple(listOf("Steel"), listOf("Ghost", "Ground", "Poison", "Rock"), listOf("Fairy", "Grass")),
        "Ground" to Triple(listOf("Flying"), listOf("Bug", "Grass"), listOf("Electric", "Fire", "Poison", "Rock", "Steel")),
        "Flying" to Triple(emptyList(), listOf("Electric", "Rock", "Steel"), listOf("Bug", "Fighting", "Grass")),
        "Psychic" to Triple(listOf("Dark"), listOf("Psychic", "Steel"), listOf("Fighting", "Poison")),
        "Bug" to Triple(emptyList(), listOf("Fairy", "Fighting", "Fire", "Flying", "Ghost", "Poison", "Steel"), listOf("Dark", "Grass", "Psychic")),
        "Rock" to Triple(emptyList(), listOf("Fighting", "Ground", "Steel"), listOf("Bug", "Fire", "Flying", "Ice")),
        "Ghost" to Triple(listOf("Normal"), listOf("Dark"), listOf("Ghost", "Psychic")),
        "Dragon" to Triple(listOf("Fairy"), listOf("Steel"), listOf("Dragon")),
        "Dark" to Triple(emptyList(), listOf("Dark", "Fairy", "Fighting"), listOf("Ghost", "Psychic")),
        "Steel" to Triple(emptyList(), listOf("Electric", "Fire", "Steel", "Water"), listOf("Fairy", "Ice", "Rock")),
        "Fairy" to Triple(emptyList(), listOf("Fire", "Poison", "Steel"), listOf("Dark", "Dragon", "Fighting")),
    )

    private val cpm = doubleArrayOf(
        0.0, 0.094, 0.16639787, 0.21573247, 0.25572005, 0.29024988,
        0.3210876, 0.34921268, 0.3752356, 0.39956728, 0.4225,
        0.44310755, 0.4627984, 0.48168495, 0.49985844, 0.51739395,
        0.5343543, 0.5507927, 0.5667545, 0.5822789, 0.5974,
        0.6121573, 0.6265671, 0.64065295, 0.65443563, 0.667934,
        0.6811649, 0.69414365, 0.7068842, 0.7193991, 0.7317,
        0.7377695, 0.74378943, 0.74976104, 0.7556855, 0.76156384,
        0.76739717, 0.7731865, 0.77893275, 0.784637, 0.7903,
        0.7953, 0.8003, 0.8053, 0.8103, 0.8153, 0.8203, 0.8253,
        0.8303, 0.8353, 0.8403, 0.8453, 0.8503, 0.8553, 0.8603, 0.8653,
    )

    private fun getCpmForLevel(level: Double): Double {
        val intLevel = level.toInt()
        if (level == intLevel.toDouble() && intLevel in 1..cpm.lastIndex) return cpm[intLevel]
        val prev = getCpmForLevel(floor(level))
        val next = getCpmForLevel(ceil(level))
        return sqrt((prev * prev + next * next) / 2.0)
    }

    private fun processDuration(duration: Int): Double {
        if (pveTurns) return round(duration / 1000.0 * 2.0) / 2.0
        return duration / 1000.0
    }

    private fun processPower(move: PvEMove): Double {
        if (pveTurns) {
            val newDur = processDuration(move.duration)
            val modifier = (newDur - move.duration / 1000.0) / newDur
            if (abs(modifier) >= 0.199) return move.power * (1.0 + modifier)
        }
        return move.power
    }

    private fun calcDamage(atk: Double, def: Double, power: Double, modifiers: Double, rounded: Boolean = false): Double {
        if (rounded) return floor(0.5 * power * (atk / def) * modifiers) + 1.0
        return 0.5 * power * (atk / def) * modifiers + 0.5
    }

    private fun getEffectivenessMultAgainst(attackType: String, enemyTypes: List<String>): Double {
        val effect = typeEffectChart[attackType] ?: return 1.0
        var mult = 1.0
        for (t in enemyTypes) {
            when {
                effect.first.contains(t) -> mult *= 0.390625
                effect.second.contains(t) -> mult *= 0.625
                effect.third.contains(t) -> mult *= 1.60
            }
        }
        return mult
    }

    private fun getTypesEffectivenessAgainstTypes(types: List<String>): Map<String, Double> {
        val result = mutableMapOf<String, Double>()
        for (attackType in typeEffectChart.keys) {
            result[attackType] = getEffectivenessMultAgainst(attackType, types)
        }
        return result
    }

    private fun getTypesEffectivenessSingleBoost(type: String): Map<String, Double> {
        val result = mutableMapOf<String, Double>()
        for (attackerType in typeEffectChart.keys) {
            result[attackerType] = if (attackerType == type) 1.60 else 1.0
        }
        return result
    }

    private fun getEffectivenessMultOfType(effectiveness: Map<String, Double>, type: String): Double {
        return effectiveness[type] ?: 1.0
    }

    private fun getHiddenPowerTypes(filter: String, types: List<String>): List<String> {
        return when (filter) {
            "None" -> emptyList()
            "Raid Boss" -> listOf("Fighting")
            "Type-Match" -> types.filter { it != "Fairy" && it != "Normal" }
            else -> typeEffectChart.keys.filter { it != "Fairy" && it != "Normal" }
        }
    }

    private fun getPokemonMoves(pkm: RawPkm, shadow: Boolean): PvEMoveLists? {
        if (pkm.fm == null || pkm.cm == null) return null
        var fm = pkm.fm.toMutableList()
        val eliteFm = (pkm.eliteFm?.toMutableList() ?: mutableListOf())
        var cm = pkm.cm.toMutableList()
        val eliteCm = (pkm.eliteCm?.toMutableList() ?: mutableListOf())

        if (fm.contains("Hidden Power") || eliteFm.contains("Hidden Power")) {
            val hpTypes = getHiddenPowerTypes("All", pkm.types)
            for (t in hpTypes) {
                if (fm.contains("Hidden Power")) fm.add("Hidden Power $t")
                if (eliteFm.contains("Hidden Power")) eliteFm.add("Hidden Power $t")
            }
        }

        val shadowOnlyCm = mutableListOf<String>()
        val pureOnlyCm = mutableListOf<String>()
        if (pkm.shadow) {
            pureOnlyCm.add("Return")
        }

        if (pkm.fmAdd != null) for (f in pkm.fmAdd) eliteFm.add(f)
        if (pkm.cmAdd != null) for (c in pkm.cmAdd) eliteCm.add(c)
        if (pkm.fmRem != null) {
            fm.removeAll(pkm.fmRem.toSet())
            eliteFm.removeAll(pkm.fmRem.toSet())
        }
        if (pkm.cmRem != null) {
            cm.removeAll(pkm.cmRem.toSet())
            eliteCm.removeAll(pkm.cmRem.toSet())
        }

        return PvEMoveLists(fm, cm, eliteFm, eliteCm, pureOnlyCm, shadowOnlyCm)
    }

    private fun getPokemonStats(stats: RawStats): Triple<Double, Double, Double> {
        val cp = getCpmForLevel(defaultLevel.toDouble())
        val atk = (stats.baseAttack + 15) * cp
        val def = (stats.baseDefense + 15) * cp
        val hp = (stats.baseStamina + 15) * cp
        return Triple(atk, def, floor(hp))
    }

    private fun getPartyBoost(fToCRatio: Double): Double {
        if (partySize == 1) return 0.0
        val fMovesPerBoost = when (partySize) { 2 -> 18; 3 -> 9; 4 -> 6; else -> 0 }
        return max(0.0, min(fToCRatio / fMovesPerBoost, 1.0))
    }

    private fun getDPS(
        types: List<String>, atk: Double, def: Double, hp: Double,
        fmObj: PvEMove, cmObj: PvEMove,
        fmMult: Double, cmMult: Double,
        enemyDef: Double, enemyY: EnemyY? = null,
    ): Double {
        val y = (enemyY?.yNum ?: estimatedYNumerator) / def
        val inCmDmg = (enemyY?.cmNum ?: estimatedCmPower) / def

        val tof = hp / y
        val x = 0.5 * -cmObj.energyDelta + 0.5 * fmObj.energyDelta +
                (if (newDps) 0.5 * inCmDmg else 0.0)

        val fmDmgMult = fmMult * (if (types.contains(fmObj.type) && fmObj.name != "Hidden Power") 1.2 else 1.0)
        val fmDmg = calcDamage(atk, enemyDef, processPower(fmObj), fmDmgMult)
        val fmDps = fmDmg / processDuration(fmObj.duration)
        val fmEps = fmObj.energyDelta / processDuration(fmObj.duration)

        val fToCRatio = (tof * -cmObj.energyDelta + processDuration(cmObj.duration) * (x - 0.5 * hp)) /
                (tof * fmObj.energyDelta - processDuration(fmObj.duration) * (x - 0.5 * hp))
        val ppBoost = getPartyBoost(fToCRatio)

        val cmDmgMult = cmMult * (if (types.contains(cmObj.type)) 1.2 else 1.0)
        val cmDmg = calcDamage(atk, enemyDef, processPower(cmObj), cmDmgMult)
        val cmDps = cmDmg / processDuration(cmObj.duration)
        val cmDpsAdj = cmDps * (1.0 + ppBoost)
        var cmEps = -cmObj.energyDelta / processDuration(cmObj.duration)

        if (cmObj.energyDelta == -100) {
            val dws = if (pveTurns) 0.0 else cmObj.damageWindowStart / 1000.0
            cmEps = (-cmObj.energyDelta + 0.5 * fmObj.energyDelta + 0.5 * y * dws) / processDuration(cmObj.duration)
        }

        if (fmDps > cmDps) return fmDps

        val dps0 = (fmDps * cmEps + cmDpsAdj * fmEps) / (cmEps + fmEps)
        val dps = dps0 + ((cmDpsAdj - fmDps) / (cmEps + fmEps)) * (0.5 - x / hp) * y

        return if (fmDps > dps) fmDps else if (dps > 0) dps else 0.0
    }

    private fun getTDO(dps: Double, hp: Double, def: Double, enemyY: EnemyY? = null): Double {
        val y = (enemyY?.yNum ?: estimatedYNumerator) / def
        val tof = hp / y
        return dps * tof
    }

    private fun getEDPS(dps: Double, tdo: Double, mega: Boolean): Double {
        val respawnTime = 1.0
        val rejoinTime = relobbyTime
        val raidPartySize = if (mega) teamSizeMega else teamSizeNormal
        val bossHp = 1_000_000_000.0

        val tof = tdo / dps
        val lives = bossHp / tdo
        val deaths = lives - 0.5
        val relobbies = deaths / raidPartySize - 0.5
        val ttw = lives * tof + (deaths - relobbies) * respawnTime + rejoinTime * relobbies
        return bossHp / ttw
    }

    private fun avgYAgainst(enemyY: Map<String, EnemyY>, effectiveness: Map<String, Double>): EnemyY {
        var yNum = 0.0
        var cmNum = 0.0
        for ((t, ey) in enemyY) {
            if (t == "Any") continue
            val mult = effectiveness[t]
            if (mult != null && !mult.isNaN()) {
                yNum += (ey.yNum ?: 0.0) * mult
                cmNum += (ey.cmNum ?: 0.0) * mult
            }
        }
        return EnemyY(yNum, cmNum)
    }

    private data class MovesetRating(
        val rat: Double, val dps: Double, val tdo: Double,
        val fm: String, val cm: String,
    )

    private fun findBestMoveset(
        pkm: RawPkm, shadow: Boolean,
        fmMap: Map<String, PvEMove>, cmMap: Map<String, PvEMove>,
        enemyEffectiveness: Map<String, Double>, enemyYs: List<Map<String, EnemyY>>,
    ): MovesetRating? {
        val types = pkm.types
        val effectiveness = getTypesEffectivenessAgainstTypes(types)
        val (atkRaw, defRaw, hp) = getPokemonStats(pkm.stats)
        val atk = if (shadow) atkRaw * 1.2 else atkRaw
        val def = if (shadow) defRaw * 0.8333333 else defRaw

        val moves = getPokemonMoves(pkm, shadow) ?: return null
        val eliteCmExtended = moves.eliteCm.toMutableList()
        if (shadow) eliteCmExtended.addAll(moves.shadowOnlyCm)
        else eliteCmExtended.addAll(moves.pureOnlyCm)
        val allFms = moves.fm + moves.eliteFm
        val allCms = moves.cm + eliteCmExtended
        val mega = pkm.form == "Mega" || pkm.form == "MegaY" || pkm.form == "MegaZ"

        var bestRat = 0.0
        var bestDps = 0.0
        var bestTdo = 0.0
        var bestFm = ""
        var bestCm = ""

        for (fmName in allFms) {
            val fm = fmMap[fmName] ?: continue
            val fmMult = getEffectivenessMultOfType(enemyEffectiveness, fm.type)

            for (cmName in allCms) {
                val cm = cmMap[cmName] ?: continue
                if (fm.type != cm.type) continue

                val cmMult = getEffectivenessMultOfType(enemyEffectiveness, cm.type)

                var sumRat = 0.0
                var sumDps = 0.0
                var sumTdo = 0.0

                for (enemyY in enemyYs) {
                    val y = avgYAgainst(enemyY, effectiveness)
                    val dps = getDPS(types, atk, def, hp, fm, cm, fmMult, cmMult, this.enemyDef, y)
                    val tdo = getTDO(dps, hp, def, y)
                    val rat = getEDPS(dps, tdo, mega)
                    sumRat += rat
                    sumDps += dps
                    sumTdo += tdo
                }

                if (enemyYs.isNotEmpty()) {
                    sumRat /= enemyYs.size
                    sumDps /= enemyYs.size
                    sumTdo /= enemyYs.size
                }

                if (sumRat > bestRat) {
                    bestRat = sumRat
                    bestDps = sumDps
                    bestTdo = sumTdo
                    bestFm = fmName
                    bestCm = cmName
                }
            }
        }

        if (bestFm.isEmpty()) return null
        return MovesetRating(bestRat, bestDps, bestTdo, bestFm, bestCm)
    }

    private fun isCostumeForm(form: String): Boolean {
        val costumeIndicators = listOf(
            "_2019", "_2020", "_2021", "_2022", "_2023", "_2024", "_2025", "_2026",
            "Copy_", "Fall_", "Costume_", "Adventure_hat", "Flying_", "Summer_",
            "Winter_", "Spring_", "Gofest_", "Gotour_", "Tshirt_", "Holiday_", "Swim_",
        )
        if (costumeIndicators.any { form.contains(it) }) return true
        if (form.all { it.isDigit() }) return true
        return false
    }

    suspend fun compute(filters: PvEFilterParams): List<PvERankingEntry> = withContext(Dispatchers.IO) {
        val pkmList = json.decodeFromString<List<RawPkm>>(fetchString(
            "https://raw.githubusercontent.com/mgrann03/pokemon-resources/main/pogo_pkm.min.json"
        ))
        val fmMap = json.decodeFromString<List<RawFm>>(fetchString(
            "https://raw.githubusercontent.com/mgrann03/pokemon-resources/main/pogo_fm.json"
        )).associate { it.name to PvEMove(it.name, it.type, it.power, it.duration, it.energyDelta, it.damageWindowStart) }

        val cmMap = json.decodeFromString<List<RawCm>>(fetchString(
            "https://raw.githubusercontent.com/mgrann03/pokemon-resources/main/pogo_cm.json"
        )).associate { it.name to PvEMove(it.name, it.type, it.power, it.duration, it.energyDelta, it.damageWindowStart) }

        val enemyEffectiveness = getTypesEffectivenessSingleBoost("Any")
        val enemyYs = listOf(mapOf("Any" to EnemyY()))

        val results = mutableListOf<PvERankingEntry>()
        val seenKeys = mutableSetOf<String>()

        for (pkm in pkmList) {
            if (!pkm.released && !filters.unreleased) continue
            if (isCostumeForm(pkm.form)) continue
            if (pkm.pkmClass != null && !filters.legendary) continue
            if ((pkm.form.startsWith("Mega") || pkm.form == "MegaY" || pkm.form == "MegaZ") && !filters.mega) continue

            val mega = pkm.form == "Mega" || pkm.form == "MegaY" || pkm.form == "MegaZ"

            fun processEntry(shadow: Boolean) {
                val key = "${pkm.id}-${pkm.form}-${shadow}"
                if (key in seenKeys) return
                val rating = findBestMoveset(pkm, shadow, fmMap, cmMap, enemyEffectiveness, enemyYs) ?: return
                seenKeys.add(key)
                results.add(PvERankingEntry(
                    rat = rating.rat, dps = rating.dps, tdo = rating.tdo,
                    id = pkm.id, name = pkm.name, form = pkm.form,
                    shadow = shadow, level = defaultLevel,
                    fm = rating.fm, fmType = fmMap[rating.fm]?.type,
                    cm = rating.cm, cmType = cmMap[rating.cm]?.type,
                    tier = if (mega) "Mega" else null,
                ))
            }

            processEntry(false)
            if (filters.includeShadow && pkm.shadow) processEntry(true)
        }

        results.sortByDescending { it.rat }
        results.take(filters.count)
    }

    private fun fetchString(url: String): String {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        return response.body?.string() ?: throw Exception("Empty response from $url")
    }

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
        val id: Int, val name: String, val type: String, val power: Double = 0.0,
        val duration: Int = 500,
        @kotlinx.serialization.SerialName("energy_delta") val energyDelta: Int = 0,
        @kotlinx.serialization.SerialName("damage_window_start") val damageWindowStart: Int = 0,
    )

    @Serializable
    private data class RawCm(
        val id: Int, val name: String, val type: String, val power: Double = 0.0,
        val duration: Int = 3000,
        @kotlinx.serialization.SerialName("energy_delta") val energyDelta: Int = -33,
        @kotlinx.serialization.SerialName("damage_window_start") val damageWindowStart: Int = 0,
    )
}
