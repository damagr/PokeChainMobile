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
 * El constructor secundario con texto JSON permite tests de JVM sin Android.
 */
class ShowcaseDataProvider private constructor(
    private val data: Map<String, SpeciesFormData>
) {

    constructor(context: Context) : this(loadFromAssets(context))
    constructor(jsonText: String) : this(parse(jsonText))

    data class SpeciesFormData(
        val name: String,
        val forms: Map<String, SpeciesSize>,
        val isUniform: Boolean
    )

    /** Get size data for a species by dex number and optional form suffix. */
    fun getSize(dex: Int, formSuffix: String? = null): SpeciesFormEntry? {
        val entry = data[dex.toString()] ?: return null
        val formKey = formSuffix ?: ""
        val size = entry.forms[formKey]
            ?: entry.forms.values.firstOrNull().takeIf { entry.isUniform }
            ?: return null
        return SpeciesFormEntry(dex, entry.name, size, formKey)
    }

    /** Get size data for a species by dex and measured height/weight.
     *
     * Selección de forma cuando hay varias (el lector no puede distinguirlas):
     *  - Especie uniforme o de una sola forma: esa forma.
     *  - Con forma base (""): se usa la base siempre que las medidas quepan en
     *    su rango (height ≤ maxHeight y weight ≤ maxWeight). Un individuo
     *    XL/XXL de la forma base desvía mucho de su base y NO implica otra
     *    forma: elegir la base "más cercana" asignaba variantes por error
     *    (p. ej. Sandslash Alolan para un Sandslash normal XXL → 710 vs 1056).
     *  - Si la medida excede el rango de la base: entre el resto de formas se
     *    elige la que contenga la medida (o la más cercana si ninguna la hace).
     *  - Sin forma base (clases de tamaño, p. ej. Pumpkaboo/Gourgeist): la
     *    forma con la base más cercana a lo medido. */
    fun getSizeByMeasurements(dex: Int, heightM: Double, weightKg: Double): SpeciesFormEntry? {
        val entry = data[dex.toString()] ?: return null
        val forms = entry.forms
        if (forms.isEmpty()) return null
        if (entry.isUniform || forms.size == 1) {
            val key = forms.keys.first()
            return SpeciesFormEntry(dex, entry.name, forms.values.first(), key)
        }
        val base = forms[DEFAULT_FORM_KEY]
        if (base != null) {
            if (heightM <= base.maxHeight && weightKg <= base.maxWeight) {
                return SpeciesFormEntry(dex, entry.name, base, DEFAULT_FORM_KEY)
            }
            val others = forms.filterKeys { it != DEFAULT_FORM_KEY }
            val other = nearestForm(others.values, heightM, weightKg)
                ?: return SpeciesFormEntry(dex, entry.name, base, DEFAULT_FORM_KEY)
            val key = others.entries.first { it.value == other }.key
            return SpeciesFormEntry(dex, entry.name, other, key)
        }
        val best = nearestForm(forms.values, heightM, weightKg) ?: return null
        val key = forms.entries.first { it.value == best }.key
        return SpeciesFormEntry(dex, entry.name, best, key)
    }

    /** Claves de forma disponibles para una dex ("" = base). Vacío si no hay datos. */
    fun getForms(dex: Int): List<String> =
        data[dex.toString()]?.forms?.keys?.toList() ?: emptyList()

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

    /** La forma cuya base está más cerca de lo medido; prefiere las que contienen la medida. */
    private fun nearestForm(
        forms: Collection<SpeciesSize>,
        heightM: Double,
        weightKg: Double
    ): SpeciesSize? {
        if (forms.isEmpty()) return null
        val fitting = forms.filter { heightM <= it.maxHeight && weightKg <= it.maxWeight }
        val pool = fitting.ifEmpty { forms }
        return pool.minByOrNull {
            abs(it.baseHeight - heightM) + abs(it.baseWeight - weightKg)
        }
    }

    companion object {
        private const val DEFAULT_FORM_KEY = ""
        private val json = Json { ignoreUnknownKeys = true }

        private fun loadFromAssets(context: Context): Map<String, SpeciesFormData> =
            try {
                parse(context.assets.open("showcase_data.json")
                    .bufferedReader().use { it.readText() })
            } catch (_: Exception) {
                emptyMap()
            }

        private fun parse(text: String): Map<String, SpeciesFormData> =
            try {
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
}

data class SpeciesFormEntry(
    val dex: Int,
    val name: String,
    val size: SpeciesSize,
    /** Clave de la forma elegida ("" = base). Útil para depurar y para el toggle del chip. */
    val formKey: String = ""
)
