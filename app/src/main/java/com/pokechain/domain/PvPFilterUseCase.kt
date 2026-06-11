package com.pokechain.domain

import com.pokechain.data.pvpoke.PvPDataProcessor
import com.pokechain.data.pvpoke.PvPResult
import com.pokechain.data.models.*

class PvPFilterUseCase(
    private val processor: PvPDataProcessor
) {
    fun execute(rankings: List<PvPRawEntry>, filters: PvPFilterParams): List<PvPResult> {
        return processor.processRankings(rankings, filters)
    }

    fun resolveBaseForm(result: PvPResult): String {
        return processor.traceBaseForm(result) ?: result.speciesId.removeSuffix("_shadow")
    }
}

class PvEFilterUseCase(
    private val baseFormResolver: (Int, String) -> String?
) {
    fun dedupAndResolve(entries: List<PvERankingEntry>): List<String> {
        val seen = mutableSetOf<String>()
        return entries.mapNotNull { entry ->
            val baseId = baseFormResolver(entry.id, entry.form)
            if (baseId != null && seen.add(baseId)) baseId else null
        }
    }
}

class SearchStringUseCase {
    fun generate(baseForms: List<String>): SearchStringResult {
        val unique = baseForms.distinct()
        return SearchStringResult(
            baseForms = unique,
            raw = unique.joinToString(",")
        )
    }
}
