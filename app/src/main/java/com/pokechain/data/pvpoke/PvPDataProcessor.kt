package com.pokechain.data.pvpoke

import com.pokechain.data.models.*

class PvPDataProcessor(
    private val gameMaster: GameMasterResponse
) {
    private val pokemonMap = gameMaster.pokemon.associateBy { it.speciesId }
    private val shadowPokemon = gameMaster.shadowPokemon?.toSet() ?: emptySet()

    fun processRankings(raw: List<PvPRawEntry>, filters: PvPFilterParams): List<PvPResult> {
        return raw
            .take(filters.count)
            .mapIndexed { index, entry ->
                val poke = pokemonMap[entry.speciesId] ?: pokemonMap[entry.speciesId.removeSuffix("_shadow")]

                PvPResult(
                    speciesId = entry.speciesId,
                    speciesName = cleanName(entry.speciesName),
                    score = entry.score,
                    moveset = entry.moveset,
                    isShadow = entry.speciesId.endsWith("_shadow"),
                    needsXL = poke?.let { needsXLCandy(it, filters.league) } ?: false,
                    eliteMoves = poke?.eliteMoves?.filter { it in entry.moveset }?.toSet() ?: emptySet(),
                    dex = poke?.dex ?: 0,
                    family = poke?.family,
                    originalRank = index + 1,
                )
            }
            .filter { matchesFilter(it, filters) }
            .distinctBy { it.speciesId.removeSuffix("_shadow") }
    }

    fun traceBaseForm(entry: PvPResult): String? {
        var speciesId = entry.speciesId.removeSuffix("_shadow").removeSuffix("_xl")
        while (true) {
            val poke = pokemonMap[speciesId] ?: break
            val parent = poke.family?.parent ?: break
            speciesId = parent
        }
        return speciesId
    }

    fun traceBaseFormForDex(dex: Int, form: String = "Normal"): String? {
        val poke = gameMaster.pokemon
            .filter { it.dex == dex }
            .minByOrNull { it.speciesId.length }
            ?: return null

        var speciesId = poke.speciesId
        val seen = mutableSetOf<String>()
        while (true) {
            if (!seen.add(speciesId)) break
            val current = pokemonMap[speciesId] ?: break
            val parent = current.family?.parent ?: break
            speciesId = parent
        }
        return speciesId
    }

    fun traceBaseDex(entry: PvPResult): Int? {
        val baseSpeciesId = traceBaseForm(entry)
        return pokemonMap[baseSpeciesId]?.dex
    }

    fun traceBaseDexForDex(dex: Int): Int? {
        val baseSpeciesId = traceBaseFormForDex(dex)
        return pokemonMap[baseSpeciesId]?.dex
    }

    fun traceBaseFormById(speciesId: String): String? {
        var current = speciesId.removeSuffix("_shadow")
        val seen = mutableSetOf<String>()
        while (true) {
            if (!seen.add(current)) break
            val poke = pokemonMap[current] ?: break
            val parent = poke.family?.parent ?: break
            current = parent
        }
        return current
    }

    private fun cleanName(name: String): String {
        return name.replace(Regex("\\s*\\(.*?\\)\\s*"), "").trim()
    }

    private fun matchesFilter(result: PvPResult, filters: PvPFilterParams): Boolean {
        val isXL = filters.xlCandy && result.needsXL
        val isShadow = filters.includeShadow && result.isShadow
        val isElite = filters.includeElite && result.eliteMoves.isNotEmpty()

        val anyActive = filters.xlCandy || filters.includeShadow || filters.includeElite
        if (!anyActive) return true

        return isXL || isShadow || isElite
    }

    private fun needsXLCandy(poke: Pokemon, league: PvPLeague): Boolean {
        val cpKey = when (league) {
            PvPLeague.GREAT -> "cp1500"
            PvPLeague.ULTRA -> "cp2500"
            PvPLeague.MASTER -> "cp10000"
        }
        val maxCp = poke.defaultIVs?.get(cpKey)?.getOrNull(0) ?: return true
        return maxCp > 40.0
    }

    private fun hasEliteMove(poke: Pokemon?, moveset: List<String>): Boolean {
        val elite = poke?.eliteMoves ?: return false
        return moveset.any { it in elite }
    }
}

data class PvPResult(
    val speciesId: String,
    val speciesName: String,
    val score: Double,
    val moveset: List<String>,
    val isShadow: Boolean,
    val needsXL: Boolean,
    val eliteMoves: Set<String>,
    val dex: Int,
    val family: Family?,
    val originalRank: Int = 0,
)
