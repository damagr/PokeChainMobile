package com.pokechain.data.showcase

import android.content.Context
import kotlin.math.abs
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Per-form size data: [pokedexHeight(m), pokedexWeight(kg), maxHeight(m)].
 */
data class SpeciesSize(
    val baseHeight: Double,
    val baseWeight: Double,
    val maxHeight: Double
) {
    /** Derived maximum weight for this species. */
    val maxWeight: Double get() = baseWeight * (maxHeight / baseHeight + 0.5)
}

/**
 * Full result of a showcase score calculation.
 */
data class ShowcaseResult(
    val total: Int,
    val heightPts: Double,
    val weightPts: Double,
    val ivPts: Double,
    val haveIvs: Boolean,
    val xxlPts: Int,
    val isXXL: Boolean,
    val isXXS: Boolean,
    val ratio: Double,
    val capped: Boolean,
    val maxH: Double,
    val maxW: Double
)

@Serializable
private data class ShowcaseFileEntry(
    @SerialName("n") val name: String,
    @SerialName("f") val forms: Map<String, List<Double>>
)

/**
 * Loads per-species size data from the bundled [showcase_data.json] asset.
 */
class ShowcaseDataProvider(context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val data: Map<String, SpeciesFormData> = load(context)

    data class SpeciesFormData(
        val name: String,
        val forms: Map<String, SpeciesSize>,
        val isUniform: Boolean
    )

    private fun load(context: Context): Map<String, SpeciesFormData> {
        return try {
            val text = context.assets.open("showcase_data.json")
                .bufferedReader().use { it.readText() }
            json.decodeFromString<Map<String, ShowcaseFileEntry>>(text)
                .mapValues { (_, e) ->
                    val forms = e.forms.mapValues { (_, v) ->
                        SpeciesSize(v[0], v[1], v[2])
                    }
                    val sizes = forms.values.toList()
                    val isUniform = sizes.isEmpty() || sizes.all {
                        it.baseHeight == sizes[0].baseHeight &&
                        it.baseWeight == sizes[0].baseWeight &&
                        it.maxHeight == sizes[0].maxHeight
                    }
                    SpeciesFormData(e.name, forms, isUniform)
                }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    /** Get size data for a species by dex number and optional form suffix. */
    fun getSize(dex: Int, formSuffix: String? = null): SpeciesFormEntry? {
        val entry = data[dex.toString()] ?: return null
        val formKey = formSuffix ?: ""
        val size = entry.forms[formKey]
            ?: entry.forms.values.firstOrNull().takeIf { entry.isUniform }
            ?: return null
        return SpeciesFormEntry(dex, entry.name, size)
    }

    /** Get size data for a species by dex and measured height/weight.
     *  Para especies no uniformes (ej. Pumpkaboo/Gourgeist con 4 tamaños),
     *  elige la forma cuya baseHeight/baseWeight se acerque más a lo medido
     *  por pantalla, en lugar de requerir un sufijo de forma. */
    fun getSizeByMeasurements(dex: Int, heightM: Double, weightKg: Double): SpeciesFormEntry? {
        val entry = data[dex.toString()] ?: return null
        val forms = entry.forms.values.toList()
        if (forms.isEmpty()) return null
        if (entry.isUniform || forms.size == 1) {
            return SpeciesFormEntry(dex, entry.name, forms.first())
        }
        var best: SpeciesSize? = null
        var bestDiff = Double.MAX_VALUE
        for (size in forms) {
            val diff = abs(size.baseHeight - heightM) + abs(size.baseWeight - weightKg)
            if (diff < bestDiff) { bestDiff = diff; best = size }
        }
        return best?.let { SpeciesFormEntry(dex, entry.name, it) }
    }

    /** Search by name or dex number. Returns dex numbers of matches. */
    fun search(query: String): List<Int> {
        if (query.isBlank()) return data.keys.mapNotNull { it.toIntOrNull() }.sorted()
        val q = query.trim().lowercase()
        return data.entries
            .filter { (dex, entry) ->
                dex == q || entry.name.lowercase().contains(q) || dex.startsWith(q)
            }
            .mapNotNull { it.key.toIntOrNull() }
            .sorted()
    }
}

data class SpeciesFormEntry(
    val dex: Int,
    val name: String,
    val size: SpeciesSize
)
