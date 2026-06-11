package com.pokechain.data.pvpoke

import com.pokechain.data.models.*

class PvPDataProcessor(
    private val gameMaster: GameMasterResponse
) {
    private val pokemonMap = gameMaster.pokemon.associateBy { it.speciesId }
    private val shadowPokemon = gameMaster.shadowPokemon?.toSet() ?: emptySet()

    fun processRankings(raw: List<PvPRawEntry>, filters: PvPFilterParams): List<PvPResult> {
        val results = raw.map { entry ->
            val poke = pokemonMap[entry.speciesId] ?: pokemonMap[entry.speciesId.removeSuffix("_shadow")]

            PvPResult(
                speciesId = entry.speciesId,
                speciesName = cleanName(entry.speciesName),
                score = entry.score,
                moveset = entry.moveset,
                isShadow = entry.speciesId.endsWith("_shadow"),
                needsXL = poke?.let { needsXLCandy(it) } ?: false,
                hasEliteMove = hasEliteMove(poke, entry.moveset),
                dex = poke?.dex ?: 0,
                family = poke?.family,
            )
        }

        return results
            .filter { matchesFilter(it, filters) }
            .distinctBy { it.speciesId.removeSuffix("_shadow") }
            .take(filters.count)
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
        if (!filters.xlCandy && result.needsXL) return false
        if (!filters.includeShadow && result.isShadow) return false
        if (filters.includeElite && !result.hasEliteMove) return false
        return true
    }

    private fun needsXLCandy(poke: Pokemon): Boolean {
        val maxCp = poke.defaultIVs?.get("cp2500")?.getOrNull(0) ?: 50.0
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
    val hasEliteMove: Boolean,
    val dex: Int,
    val family: Family?,
)
